package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.Render2DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.util.BlockUtils;
import namidevelopment.kiriyaga.api.util.ColorUtils;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
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
    public final DoubleSetting thickness = addSetting(new DoubleSetting("Thickness", 1.0, 1.0, 5.0));

    public TracersFeature() {
        super("Tracers", "Draws view-aligned lines to selected ESP and search targets.", FeatureCategory.of("Render"), "tracer");
    }

    @SubscribeEvent
    public void onRender(Render2DEvent event) {
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

        double maxDistanceSqr = range.get() * range.get();
        GuiGraphics graphics = event.getDrawContext();
        float partialTick = event.getRenderTickCounter().getGameTimeDeltaPartialTick(true);
        float screenCenterX = graphics.guiWidth() * 0.5f;
        float screenCenterY = graphics.guiHeight() * 0.5f;
        for (Map.Entry<Entity, Color> target : targets.entrySet()) {
            Entity entity = target.getKey();
            if (entity.isRemoved() || !entity.isAlive())
                continue;

            Vec3 interpolatedPosition = EntityUtils.getRenderPos(entity, partialTick);
            Vec3 position = entity.getBoundingBox()
                    .move(interpolatedPosition.subtract(entity.position()))
                    .getCenter();
            if (MC.player.distanceToSqr(position) > maxDistanceSqr)
                continue;
            float[] endpoint = getScreenEndpoint(position, graphics, screenCenterX, screenCenterY);
            if (endpoint == null)
                continue;
            drawScreenLine(graphics, screenCenterX, screenCenterY,
                    endpoint[0], endpoint[1], target.getValue());
        }

        FreecamFeature freecam = FEATURE_SERVICE.getStorage().getByClass(FreecamFeature.class);
        if (freecam != null && freecam.isEnabled()) {
            Vec3 playerPosition = EntityUtils.getRenderPos(MC.player, partialTick);
            Vec3 playerCenter = MC.player.getBoundingBox()
                    .move(playerPosition.subtract(MC.player.position()))
                    .getCenter();
            float[] endpoint = getScreenEndpoint(playerCenter, graphics, screenCenterX, screenCenterY);
            if (endpoint != null) {
                float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
                Color rainbow = Color.getHSBColor(hue, 1.0f, 1.0f);
                drawScreenLine(graphics, screenCenterX, screenCenterY,
                        endpoint[0], endpoint[1], rainbow);
            }
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
                    float[] endpoint = getScreenEndpoint(center, graphics, screenCenterX, screenCenterY);
                    if (endpoint == null)
                        continue;
                    drawScreenLine(graphics, screenCenterX, screenCenterY,
                            endpoint[0], endpoint[1], color);
                }
            }
        }
    }

    private float[] getScreenEndpoint(Vec3 worldPosition, GuiGraphics graphics,
                                      float centerX, float centerY) {
        Camera camera = MC.gameRenderer.getMainCamera();
        Vec3 cameraOffset = worldPosition.subtract(camera.position());
        double depth = cameraOffset.dot(new Vec3(camera.forwardVector()));
        double screenX;
        double screenY;
        boolean behindCamera = depth <= 0;

        if (behindCamera) {
            double fov = Math.toRadians(MC.options.fov().get());
            double focalLength = graphics.guiHeight() / (2.0 * Math.tan(fov * 0.5));
            double safeDepth = Math.max(0.01, Math.abs(depth));
            double horizontal = cameraOffset.dot(new Vec3(camera.leftVector()).scale(-1.0));
            double vertical = cameraOffset.dot(new Vec3(camera.upVector()));
            screenX = centerX + horizontal / safeDepth * focalLength;
            screenY = centerY - vertical / safeDepth * focalLength;
        } else {
            Vec3 projected = RenderUtil.project(worldPosition);
            if (!Double.isFinite(projected.x) || !Double.isFinite(projected.y))
                return null;
            screenX = projected.x;
            screenY = projected.y;
        }

        boolean outsideScreen = screenX < 0 || screenX > graphics.guiWidth()
                || screenY < 0 || screenY > graphics.guiHeight();
        if (!behindCamera && !outsideScreen)
            return new float[]{(float) screenX, (float) screenY};

        double dx = screenX - centerX;
        double dy = screenY - centerY;
        if (Math.hypot(dx, dy) < 0.001) {
            dx = 0;
            dy = -1;
        }

        double maxX = Math.max(1, centerX - 2);
        double maxY = Math.max(1, centerY - 2);
        double scaleX = Math.abs(dx) < 0.001 ? Double.POSITIVE_INFINITY : maxX / Math.abs(dx);
        double scaleY = Math.abs(dy) < 0.001 ? Double.POSITIVE_INFINITY : maxY / Math.abs(dy);
        double scale = Math.min(scaleX, scaleY);
        return new float[]{
                (float) (centerX + dx * scale),
                (float) (centerY + dy * scale)
        };
    }

    private void drawScreenLine(GuiGraphics graphics, float startX, float startY,
                                float endX, float endY, Color color) {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float length = (float) Math.hypot(deltaX, deltaY);
        if (length < 1)
            return;

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((startX + endX) * 0.5f, (startY + endY) * 0.5f);
        pose.rotate((float) Math.atan2(deltaY, deltaX));
        int lineThickness = Math.max(1, (int) Math.round(thickness.get()));
        int top = -(lineThickness / 2);
        graphics.fill((int) -Math.ceil(length * 0.5f), top,
                (int) Math.ceil(length * 0.5f), top + lineThickness, color.getRGB());
        pose.popMatrix();
    }
}
