package namidevelopment.kiriyaga.nami.impl.util;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

public final class ContainerActions {
    private ContainerActions() {}

    public static void transferContainer(AbstractContainerMenu menu, boolean toPlayer) {
        if (MC.player == null || MC.gameMode == null || menu != MC.player.containerMenu)
            return;

        int containerSlots = menu.slots.size() - 36;
        if (containerSlots <= 0)
            return;

        int first = toPlayer ? 0 : containerSlots;
        int limit = toPlayer ? containerSlots : menu.slots.size();
        int step = toPlayer ? 1 : -1;
        int slot = toPlayer ? first : limit - 1;
        while (toPlayer ? slot < limit : slot >= first) {
            if (menu.getSlot(slot).hasItem()) {
                MC.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.QUICK_MOVE, MC.player);
            }
            slot += step;
        }
    }
}
