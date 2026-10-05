package namidevelopment.kiriyaga.api.core.command;


import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.ChatMessageEvent;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandSource;

import java.util.ArrayList;
import java.util.List;
import static namidevelopment.kiriyaga.api.NamiApi.*;

public class CommandExecutor {

    private final CommandSuggester suggester;
    private String prefix = "-";

    public CommandExecutor(CommandSuggester suggester) {
        this.suggester = suggester;
    }

    @SubscribeEvent
    public void onChatMessage(ChatMessageEvent event) {
        String message = event.getMessage();
        String normalizedMessage = message.trim();
        if (normalizedMessage.equalsIgnoreCase("?saturnic")
                || normalizedMessage.equalsIgnoreCase("?saturnic help")) {
            event.setCancelled(true);
            try {
                suggester.getDispatcher().execute("help", new CommandSource());
            } catch (Exception e) {
                CHAT_SERVICE.sendPersistent("CommandExecutor",
                        CAT_FORMAT.format("{red}Unable to display Saturnic help: " + e.getMessage()));
            }
            return;
        }

        if (!message.startsWith(prefix)) return;

        event.setCancelled(true);

        String input = message.substring(prefix.length()).trim();
        if (input.isEmpty()) return;

        try {
            suggester.getDispatcher().execute(input, new CommandSource());
        } catch (Exception e) {
            CHAT_SERVICE.sendPersistent("CommandExecutor",
                    CAT_FORMAT.format("{red}Invalid command input: " + e.getMessage()));
        }
    }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            this.prefix = "-";
        } else {
            this.prefix = prefix;
        }
    }
}
