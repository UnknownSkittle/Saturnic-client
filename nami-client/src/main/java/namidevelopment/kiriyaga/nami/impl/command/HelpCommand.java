package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.stream.Collectors;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class HelpCommand extends Command {

    public HelpCommand() {
        super("help");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {
                new CommandRoute(null)
        };
    }

    @Override
    public void execute(String route, Object[] args) {
        List<Command> cmds = COMMAND_SERVICE.getStorage().getCommands();

        if (cmds.isEmpty()) {
            CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("No commands registered."));
            return;
        }

        String displayText = cmds.stream().filter(c -> c.getName() != null).map(this::getDisplay).collect(Collectors.joining(", "));

        MutableComponent message = CAT_FORMAT.format(
                "{gray}Saturnic help — type {global}?" + "saturnic{gray} in chat at any time. "
                        + "The ClickGUI opens with {global}P{gray} by default (also assignable under Client > ClickGui). "
                        + "Command prefix: {global}" + COMMAND_SERVICE.getExecutor().getPrefix()
                        + "{gray}. Available commands: " + displayText + "{white}.");
        CHAT_SERVICE.sendPersistent(this.getName(), message);
    }

    private String getDisplay(Command command) {
        String display = command.getName().replace(" ", "");
        return "{global}" + display + "{gray}";
    }
}
