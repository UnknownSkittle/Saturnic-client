package namidevelopment.kiriyaga.nami.impl.feature.movement;

import com.mojang.blaze3d.platform.InputConstants;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.RotationsFeatureConfig;
import namidevelopment.kiriyaga.api.core.rotation.RotationStateHandler;
import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.nami.mixin.DuckKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.util.concurrent.ThreadLocalRandom;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterFeature
public class SpeedFeature extends Feature {

    // GRIMAC / GRIMAC_NEW / NCP / VULKAN keep their old names so saved configs still load.
    public enum Compatibility { DEFAULT, GRIMAC, GRIMAC_NEW, NCP, VULKAN, AVA }

    private enum Mode {
        ROTATION
    }

    private static final String ID = SpeedFeature.class.getName();

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.ROTATION));
    public final EnumSetting<Compatibility> compatibility =
            addSetting(new EnumSetting<>("Compatibility", Compatibility.DEFAULT));
    public final BoolSetting inLiquid = addSetting(new BoolSetting("InWater", true));

    public final DoubleSetting step = addSetting(new DoubleSetting("Step", 0, 0, 180));

    public final BoolSetting smartYaw = addSetting(new BoolSetting("SmartYaw", true));
    /** Round the yaw step to the mouse-sensitivity grid (what a real mouse can produce). */
    public final BoolSetting gcdSnap = addSetting(new BoolSetting("GcdSnap", true));
    public final BoolSetting randomize = addSetting(new BoolSetting("Randomize", true));

    public final BoolSetting fixKeys = addSetting(new BoolSetting("FixKeys", false));

    public SpeedFeature() {
        super("Speed", "Increases movement speed.", FeatureCategory.of("Movement"));
    }

    @Override
    public void onDisable() {
        if (ROTATION_SERVICE != null)
            ROTATION_SERVICE.getRequestHandler().cancel(ID);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreTickKeys(PreTickEvent event) {
        if (!fixKeys.get() || MC.player == null || MC.screen != null) return;

        MC.options.keyUp.setDown(physicallyDown(MC.options.keyUp));
        MC.options.keyDown.setDown(physicallyDown(MC.options.keyDown));
        MC.options.keyLeft.setDown(physicallyDown(MC.options.keyLeft));
        MC.options.keyRight.setDown(physicallyDown(MC.options.keyRight));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;
        this.clearDisplayInfo();

        LocalPlayer player = MC.player;

        if (blocked(player) || !isMoving()) {
            unwind(player);
            return;
        }

        this.addDisplayInfo(mode.get().toString());

        if (mode.get() != Mode.ROTATION) return;

        RotationStateHandler state = ROTATION_SERVICE.getStateHandler();
        float realYaw = player.getYRot();
        float serverYaw = state.getServerYRot();

        float yaw = stepToward(serverYaw, targetYaw(realYaw, serverYaw), stepLimit());

        // Nothing to spoof: already facing the real yaw and the keys need no rotation.
        if (!state.isRotating() && Math.abs(Mth.wrapDegrees(yaw - realYaw)) < 0.5f) return;

        submit(yaw, player.getXRot());
    }

    // ---------------------------------------------------------------- rotation


    private float targetYaw(float realYaw, float serverYaw) {
        int[] axes = axes();
        int x = axes[0], z = axes[1];
        if (x == 0 && z == 0) return realYaw;

        if (!smartYaw.get()) {
            if (z > 0) return realYaw;
            if (z < 0) return realYaw + 180f;
            return realYaw + (x > 0 ? 90f : -90f);
        }

        float direction = realYaw + (float) Math.toDegrees(Math.atan2(x, z));
        float best = direction;
        float bestDistance = Float.MAX_VALUE;
        for (int k = -1; k <= 1; k++) {
            float candidate = direction - 45f * k;
            float distance = Math.abs(Mth.wrapDegrees(candidate - serverYaw));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private float stepToward(float from, float to, float maxStep) {
        float delta = Mth.wrapDegrees(to - from);
        float move = Mth.clamp(delta, -maxStep, maxStep);
        if (gcdSnap.get() && move != 0f) move = snapToGcd(move);
        return from + move;
    }

    private void unwind(LocalPlayer player) {
        if (ROTATION_SERVICE == null) return;

        RotationRequest active = ROTATION_SERVICE.getRequestHandler().getActiveRequest();
        if (active != null && !ID.equals(active.id)) return; // another feature owns the rotation

        RotationStateHandler state = ROTATION_SERVICE.getStateHandler();
        if (!state.isRotating()) return;

        float serverYaw = state.getServerYRot();
        float delta = Mth.wrapDegrees(player.getYRot() - serverYaw);
        if (Math.abs(delta) <= threshold()) return; // the handler returns the small rest by itself

        submit(stepToward(serverYaw, serverYaw + delta, stepLimit()), player.getXRot());
    }

    private void submit(float yaw, float pitch) {
        ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(
                ID, 1, yaw, pitch, RotationsFeatureConfig.RotationMode.MOTION
        ));
    }

    // ------------------------------------------------------------------- step

    private float profileStep() {
        return switch (compatibility.get()) {
            case DEFAULT -> 180.0f;
            case GRIMAC -> 45.0f;
            case GRIMAC_NEW -> 30.0f;
            case NCP -> 90.0f;
            case VULKAN -> 45.0f;
            case AVA -> 90.0f;
        };
    }

    private float stepLimit() {
        float limit = step.get() > 0 ? step.get().floatValue() : profileStep();

        if (randomize.get() && limit < 180f)
            limit *= 0.85f + ThreadLocalRandom.current().nextFloat() * 0.15f;

        return Math.max(limit, threshold() + 1f);
    }

    private float threshold() {
        RotationsFeatureConfig config = FeatureContractService.get(RotationsFeatureConfig.class);
        return config != null ? (float) config.getRotationThreshold() : 5f;
    }

    private float snapToGcd(float delta) {
        double sensitivity = MC.options.sensitivity().get() * 0.6 + 0.2;
        float gcd = (float) (sensitivity * sensitivity * sensitivity * 8.0 * 0.15);
        if (gcd <= 0f) return delta;
        return Math.round(delta / gcd) * gcd;
    }

    // ------------------------------------------------------------------ input

    private boolean blocked(LocalPlayer player) {
        if (player.isVisuallyCrawling() || player.isCrouching() || player.isShiftKeyDown() || player.isFallFlying())
            return true;
        if (player.isPassenger() || player.onClimbable() || player.isInLava() || player.getAbilities().flying)
            return true;
        return !inLiquid.get() && player.isInWater();
    }

    private boolean isMoving() {
        return MC.options.keyUp.isDown() ||
                MC.options.keyDown.isDown() ||
                MC.options.keyLeft.isDown() ||
                MC.options.keyRight.isDown();
    }

    /** {x, z}: x = right minus left, z = forward minus back, from the same cache getDirection() used. */
    private int[] axes() {
        var cache = INPUT_SERVICE.getInputCache();
        int x = (cache.right() ? 1 : 0) - (cache.left() ? 1 : 0);
        int z = (cache.forward() ? 1 : 0) - (cache.back() ? 1 : 0);
        return new int[]{x, z};
    }

    private static boolean physicallyDown(KeyMapping key) {
        int keyCode = ((DuckKeyMapping) key).getKey().getValue();
        return InputConstants.isKeyDown(MC.getWindow(), keyCode);
    }
}
