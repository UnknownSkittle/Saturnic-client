package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import net.minecraft.world.phys.Vec3;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class FlightFeature extends Feature {
    public enum Compatibility { DEFAULT, GRIMAC, GRIMAC_NEW, NCP, VULKAN }

    public final EnumSetting<Compatibility> compatibility =
            addSetting(new EnumSetting<>("Compatibility", Compatibility.DEFAULT));
    public final DoubleSetting horizontalSpeed = addSetting(new DoubleSetting("Horizontal", 0.25, 0.05, 1.5));
    public final DoubleSetting verticalSpeed = addSetting(new DoubleSetting("Vertical", 0.25, 0.05, 1.0));

    private boolean previousNoGravity;

    public FlightFeature() {
        super("Flight", "Provides directional flight controls.", FeatureCategory.of("Movement"), "fly");
    }

    @Override
    public void onEnable() {
        if (MC.player != null) {
            previousNoGravity = MC.player.isNoGravity();
        }
    }

    @Override
    public void onDisable() {
        if (MC.player != null) {
            MC.player.setNoGravity(previousNoGravity);
            Vec3 velocity = MC.player.getDeltaMovement();
            MC.player.setDeltaMovement(velocity.x, 0, velocity.z);
        }
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null || MC.player.isPassenger() || MC.player.isFallFlying()) {
            return;
        }

        MC.player.setNoGravity(true);
        float forward = (MC.options.keyUp.isDown() ? 1 : 0) - (MC.options.keyDown.isDown() ? 1 : 0);
        float strafe = (MC.options.keyLeft.isDown() ? 1 : 0) - (MC.options.keyRight.isDown() ? 1 : 0);
        double yaw = Math.toRadians(MC.player.getYRot());
        double x = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
        double z = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
        double length = Math.hypot(x, z);
        double horizontalScale = horizontalScale();
        double verticalScale = verticalScale();
        if (length > 0) {
            x = x / length * horizontalSpeed.get() * horizontalScale;
            z = z / length * horizontalSpeed.get() * horizontalScale;
        }

        double y = (MC.options.keyJump.isDown() ? verticalSpeed.get() : 0)
                - (MC.options.keyShift.isDown() ? verticalSpeed.get() : 0);
        y *= verticalScale;
        double smoothing = smoothing();
        Vec3 velocity = MC.player.getDeltaMovement();
        MC.player.setDeltaMovement(
                velocity.x + (x - velocity.x) * smoothing,
                velocity.y + (y - velocity.y) * smoothing,
                velocity.z + (z - velocity.z) * smoothing
        );
        MC.player.fallDistance = 0;
    }

    private double horizontalScale() {
        return switch (compatibility.get()) {
            case DEFAULT -> 1.0;
            case GRIMAC -> 0.65;
            case GRIMAC_NEW -> 0.5;
            case NCP -> 0.75;
            case VULKAN -> 0.6;
        };
    }

    private double verticalScale() {
        return switch (compatibility.get()) {
            case DEFAULT -> 1.0;
            case GRIMAC -> 0.5;
            case GRIMAC_NEW -> 0.35;
            case NCP -> 0.65;
            case VULKAN -> 0.45;
        };
    }

    private double smoothing() {
        return switch (compatibility.get()) {
            case DEFAULT -> 1.0;
            case GRIMAC -> 0.25;
            case GRIMAC_NEW -> 0.15;
            case NCP -> 0.35;
            case VULKAN -> 0.2;
        };
    }
}
