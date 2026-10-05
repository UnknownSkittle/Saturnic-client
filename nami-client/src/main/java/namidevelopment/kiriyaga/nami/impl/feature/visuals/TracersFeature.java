package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.util.BlockUtils;
import namidevelopment.kiriyaga.api.util.ColorUtils;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.blocksearch.BlockSearchFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class TracersFeature extends Feature {
    public final BoolSetting espTargets = addSetting(new BoolSetting("ESP", true));
    public final BoolSetting entitySearchTargets = addSetting(new BoolSetting("EntitySearch", false));
    public final BoolSetting itemSearchTargets = addSetting(new BoolSetting("ItemSearch", false));
    public final BoolSetting blockSearchTargets = addSetting(new BoolSetting("BlockSearch", false));
    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 128, 16, 512));

    public TracersFeature() {
        super("Tracers", "Draws view-aligned lines to selected ESP and search targets.", FeatureCategory.of("Render"), "tracer");
    }

    @SubscribeEvent
    public void onRender(Render3DEvent event) {
        if (MC.level == null || MC.player == null)
            return;

        Map<Entity, Color> targets = new LinkedHashMap<>();
        if (espTargets.get()) {
            ESPFeature esp = FEATURE_SERVICE.getStorage().getByClass(ESPFeature.class);
            if (esp != null && esp.isEnabled()) {
                for (Entity entity : esp.getEntitiesToRender())
                    targets.putIfAbsent(entity, esp.getColorForEntity(entity,
                            FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class)));
            }
        }

        if (entitySearchTargets.get()) {
            EntitySearchFeature search = FEATURE_SERVICE.getStorage().getByClass(EntitySearchFeature.class);
            if (search != null && search.isEnabled()) {
                for (Entity entity : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.ALL)) {
                    if (search.matchesEntity(entity))
                        targets.putIfAbsent(entity, search.getColorForEntity(entity));
                }
            }
        }

        if (itemSearchTargets.get()) {
            ItemSearchFeature search = FEATURE_SERVICE.getStorage().getByClass(ItemSearchFeature.class);
            if (search != null && search.isEnabled()) {
                for (Entity entity : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.ALL)) {
                    if (search.matchesItem(entity))
                        targets.putIfAbsent(entity, ColorUtils.COLOR_ITEM);
                }
            }
        }

        Vec3 origin = event.getCamera().position().add(event.getCamera().getNearPlane().getPointOnPlane(0, 0));
        double maxDistanceSqr = range.get() * range.get();
        for (Map.Entry<Entity, Color> target : targets.entrySet()) {
            Entity entity = target.getKey();
            if (entity.isRemoved() || !entity.isAlive())
                continue;

            Vec3 interpolatedPosition = EntityUtils.getRenderPos(entity, event.getTickDelta());
            Vec3 position = entity.getBoundingBox()
                    .move(interpolatedPosition.subtract(entity.position()))
                    .getCenter();
            if (MC.player.distanceToSqr(position) > maxDistanceSqr)
                continue;
            RenderUtil.drawLine(origin, position, target.getValue(), 1.5f);
        }

        if (blockSearchTargets.get()) {
            BlockSearchFeature search = FEATURE_SERVICE.getStorage().getByClass(BlockSearchFeature.class);
            if (search == null || !search.isEnabled())
                return;

            for (Set<BlockPos> chunkBlocks : BlockSearchFeature.getFoundChunks()) {
                for (BlockPos pos : chunkBlocks) {
                    Vec3 center = Vec3.atCenterOf(pos);
                    if (MC.player.distanceToSqr(center) > maxDistanceSqr)
                        continue;
                    Color color = BlockUtils.getColorByBlockId(MC.level.getBlockState(pos));
                    RenderUtil.drawLine(origin, center, color, 1.5f);
                }
            }
        }
    }
}
