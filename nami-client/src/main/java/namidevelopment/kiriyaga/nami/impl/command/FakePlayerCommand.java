package namidevelopment.kiriyaga.nami.impl.command;

import com.mojang.authlib.GameProfile;
import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterCommand
public class FakePlayerCommand extends Command {

    private RemotePlayer fakePlayer;

    public FakePlayerCommand() {
        super("fakeplayer");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {
                new CommandRoute(null)
        };
    }

    @Override
    public void execute(String route, Object[] args) {
        if (MC.level == null || MC.player == null)
            return;

        if (fakePlayer != null) {
            EVENT_SERVICE.unregister(this);
            MC.level.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            fakePlayer = null;

            CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Fake player has been{red} removed{gray}."));
            return;
        }

        String name = "Saturnic";

        double health = 20.0f;
        fakePlayer = new RemotePlayer(MC.level, new GameProfile(UUID.randomUUID(), name));
        fakePlayer.copyPosition(MC.player);
        fakePlayer.setYRot(MC.player.getYRot());
        fakePlayer.setXRot(MC.player.getXRot());
        fakePlayer.setId(-696969);
        fakePlayer.setHealth((float) health);

        copy(MC.player, fakePlayer);
        MC.level.addEntity(fakePlayer);

        CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Fake player has been{green} added{gray}."));

        EVENT_SERVICE.register(this);
    }

    @SubscribeEvent
    public void onPreTickEvent(PreTickEvent event) {
        if (MC.level == null || MC.player == null || fakePlayer == null)
            return;

        if (fakePlayer.getHealth() <= 0.0f) {
            fakePlayer.setHealth(20.0f);
            fakePlayer.removeAllEffects();
        }

        damage(0.5f);
    }

    private void damage(float amount) {
        float newHealth = fakePlayer.getHealth() - amount;

        if (newHealth <= 0.0f) {
            fakePlayer.setHealth(0.5f);
            fakePlayer.removeAllEffects();
        } else {
            fakePlayer.setHealth(newHealth);
        }
    }

    private void copy(Player from, Player to) {
        to.setItemSlot(EquipmentSlot.MAINHAND, from.getItemBySlot(EquipmentSlot.MAINHAND).copy());
        to.setItemSlot(EquipmentSlot.OFFHAND, from.getItemBySlot(EquipmentSlot.OFFHAND).copy());
        to.setItemSlot(EquipmentSlot.HEAD, from.getItemBySlot(EquipmentSlot.HEAD).copy());
        to.setItemSlot(EquipmentSlot.CHEST, from.getItemBySlot(EquipmentSlot.CHEST).copy());
        to.setItemSlot(EquipmentSlot.LEGS, from.getItemBySlot(EquipmentSlot.LEGS).copy());
        to.setItemSlot(EquipmentSlot.FEET, from.getItemBySlot(EquipmentSlot.FEET).copy());
    }
}
