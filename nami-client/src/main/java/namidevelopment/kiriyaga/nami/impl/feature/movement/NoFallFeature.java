package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class NoFallFeature extends Feature {
    public enum Mode { PACKET, GRIMAC, GRIMAC_NEW, NCP, VULKAN }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.PACKET));
    public final DoubleSetting minFallDistance = addSetting(new DoubleSetting("MinFallDistance", 3.0, 1.0, 20.0));
    private boolean grimNewGroundSent;

    public NoFallFeature() {
        super("NoFall", "Reports grounded movement after a fall-distance threshold.", FeatureCategory.of("Movement"), "nofall");
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket packet)) {
            return;
        }

        if (MC.player == null || MC.level == null) {
            grimNewGroundSent = false;
            return;
        }

        if (MC.player.onGround()) {
            grimNewGroundSent = false;
            return;
        }

        if (packet.isOnGround()) {
            return;
        }

        if (MC.player.isCreative()
                || MC.player.isFallFlying()
                || MC.player.fallDistance < minFallDistance.get()) {
            grimNewGroundSent = false;
            return;
        }

        switch (mode.get()) {
            case PACKET -> replaceWithGroundedMovement(event, packet);
            case GRIMAC -> {
                if (packet.hasPosition()) {
                    sendGroundedStatus();
                }
            }
            case GRIMAC_NEW -> {
                if (!grimNewGroundSent) {
                    sendGroundedStatus();
                    grimNewGroundSent = true;
                }
            }
            case NCP -> {
                if (packet.hasPosition()) {
                    replaceWithGroundedMovement(event, packet);
                }
            }
            case VULKAN -> {
                sendGroundedStatus();
            }
        }
    }

    private void replaceWithGroundedMovement(PacketSendEvent event, ServerboundMovePlayerPacket packet) {
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

    private void sendGroundedStatus() {
        MC.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, MC.player.horizontalCollision));
    }
}
