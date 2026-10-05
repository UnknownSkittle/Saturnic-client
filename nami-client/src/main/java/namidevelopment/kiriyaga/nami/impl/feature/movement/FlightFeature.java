package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import net.minecraft.world.phys.Vec3;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class FlightFeature extends Feature {
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
        if (length > 0) {
            x = x / length * horizontalSpeed.get();
            z = z / length * horizontalSpeed.get();
        }

        double y = (MC.options.keyJump.isDown() ? verticalSpeed.get() : 0)
                - (MC.options.keyShift.isDown() ? verticalSpeed.get() : 0);
        MC.player.setDeltaMovement(x, y, z);
        MC.player.fallDistance = 0;
    }
}
