package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.api.event.impl.KeyInputEvent;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import namidevelopment.kiriyaga.api.model.setting.KeyBindSetting;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
        public void onKey(long l, int i, KeyEvent keyInput, CallbackInfo ci) {
        if (keyInput.input() == GLFW.GLFW_KEY_F3) return;

        KeyInputEvent event = new KeyInputEvent(keyInput.input(), keyInput.scancode(), i, keyInput.modifiers());
        EVENT_SERVICE.post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyClickgui(long l, int i, KeyEvent keyInput, CallbackInfo ci) {
        if (MC == null) return;
        if (i != GLFW.GLFW_PRESS) return;

        if (FEATURE_SERVICE.getStorage() == null) return;

        ClickGuiFeature clickGui = FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class);
        if (clickGui == null) return;
        clickGui.ensureKeyboardKeyBind();

        KeyBindSetting bind = clickGui.getKeyBind();
        if (bind == null) return;

        if (bind.get() == keyInput.input()) {
            Screen screen = MC.screen;

            if (screen instanceof TitleScreen
                    || screen instanceof SelectWorldScreen
                    || screen instanceof JoinMultiplayerScreen) {

                if (!(screen.getFocused() instanceof MultilineTextField)) {
                    clickGui.toggle();
                }
            }
        }
    }

}
