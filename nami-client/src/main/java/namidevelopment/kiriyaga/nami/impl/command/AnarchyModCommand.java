package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import namidevelopment.kiriyaga.nami.impl.gui.screen.AnarchyModDownloadScreen;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterCommand
public class AnarchyModCommand extends Command {
    public AnarchyModCommand() {
        super("anarchymod");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[]{new CommandRoute(null)};
    }

    @Override
    public void execute(String route, Object[] args) {
        MC.setScreen(new AnarchyModDownloadScreen());
    }
}
