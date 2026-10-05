package namidevelopment.kiriyaga.nami.impl.feature.visuals.blocksearch;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.ChunkDataEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.api.util.BlockUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class BlockSearchFeature extends Feature {

    private final WhitelistSetting blockList = addSetting(new WhitelistSetting("Whitelist", true, WhitelistSetting.Type.BLOCK));
    public final BoolSetting fill = addSetting(new BoolSetting("Fill", true));
    public final BoolSetting notAtSpawn = addSetting(new BoolSetting("NotAtSpawn", false));

    private Set<Identifier> candidateIds = new HashSet<>();
    private final BlockingQueue<ChunkSnapshot> snapshotQueue = new LinkedBlockingQueue<>();
    public static final ConcurrentMap<Long, Set<BlockPos>> chunkBlocks = new ConcurrentHashMap<>();

    private BlockSearch worker;

    public BlockSearchFeature() {
        super("BlockSearch", "Search for specified blocks.", FeatureCategory.of("Render"));

        blockList.setOnChanged(this::updateCandidateBlocks);
        notAtSpawn.setOnChanged(this::reloadChunks);
    }

    @Override
    public void onEnable() {
        updateCandidateBlocks();

        worker = new BlockSearch(snapshotQueue, chunkBlocks);
        worker.start();
        reloadChunks();
    }

    @Override
    public void onDisable() {
        if (worker != null) worker.stopWorker();
        snapshotQueue.clear();
        chunkBlocks.clear();
    }

    private void updateCandidateBlocks() {
        candidateIds = blockList.getWhitelist();
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onChunkLoad(ChunkDataEvent event) {
        if (MC.level == null || MC.player == null) return;

        if (notAtSpawn.get()) {
            BlockPos p = MC.player.blockPosition();
            if (Math.abs(p.getX()) + Math.abs(p.getZ()) < 12000) return;
        }

        snapshotQueue.offer(makeSnapshot(event.getChunk()));

        chunkBlocks.keySet().removeIf(chunkKey -> {
            int cx = ChunkPos.getX(chunkKey);
            int cz = ChunkPos.getZ(chunkKey);
            return !isLoaded(cx, cz);
        });
    }

    private ChunkSnapshot makeSnapshot(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        Set<BlockPos> found = new HashSet<>();
        LevelChunkSection[] sections = chunk.getSections();
        for (int secIndex = 0; secIndex < sections.length; secIndex++) {
            LevelChunkSection section = sections[secIndex];
            if (section == null || section.hasOnlyAir()) continue;

            int baseY = chunk.getSectionYFromSectionIndex(secIndex) << 4;

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {

                        var state = section.getBlockState(x, y, z);
                        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());

                        if (!candidateIds.contains(id)) continue;

                        found.add(new BlockPos(
                                pos.getMinBlockX() + x,
                                baseY + y,
                                pos.getMinBlockZ() + z
                        ));
                    }
                }
            }
        }
        return new ChunkSnapshot(pos, found);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRender(Render3DEvent event) {
        if (MC.level == null || MC.player == null) return;
        for (Set<BlockPos> set : chunkBlocks.values()) {
            for (BlockPos pos : set) {
                var state = MC.level.getBlockState(pos);
                Color color = BlockUtils.getColorByBlockId(state);

                RenderUtil.drawBlockPosLines(MC.level, pos, state, color, fill.get(), true, 1.5f);

            }
        }
    }

    public static Collection<Set<BlockPos>> getFoundChunks() {
        return chunkBlocks.values();
    }

    private void reloadChunks() {
        if (MC.level == null || MC.player == null) return;

        snapshotQueue.clear();
        chunkBlocks.clear();
        int radius = MC.options.renderDistance().get();
        ChunkPos pc = new ChunkPos(MC.player.blockPosition());

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                snapshotQueue.offer(makeSnapshot(MC.level.getChunk(pc.x + dx, pc.z + dz)));
            }
        }
    }

    private boolean isLoaded(int x, int z) {
        if (MC.level == null) return false;
        return MC.level.getChunkSource().getChunk(x, z, false) != null;
    }
}