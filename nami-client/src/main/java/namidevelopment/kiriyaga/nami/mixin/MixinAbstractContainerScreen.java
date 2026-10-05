package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.api.event.impl.MouseClickEvent;
import namidevelopment.kiriyaga.api.event.impl.MouseScrollEvent;
import namidevelopment.kiriyaga.api.event.impl.RenderSlotsEvent;
import namidevelopment.kiriyaga.api.event.impl.RenderTooltipEvent;
import namidevelopment.kiriyaga.nami.impl.feature.client.PatchFeature;
import namidevelopment.kiriyaga.nami.impl.util.ContainerActions;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen<T extends AbstractContainerMenu> {
    @Shadow
    @Final
    protected T menu;
    @Shadow protected Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow public int imageWidth;

    @Inject(method = "init", at = @At("TAIL"))
    private void addChestActions(CallbackInfo ci) {
        if (!(((Object) this) instanceof ContainerScreen) || MC.player == null
                || MC.player.containerMenu.slots.size() <= 36)
            return;

        int buttonY = Math.max(2, topPos - 21);
        DuckScreen screen = (DuckScreen) this;
        screen.nami$addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.literal("Loot"), button ->
                        ContainerActions.transferContainer(menu, true))
                .bounds(leftPos + imageWidth - 100, buttonY, 48, 18)
                .build());
        screen.nami$addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.literal("Store"), button ->
                        ContainerActions.transferContainer(menu, false))
                .bounds(leftPos + imageWidth - 50, buttonY, 48, 18)
                .build());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent click, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        MouseClickEvent event = new MouseClickEvent(click.x(), click.y(), click.button());
        EVENT_SERVICE.post(event);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        MouseScrollEvent event = new MouseScrollEvent(mouseX, mouseY, verticalAmount);
        EVENT_SERVICE.post(event);
    }

    /*
    Author: EnderKill98 (github.com/EnderKill98)
    Licensed under MIT (2025)
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void mouseDragged(MouseButtonEvent mouseButtonEvent, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        if (!FEATURE_SERVICE.getStorage().getByClass(PatchFeature.class).slotDragDesync.get()) return;

        if (MC.player != null && MC.player.getAbilities().instabuild) return;
        ItemStack cursorStack = menu.getCarried();
        if (cursorStack == null || cursorStack.isEmpty()) return;
        if (!cursorStack.isStackable() || cursorStack.getItem() instanceof MapItem || cursorStack.getItem() instanceof BannerItem)
            cir.setReturnValue(true);
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    protected void onRenderTooltip(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        ItemStack stack = hoveredSlot != null ? hoveredSlot.getItem() : ItemStack.EMPTY;
        RenderTooltipEvent event = new RenderTooltipEvent(graphics, mouseX, mouseY, stack);
        EVENT_SERVICE.post(event);

        if (event.isCancelled())
            ci.cancel();
    }

    @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlots(Lnet/minecraft/client/gui/GuiGraphics;II)V", shift = At.Shift.AFTER))
    private void onRenderSlots(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        RenderSlotsEvent event = new RenderSlotsEvent(graphics, mouseX, mouseY, menu.slots);
        EVENT_SERVICE.post(event);
    }
}
