package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.KeyBindSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static namidevelopment.kiriyaga.api.NamiApi.KEYBIND_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class ClickTPFeature extends Feature {
    public final KeyBindSetting useKey = addSetting(new KeyBindSetting("Use", KeyBindSetting.KEY_NONE));
    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 64, 8, 256));

    public ClickTPFeature() {
        super("ClickTP", "Teleports to the block you are looking at when the key is pressed.", FeatureCategory.of("Movement"), "clickteleport");
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null || !KEYBIND_SERVICE.isPressedToggle(useKey)) {
            return;
        }
        if (!(MC.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        double x = hit.getBlockPos().getX() + 0.5;
        double y = hit.getBlockPos().getY() + 1.0;
        double z = hit.getBlockPos().getZ() + 0.5;
        if (MC.player.distanceToSqr(x, y, z) > range.get() * range.get()) {
            return;
        }

        MC.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
        MC.player.setPos(x, y, z);
        MC.player.fallDistance = 0;
    }
}
