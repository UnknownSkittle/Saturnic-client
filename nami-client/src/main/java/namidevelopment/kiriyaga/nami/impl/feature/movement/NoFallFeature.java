package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class NoFallFeature extends Feature {
    public final DoubleSetting minFallDistance = addSetting(new DoubleSetting("MinFallDistance", 3.0, 1.0, 20.0));

    public NoFallFeature() {
        super("NoFall", "Reports grounded movement after a fall-distance threshold.", FeatureCategory.of("Movement"), "nofall");
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket packet)
                || packet.isOnGround()
                || MC.player == null
                || MC.level == null
                || MC.player.isCreative()
                || MC.player.isFallFlying()
                || MC.player.onGround()
                || MC.player.fallDistance < minFallDistance.get()) {
            return;
        }

        event.cancel();

        boolean horizontalCollision = packet.horizontalCollision();
        ServerboundMovePlayerPacket groundedPacket;
        if (packet instanceof ServerboundMovePlayerPacket.PosRot posRot) {
            groundedPacket = new ServerboundMovePlayerPacket.PosRot(
                    posRot.getX(MC.player.getX()),
                    posRot.getY(MC.player.getY()),
                    posRot.getZ(MC.player.getZ()),
                    posRot.getYRot(MC.player.getYRot()),
                    posRot.getXRot(MC.player.getXRot()),
                    true,
                    horizontalCollision
            );
        } else if (packet instanceof ServerboundMovePlayerPacket.Pos pos) {
            groundedPacket = new ServerboundMovePlayerPacket.Pos(
                    pos.getX(MC.player.getX()),
                    pos.getY(MC.player.getY()),
                    pos.getZ(MC.player.getZ()),
                    true,
                    horizontalCollision
            );
        } else if (packet instanceof ServerboundMovePlayerPacket.Rot rot) {
            groundedPacket = new ServerboundMovePlayerPacket.Rot(
                    rot.getYRot(MC.player.getYRot()),
                    rot.getXRot(MC.player.getXRot()),
                    true,
                    horizontalCollision
            );
        } else {
            groundedPacket = new ServerboundMovePlayerPacket.StatusOnly(true, horizontalCollision);
        }

        MC.player.connection.send(groundedPacket);
    }
}
