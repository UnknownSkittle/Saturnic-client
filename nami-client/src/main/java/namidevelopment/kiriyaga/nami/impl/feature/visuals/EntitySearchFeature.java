package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.api.util.ColorUtils;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.awt.*;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class EntitySearchFeature extends Feature {

    private final WhitelistSetting entityWhitelist = addSetting(new WhitelistSetting("Whitelist", true, WhitelistSetting.Type.ENTITY));
    public final BoolSetting renderBoxes = addSetting(new BoolSetting("Render", true));
    public EntitySearchFeature() {
        super("EntitySearch", "Searchs for specified entities.", FeatureCategory.of("Render"));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRender(Render3DEvent event) {
        if (MC.level == null || MC.player == null) return;

        for (Entity entity : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.ALL)) {
            if (entity.isRemoved() || !entity.isAlive()) continue;

            if (!matchesEntity(entity)) continue;

            Color color = getColorForEntity(entity);

            double interpX = entity.xOld + (entity.getX() - entity.xOld) * event.getTickDelta();
            double interpY = entity.yOld + (entity.getY() - entity.yOld) * event.getTickDelta();
            double interpZ = entity.zOld + (entity.getZ() - entity.zOld) * event.getTickDelta();

            AABB box = entity.getBoundingBox().move(
                    interpX - entity.getX(),
                    interpY - entity.getY(),
                    interpZ - entity.getZ()
            );

            if (renderBoxes.get()) {
                RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
            }

        }
    }

    boolean matchesEntity(Entity entity) {
        return entityWhitelist.getWhitelist().contains(EntityType.getKey(entity.getType()));
    }

    Color getColorForEntity(Entity entity) {
        if (entity instanceof Player) {
            return FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledGlobalColor();
        }
        if (EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PASSIVE).contains(entity)) return ColorUtils.COLOR_PASSIVE;
        if (EntityUtils.getEntities(EntityUtils.EntityTypeCategory.NEUTRAL).contains(entity)) return ColorUtils.COLOR_NEUTRAL;
        if (EntityUtils.getEntities(EntityUtils.EntityTypeCategory.HOSTILE).contains(entity)) return ColorUtils.COLOR_HOSTILE;
        if (entity instanceof ItemEntity) return ColorUtils.COLOR_ITEM;
        return Color.WHITE;
    }
}
