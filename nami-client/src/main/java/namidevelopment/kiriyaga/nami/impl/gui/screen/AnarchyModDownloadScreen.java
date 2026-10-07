package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.zip.ZipFile;

import static namidevelopment.kiriyaga.api.NamiApi.CONFIG_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.LOGGER;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

public class AnarchyModDownloadScreen extends NamiScreen {
    private static final URI DOWNLOAD_URI = URI.create(
            "https://anarchymod-releases.6b6t.org/1.4.3/anarchymod-mc-1.21.11-1.4.3.jar");
    private static final String FILE_NAME = "anarchymod-mc-1.21.11-1.4.3.jar";
    private static final long MAX_DOWNLOAD_BYTES = 100L * 1024 * 1024;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final net.minecraft.client.gui.screens.Screen parent;
    private String status = "";
    private boolean downloading;
    private boolean choiceComplete;
    private Button yesButton;
    private Button noButton;

    public AnarchyModDownloadScreen() {
        this(MC.screen);
    }

    public AnarchyModDownloadScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(Component.literal("AnarchyMod setup"));
        this.parent = parent;
    }

    public static boolean shouldPrompt() {
        return !Files.exists(getDecisionFile());
    }

    private static Path getDecisionFile() {
        return CONFIG_SERVICE.getDirectoryProvider().getBaseDir().toPath()
                .resolve("anarchymod-setup.txt");
    }

    private boolean saveDecision(String decision) {
        Path decisionFile = getDecisionFile();
        try {
            Files.createDirectories(decisionFile.getParent());
            Files.writeString(decisionFile, decision + System.lineSeparator());
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save AnarchyMod setup choice to " + decisionFile, e);
            return false;
        }
    }

    @Override
    protected void init() {
        yesButton = addRenderableWidget(Button.builder(Component.literal("Yes, download and install"), button -> startDownload())
                .bounds(width / 2 - 154, height / 2 + 38, 190, 20)
                .build());
        noButton = addRenderableWidget(Button.builder(Component.literal("No thanks"), button -> decline())
                .bounds(width / 2 + 44, height / 2 + 38, 110, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xE611151B);
        drawCentered(graphics, "Would you like to download AnarchyMod to play on 6b6t?",
                height / 2 - 50, 0xFFFFFFFF);
        drawCentered(graphics, "Yes will download and install the third-party mod for your next launch.",
                height / 2 - 29, 0xFFD4DEDF);
        drawCentered(graphics, "Only continue if you trust the publisher. Source: anarchymod-releases.6b6t.org",
                height / 2 - 12, 0xFFFFC8A0);
        if (!status.isEmpty()) {
            drawCentered(graphics, status, height / 2 + 12, 0xFFEAF4F4);
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void drawCentered(GuiGraphics graphics, String text, int y, int color) {
        graphics.drawString(font, text, (width - font.width(text)) / 2, y, color);
    }

    private void startDownload() {
        if (downloading || choiceComplete) return;

        Path modsDirectory = MC.gameDirectory.toPath().resolve("mods");
        Path destination = modsDirectory.resolve(FILE_NAME);
        if (Files.exists(destination)) {
            status = "AnarchyMod is already in your mods folder; it was not replaced.";
            finishChoice("installed");
            showDoneButton();
            return;
        }

        downloading = true;
        yesButton.active = false;
        noButton.active = false;
        status = "Downloading and validating AnarchyMod...";

        CompletableFuture.runAsync(() -> downloadAndInstall(destination))
                .whenComplete((ignored, error) -> MC.execute(() -> {
                    downloading = false;
                    if (error == null) {
                        status = "Installed. Restart Minecraft to load AnarchyMod.";
                        finishChoice("installed");
                        showDoneButton();
                    } else {
                        Throwable cause = error;
                        while (cause instanceof CompletionException && cause.getCause() != null) {
                            cause = cause.getCause();
                        }
                        status = "Download/install failed: " + cause.getMessage();
                        LOGGER.error("Failed to download or install AnarchyMod from " + DOWNLOAD_URI, cause);
                        yesButton.active = true;
                        noButton.active = true;
                    }
                }));
    }

    private void decline() {
        if (downloading || choiceComplete) return;
        choiceComplete = true;
        saveDecision("declined");
        MC.setScreen(parent);
    }

    private void finishChoice(String decision) {
        choiceComplete = true;
        if (!saveDecision(decision)) {
            status = "Installed, but the one-time choice could not be saved.";
        }
    }

    private void showDoneButton() {
        yesButton.visible = false;
        noButton.visible = false;
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> MC.setScreen(parent))
                .bounds(width / 2 - 50, height / 2 + 38, 100, 20)
                .build());
    }

    private void downloadAndInstall(Path destination) {
        Path modsDirectory = destination.getParent();
        Path temporaryFile = null;

        try {
            HttpRequest request = HttpRequest.newBuilder(DOWNLOAD_URI)
                    .timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "Saturnic Minecraft client")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream input = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IOException("Server returned HTTP " + response.statusCode());
                }

                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (contentLength > MAX_DOWNLOAD_BYTES) {
                    throw new IOException("The download exceeds the 100 MiB size limit");
                }

                Files.createDirectories(modsDirectory);
                temporaryFile = Files.createTempFile(modsDirectory, "anarchymod-", ".part");
                long bytes = copyWithLimit(input, temporaryFile);
                if (bytes == 0) {
                    throw new IOException("The server returned an empty file");
                }
            }

            validateFabricModJar(temporaryFile);
            Files.move(temporaryFile, destination);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        } catch (IOException e) {
            throw new CompletionException(e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException e) {
                    LOGGER.warn("Failed to remove temporary AnarchyMod download " + temporaryFile, e);
                }
            }
        }
    }

    private long copyWithLimit(InputStream input, Path destination) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        try (var output = Files.newOutputStream(destination)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new IOException("The download exceeds the 100 MiB size limit");
                }
                output.write(buffer, 0, read);
            }
        }
        return total;
    }

    private void validateFabricModJar(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            if (zip.getEntry("fabric.mod.json") == null) {
                throw new IOException("The downloaded JAR does not contain fabric.mod.json");
            }
        } catch (java.util.zip.ZipException e) {
            throw new IOException("The downloaded file is not a valid JAR archive", e);
        }
    }

    @Override
    public void onClose() {
        if (downloading) return;
        if (!choiceComplete) {
            decline();
        } else {
            MC.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
