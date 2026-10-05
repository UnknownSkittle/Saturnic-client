package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.api.event.impl.RenderScreenEvent;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.NoRenderFeature;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(Screen.class)
public abstract class MixinScreen {
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    public void noBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (FEATURE_SERVICE.getStorage() == null) return;

        NoRenderFeature m = FEATURE_SERVICE.getStorage().getByClass(NoRenderFeature.class);

        if (m != null && m.isEnabled() && m.noBackground.get() && MC.level != null) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void renderInGameBackground(GuiGraphics drawContext, CallbackInfo ci) {
        if (FEATURE_SERVICE.getStorage() == null) return;

        NoRenderFeature m = FEATURE_SERVICE.getStorage().getByClass(NoRenderFeature.class);

        if (m != null && m.isEnabled() && m.noBackground.get() && MC.level != null) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EVENT_SERVICE.post(new RenderScreenEvent(context, null, mouseX, mouseY));
    }
}
