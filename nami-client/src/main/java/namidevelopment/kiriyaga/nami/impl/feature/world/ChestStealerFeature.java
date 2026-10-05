package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ClickType;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class ChestStealerFeature extends Feature {
    public final IntSetting delay = addSetting(new IntSetting("Delay", 2, 0, 20));
    private int waitTicks;

    public ChestStealerFeature() {
        super("ChestStealer", "Moves container contents into your inventory.", FeatureCategory.of("World"), "stealer");
    }

    @Override
    public void onDisable() {
        waitTicks = 0;
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.gameMode == null || !(MC.screen instanceof ContainerScreen)) {
            waitTicks = 0;
            return;
        }

        var menu = MC.player.containerMenu;
        int containerSlots = menu.slots.size() - 36;
        if (containerSlots <= 0)
            return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        for (int slot = 0; slot < containerSlots; slot++) {
            if (!menu.getSlot(slot).hasItem())
                continue;
            MC.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.QUICK_MOVE, MC.player);
            waitTicks = delay.get();
            return;
        }
    }
}
