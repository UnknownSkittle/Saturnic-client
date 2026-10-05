package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.PlayerIntelFeature;
import namidevelopment.kiriyaga.nami.impl.gui.screen.LocationMapScreen;

import java.time.Instant;
import java.util.Comparator;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class IntelCommand extends Command {
    public IntelCommand() {
        super("intel");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {
                new CommandRoute("start"),
                new CommandRoute("stop"),
                new CommandRoute("map"),
                new CommandRoute("base", new CommandArgument.StringArg("name", 1, 64)),
                new CommandRoute("remove", new CommandArgument.StringArg("name", 1, 64)),
                new CommandRoute("list"),
                new CommandRoute("players")
        };
    }

    @Override
    public void execute(String route, Object[] args) {
        PlayerIntelFeature feature = FEATURE_SERVICE.getStorage().getByClass(PlayerIntelFeature.class);
        if (feature == null) {
            CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{red}Player intel feature is unavailable."));
            return;
        }

        switch (route) {
            case "start" -> {
                feature.setEnabled(true);
                CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}Player location logging enabled."));
            }
            case "stop" -> {
                feature.setEnabled(false);
                CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}Player location logging disabled."));
            }
            case "map" -> MC.setScreen(new LocationMapScreen(feature));
            case "base" -> {
                String name = (String) args[0];
                if (feature.addBase(name)) {
                    CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format(
                            "{gray}Saved base {global}" + name + "{gray} at your current location."));
                } else {
                    CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{red}Join a world before saving a base."));
                }
            }
            case "remove" -> {
                String name = (String) args[0];
                if (feature.removeBase(name)) {
                    CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}Removed base {global}" + name + "{gray}."));
                } else {
                    CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}No base named {global}" + name + "{gray} exists in this world."));
                }
            }
            case "list" -> listBases(feature);
            case "players" -> listPlayers(feature);
        }
    }

    private void listBases(PlayerIntelFeature feature) {
        PlayerIntelFeature.WorldIntel world = feature.getCurrentWorldIntel();
        if (world == null || world.bases.isEmpty()) {
            CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}No bases are saved for this world."));
            return;
        }

        StringBuilder message = new StringBuilder("{gray}Bases:");
        world.bases.values().forEach(base -> message.append("\n{global}")
                .append(base.name).append("{gray} at {global}")
                .append(base.x).append(", ").append(base.y).append(", ").append(base.z));
        CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format(message.toString()));
    }

    private void listPlayers(PlayerIntelFeature feature) {
        PlayerIntelFeature.WorldIntel world = feature.getCurrentWorldIntel();
        if (world == null || world.players.isEmpty()) {
            CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}No player sightings are saved for this world."));
            return;
        }

        StringBuilder message = new StringBuilder("{gray}Recent player sightings:");
        world.players.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().latest() != null)
                .sorted(Comparator.comparingLong(entry -> -entry.getValue().latest().timestamp))
                .limit(20)
                .forEach(entry -> {
                    PlayerIntelFeature.PlayerSighting sighting = entry.getValue().latest();
                    message.append("\n{global}").append(entry.getKey())
                            .append("{gray} at {global}").append(sighting.x).append(", ")
                            .append(sighting.y).append(", ").append(sighting.z)
                            .append("{gray} (").append(Instant.ofEpochMilli(sighting.timestamp)).append(" UTC)");
                });
        CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format(message.toString()));
    }
}
