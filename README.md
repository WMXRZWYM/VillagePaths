# VulkBatch
<img width="256" height="256" alt="V" src="https://github.com/user-attachments/assets/9e6fd7b7-56d8-4fe2-be5d-f54a2bba2c0b" />


A lightweight client-side optimization mod for Minecraft 26.3 (Vulkan/OpenGL).

## Features

- **BufferBuilder Batching:** Intercepts `BufferBuilder` builds to reuse memory allocations, significantly reducing GC pressure during heavy entity and particle rendering.
- **Chunk Rebuild Optimization:** Introduces a 5-tick rolling window to skip redundant chunk rebuilds, saving CPU cycles when blocks are constantly updated (e.g., explosions, fast mining).
- **Vulkan & OpenGL Support:** Fully compatible with both rendering backends introduced in Minecraft 26.x.

## Installation

1. Install **Fabric Loader** for Minecraft 26.3.
2. Download and drop the `.jar` file into your `mods` folder.
3. Requires **Fabric API**.

## License

This project is licensed under the MIT License.
