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
import java.util.Set;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class StorageESPFeature extends Feature {
    public final IntSetting range = addSetting(new IntSetting("ChunkRange", 8, 2, 16));
    private Set<BlockPos> storagePositions = Set.of();
    private int ticks;

    public StorageESPFeature() {
        super("StorageESP", "Highlights loaded chests, barrels, shulkers, and utility storage blocks.", FeatureCategory.of("Render"), "storage");
    }

    @Override
    public void onDisable() {
        storagePositions = Set.of();
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.level == null || MC.player == null || ++ticks % 20 != 0)
            return;
        storagePositions = StorageBlockUtils.findLoadedStorage(range.get());
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        if (MC.level == null || MC.player == null)
            return;

        for (BlockPos pos : storagePositions) {
            if (MC.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 192 * 192)
                continue;
            RenderUtil.drawBlockPosLines(MC.level, pos, MC.level.getBlockState(pos),
                    new Color(255, 177, 66), false, true, 1.5f);
        }
    }
}
