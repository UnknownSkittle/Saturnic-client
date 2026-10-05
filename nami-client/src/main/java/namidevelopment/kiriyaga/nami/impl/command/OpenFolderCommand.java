package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;

import java.awt.Desktop;
import java.io.IOException;
import java.io.File;
import java.util.Locale;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class OpenFolderCommand extends Command {

    public OpenFolderCommand() {
        super("openfolder");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {new CommandRoute(null)};
    }

    @Override
    public void execute(String route, Object[] args) {
        try {
            File dir = CONFIG_SERVICE.getDirectoryProvider().getBaseDir();
            if (!dir.exists() && !dir.mkdirs()) {
                CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Failed to create folder: {global}" + dir.getAbsolutePath() + "{gray}."));
                return;
            }

            boolean opened = tryDesktopOpen(dir) || tryOsOpen(dir);
            if (opened) {
                CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Opened folder: {global}" + dir.getAbsolutePath() + "{gray}."));
            } else {
                CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Could not open folder automatically. Path: {global}" + dir.getAbsolutePath() + "{gray}."));
            }
        } catch (Exception e) {
            CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Failed to open folder: {global}" + e.getMessage() + "{gray}."));
            LOGGER.error("Failed to open config folder", e);
        }
    }

    private boolean tryDesktopOpen(File dir) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
                return true;
            }
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            LOGGER.debug("Desktop folder opening is unavailable", e);
        }
        return false;
    }

    private boolean tryOsOpen(File dir) {
        String path = dir.getAbsolutePath();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (os.startsWith("windows")) {
            return startCommand("explorer", path);
        }
        if (os.startsWith("mac")) {
            return startCommand("open", path);
        }
        return startCommand("xdg-open", path)
                || startCommand("gio", "open", path)
                || startCommand("open", path);
    }

    private boolean startCommand(String... command) {
        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException | SecurityException | UnsupportedOperationException e) {
            LOGGER.debug("Unable to start folder opener: " + command[0], e);
            return false;
        }
    }
}
