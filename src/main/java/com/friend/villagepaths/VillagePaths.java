package com.friend.villagepaths;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class VillagePaths implements ModInitializer {

    // 仅用于记录已经规划过的路径（防止重复计算）
    private static final Set<String> DONE = ConcurrentHashMap.newKeySet();
    private static final Set<ServerPlayer> PENDING_PLAYERS = ConcurrentHashMap.newKeySet();

    // 核心队列：存放所有待修改的方块坐标 (采用无锁并发队列)
    private static final Queue<RoadNode> ROAD_QUEUE = new ConcurrentLinkedQueue<>();

    // 封装方块信息，记录需要怎么修改
    private record RoadNode(ServerLevel level, BlockPos pos) {}

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((h, _, _) -> PENDING_PLAYERS.add(h.player));

        // 1. 进服 5 秒后计算路线
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 != 0 || PENDING_PLAYERS.isEmpty()) return;
            for (var p : new HashSet<>(PENDING_PLAYERS)) {
                PENDING_PLAYERS.remove(p);
                connect(p.level(), p.blockPosition(), p);
            }
        });

        // 2. 核心铺路线程：每 tick 从队列里取 2000 个方块进行处理，不卡顿且极快
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int count = 0;
            while (!ROAD_QUEUE.isEmpty() && count < 2000) {
                RoadNode node = ROAD_QUEUE.poll();
                if (node != null) {
                    placeRoadBlock(node.level, node.pos);
                    count++;
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((d, _, _) ->
                d.register(Commands.literal("connectvillage").executes(c -> {
                    if (c.getSource().getEntity() instanceof ServerPlayer p) {
                        connect(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), p);
                    }
                    return 1;
                }))
        );
    }

    private void connect(ServerLevel lvl, BlockPos org, ServerPlayer player) {
        var v1 = lvl.findNearestMapStructure(StructureTags.VILLAGE, org, 5000, false);
        if (v1 == null) {
            player.sendSystemMessage(Component.literal("§c[VillagePaths] No village found within 5000 blocks."));
            return;
        }

        var v2 = lvl.findNearestMapStructure(StructureTags.VILLAGE, v1, 20000, false);
        if (v2 == null || v2.equals(v1)) v2 = v1.offset(500, 0, 500);

        String k1 = v1.toShortString(), k2 = v2.toShortString();
        if (!DONE.add(k1.compareTo(k2) < 0 ? k1 + ":" + k2 : k2 + ":" + k1)) return;

        player.sendSystemMessage(Component.literal("§a[VillagePaths] Path planned! Generating in background."));

        // 计算路径点，全部塞进队列
        List<BlockPos> pts = new ArrayList<>(calcLine(org, v1));
        pts.addAll(calcLine(v1, v2));

        for (var p : pts) {
            // 3x3 的范围，全部加入队列
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    ROAD_QUEUE.add(new RoadNode(lvl, p.offset(ox, 0, oz)));
                }
            }
        }
    }

    // 安全的方块修改逻辑：只在区块已加载时执行
    private void placeRoadBlock(ServerLevel lvl, BlockPos pos) {
        // 如果区块没加载，直接跳过（丢到未加载区块里也没用）
        if (!lvl.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return;

        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - 1;
        BlockPos groundPos = new BlockPos(pos.getX(), y, pos.getZ());
        BlockState gs = lvl.getBlockState(groundPos);

        // 清理上方空间
        for (int j = 1; j <= 3; j++) {
            lvl.setBlock(groundPos.above(j), Blocks.AIR.defaultBlockState(), 2);
        }

        // 遇水搭桥
        if (gs.is(Blocks.WATER)) {
            lvl.setBlock(groundPos, Blocks.OAK_PLANKS.defaultBlockState(), 2);
        } else {
            lvl.setBlock(groundPos, surf(gs), 2);
        }
    }

    private List<BlockPos> calcLine(BlockPos v1, BlockPos v2) {
        List<BlockPos> pts = new ArrayList<>();
        int x = v1.getX(), z = v1.getZ(), dx = Math.abs(v2.getX() - x), dz = Math.abs(v2.getZ() - z);
        int sx = x < v2.getX() ? 1 : -1, sz = z < v2.getZ() ? 1 : -1, err = dx - dz;
        while (true) {
            pts.add(new BlockPos(x, 0, z));
            if (x == v2.getX() && z == v2.getZ()) break;
            int e2 = 2 * err;
            if (e2 > -dz) { err -= dz; x += sx; }
            if (e2 < dx) { err += dx; z += sz; }
        }
        return pts;
    }

    private BlockState surf(BlockState gs) {
        if (gs.is(Blocks.SAND) || gs.is(Blocks.RED_SAND)) return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        if (gs.is(Blocks.SNOW) || gs.is(Blocks.SNOW_BLOCK)) return Blocks.PACKED_ICE.defaultBlockState();
        if (gs.is(Blocks.STONE) || gs.is(Blocks.ANDESITE) || gs.is(Blocks.DIORITE) || gs.is(Blocks.GRANITE)) return Blocks.GRAVEL.defaultBlockState();
        return Blocks.DIRT_PATH.defaultBlockState();
    }
}