package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.ColorUtils.COLOR_ITEM;

@RegisterFeature
public class ItemSearchFeature extends Feature {

    private final WhitelistSetting itemWhitelist = addSetting(new WhitelistSetting("Whitelist", true, WhitelistSetting.Type.ENTITY));
    public final BoolSetting renderBoxes = addSetting(new BoolSetting("Render", true));
    public final BoolSetting itemFrames = addSetting(new BoolSetting("ItemFrames", false));
    public final BoolSetting chatFeedback = addSetting(new BoolSetting("ChatFeedback", false));

    private final Set<Integer> sent = new HashSet<>();
    public ItemSearchFeature() {
        super("ItemSearch", "Searches for specified item entities.", FeatureCategory.of("Render"));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRender(Render3DEvent event) {
        if (MC.level == null || MC.player == null) return;

        for (Entity entity : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.DROPPED_ITEMS)) {
            if (entity instanceof ItemEntity item) {
                if (item.isRemoved() || !item.isAlive()) continue;

                if (!matchesItem(item)) continue;

                double interpX = item.xOld + (item.getX() - item.xOld) * event.getTickDelta();
                double interpY = item.yOld + (item.getY() - item.yOld) * event.getTickDelta();
                double interpZ = item.zOld + (item.getZ() - item.zOld) * event.getTickDelta();

                AABB box = item.getBoundingBox().move(interpX - item.getX(), interpY - item.getY(), interpZ - item.getZ());

                if (renderBoxes.get()) {
                    RenderUtil.drawBoxLines(box, COLOR_ITEM, true, true, 1.5f);
                }

                Integer entId = entity.getId();

                if (!sent.contains(entId) && chatFeedback.get()) {
                    Component message = CAT_FORMAT.format("Item: {global}" + item.getItem().getHoverName().getString() + " {gray} found.");
                    CHAT_SERVICE.sendPersistent(entId.toString(), message);
                    sent.add(entId);
                }
            }
        }
        if (itemFrames.get()) {
            for (Entity entity : MC.level.entitiesForRendering()) {
                if (!(entity instanceof ItemFrame frame)) continue;

                if (frame.getItem().isEmpty()) continue;

                if (!matchesItem(frame)) continue;
                double interpX = frame.xOld + (frame.getX() - frame.xOld) * event.getTickDelta();
                double interpY = frame.yOld + (frame.getY() - frame.yOld) * event.getTickDelta();
                double interpZ = frame.zOld + (frame.getZ() - frame.zOld) * event.getTickDelta();

                AABB box = frame.getBoundingBox().move(interpX - frame.getX(), interpY - frame.getY(), interpZ - frame.getZ());

                if (renderBoxes.get()) {
                    RenderUtil.drawBoxLines(box, COLOR_ITEM, true, true, 1.5f);
                }

                Integer entId = entity.getId();

                if (!sent.contains(entId) && chatFeedback.get()) {
                    Component message = CAT_FORMAT.format("Item: {global}" + frame.getItem().getItemName() + " {gray} found.");
                    CHAT_SERVICE.sendPersistent(entId.toString(), message);
                    sent.add(entId);
                }
            }

        }
    }

    boolean matchesItem(Entity entity) {
        if (entity instanceof ItemEntity item)
            return itemWhitelist.getWhitelist().contains(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()));
        if (entity instanceof ItemFrame frame)
            return itemFrames.get() && !frame.getItem().isEmpty()
                    && itemWhitelist.getWhitelist().contains(BuiltInRegistries.ITEM.getKey(frame.getItem().getItem()));
        return false;
    }
}
