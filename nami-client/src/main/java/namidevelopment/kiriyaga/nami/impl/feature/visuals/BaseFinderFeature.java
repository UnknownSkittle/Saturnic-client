package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import net.minecraft.core.BlockPos;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class BaseFinderFeature extends Feature {
    public final IntSetting chunkRange = addSetting(new IntSetting("ChunkRange", 8, 2, 16));
    public final IntSetting minContainers = addSetting(new IntSetting("MinContainers", 4, 2, 12));
    private Set<BlockPos> possibleBases = Set.of();
    private int ticks;

    public BaseFinderFeature() {
        super("BaseFinder", "Marks loaded areas with clusters of storage blocks as possible bases.", FeatureCategory.of("Render"), "bases");
    }

    @Override
    public void onDisable() {
        possibleBases = Set.of();
        clearDisplayInfo();
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.level == null || MC.player == null || ++ticks % 40 != 0)
            return;

        Map<Long, Set<BlockPos>> cells = new HashMap<>();
        for (BlockPos pos : StorageBlockUtils.findLoadedStorage(chunkRange.get())) {
            int cellX = Math.floorDiv(pos.getX(), 32);
            int cellZ = Math.floorDiv(pos.getZ(), 32);
            long key = (long) cellX << 32 | (cellZ & 0xffffffffL);
            cells.computeIfAbsent(key, ignored -> new HashSet<>()).add(pos);
        }

        Set<BlockPos> found = new HashSet<>();
        cells.values().stream()
                .filter(cell -> cell.size() >= minContainers.get())
                .forEach(found::addAll);
        possibleBases = found;

        clearDisplayInfo();
        possibleBases.stream()
                .min((a, b) -> Double.compare(MC.player.distanceToSqr(a.getX(), a.getY(), a.getZ()),
                        MC.player.distanceToSqr(b.getX(), b.getY(), b.getZ())))
                .ifPresent(pos -> addDisplayInfo("Possible base near " + pos.getX() + ", " + pos.getZ()));
        if (!possibleBases.isEmpty())
            addDisplayInfo("Storage blocks: " + possibleBases.size());
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        if (MC.level == null || MC.player == null)
            return;

        for (BlockPos pos : possibleBases) {
            if (MC.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 192 * 192)
                continue;
            RenderUtil.drawBlockPosLines(MC.level, pos, MC.level.getBlockState(pos),
                    new Color(255, 90, 90), false, true, 2.0f);
        }
    }
}
