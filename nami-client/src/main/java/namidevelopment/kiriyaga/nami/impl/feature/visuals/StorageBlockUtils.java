package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Set;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

final class StorageBlockUtils {
    private StorageBlockUtils() {}

    static Set<BlockPos> findLoadedStorage(int chunkRadius) {
        if (MC.level == null || MC.player == null)
            return Set.of();

        Set<BlockPos> positions = new HashSet<>();
        ChunkPos playerChunk = new ChunkPos(MC.player.blockPosition());
        for (int chunkX = playerChunk.x - chunkRadius; chunkX <= playerChunk.x + chunkRadius; chunkX++) {
            for (int chunkZ = playerChunk.z - chunkRadius; chunkZ <= playerChunk.z + chunkRadius; chunkZ++) {
                var chunk = MC.level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null)
                    continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    var block = blockEntity.getBlockState().getBlock();
                    if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST
                            || block == Blocks.ENDER_CHEST || block == Blocks.BARREL
                            || block == Blocks.HOPPER || block == Blocks.DISPENSER
                            || block == Blocks.DROPPER || block == Blocks.FURNACE
                            || block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER
                            || block == Blocks.BREWING_STAND || block instanceof ShulkerBoxBlock) {
                        positions.add(blockEntity.getBlockPos().immutable());
                    }
                }
            }
        }
        return positions;
    }
}
