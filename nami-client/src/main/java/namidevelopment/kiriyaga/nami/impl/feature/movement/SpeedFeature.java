package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import net.minecraft.util.Mth;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class SpeedFeature extends Feature {

    public enum Compatibility { DEFAULT, GRIMAC, GRIMAC_NEW, NCP, VULKAN }

    private enum Mode {
        ROTATION
    }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.ROTATION));
    public final EnumSetting<Compatibility> compatibility =
            addSetting(new EnumSetting<>("Compatibility", Compatibility.DEFAULT));
    public final BoolSetting inLiquid = addSetting(new BoolSetting("InWater", true));

    public SpeedFeature() {
        super("Speed", "Increases movement speed.", FeatureCategory.of("Movement"));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null) return;
        this.clearDisplayInfo();

        if (MC.player.isVisuallyCrawling() || MC.player.isCrouching() || MC.player.isShiftKeyDown() || MC.player.isFallFlying())
            return; // this fallback need due to sprinting not apply for theese states
        // also we do not need swimming because swimming do apply speed for sprinitng

        if (!inLiquid.get() && MC.player.isInWater())
            return;

        this.addDisplayInfo(mode.get().toString());

        if (mode.get() == Mode.ROTATION && isMoving()) {
            float targetYaw = INPUT_SERVICE.getClientHandler().getDirection();
            if (compatibility.get() == Compatibility.DEFAULT) {
                ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(
                        SpeedFeature.class.getName(), 1, targetYaw, MC.player.getXRot(), RotationsFeature.RotationMode.MOTION
                ));
                return;
            }

            float serverYaw = ROTATION_SERVICE.getStateHandler().getServerYRot();
            float yawDelta = Mth.wrapDegrees(targetYaw - serverYaw);
            float yaw = serverYaw + Mth.clamp(yawDelta, -maxRotationStep(), maxRotationStep());
            float pitch = MC.player.getXRot();
            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(SpeedFeature.class.getName(), 1, yaw, pitch, RotationsFeature.RotationMode.MOTION));
        }
    }

    private float maxRotationStep() {
        return switch (compatibility.get()) {
            case DEFAULT -> 180.0f;
            case GRIMAC -> 45.0f;
            case GRIMAC_NEW -> 30.0f;
            case NCP -> 60.0f;
            case VULKAN -> 45.0f;
        };
    }

    private boolean isMoving() {
        return MC.options.keyUp.isDown() ||
                MC.options.keyDown.isDown() ||
                MC.options.keyLeft.isDown() ||
                MC.options.keyRight.isDown();
    }
}
