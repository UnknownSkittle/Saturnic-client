package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.nami.impl.feature.visuals.PlayerIntelFeature;
import namidevelopment.kiriyaga.nami.impl.gui.IntelMapRenderer;
import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Rectangle;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

public class LocationMapScreen extends NamiScreen {
    private final PlayerIntelFeature intelFeature;
    private double centerX;
    private double centerZ;
    private double radius = 256;
    private boolean dragging;
    private Rectangle mapBounds = new Rectangle();

    public LocationMapScreen(PlayerIntelFeature intelFeature) {
        super(Component.literal("Player location heatmap"));
        this.intelFeature = intelFeature;
        recenter();
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Recenter"), button -> recenter())
                .bounds(width - 104, 10, 92, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xE611151B);
        graphics.drawString(font, "Player Intel Map", 14, 15, 0xFFFFFFFF);
        graphics.drawString(font, String.format("Center: %.0f, %.0f   View radius: %.0f blocks",
                centerX, centerZ, radius), 14, 31, 0xFFB8C6C9);

        int availableSize = Math.max(1, Math.min(width - 32, height - 100));
        int mapSize = Math.max(1, Math.min(availableSize, 1000));
        int left = (width - mapSize) / 2;
        int top = 58 + Math.max(0, (height - 100 - mapSize) / 2);
        mapBounds = new Rectangle(left, top, mapSize, mapSize);

        PlayerIntelFeature.WorldIntel world = intelFeature.getCurrentWorldIntel();
        boolean hasPlayer = MC.player != null;
        double playerX = hasPlayer ? MC.player.getX() : 0;
        double playerZ = hasPlayer ? MC.player.getZ() : 0;
        IntelMapRenderer.drawMap(graphics, font, world, mapBounds, centerX, centerZ, radius,
                hasPlayer, playerX, playerZ, true);

        graphics.drawString(font, "Scroll: zoom   Drag: pan   Cyan: you   Red: player sightings   Green: bases",
                14, height - 28, 0xFFD4DEDF);
        graphics.drawString(font, "Heat fades with age; visit density is sampled locally.   Esc: close",
                14, height - 15, 0xFF9EABAE);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && mapBounds.contains(event.x(), event.y())) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == 0) {
            double scale = mapBounds.width / (radius * 2.0);
            centerX -= deltaX / scale;
            centerZ -= deltaY / scale;
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!mapBounds.contains(mouseX, mouseY)) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        double oldScale = mapBounds.width / (radius * 2.0);
        double worldXUnderCursor = centerX + (mouseX - mapBounds.getCenterX()) / oldScale;
        double worldZUnderCursor = centerZ + (mouseY - mapBounds.getCenterY()) / oldScale;
        radius = Math.max(16, Math.min(8192, radius * (verticalAmount > 0 ? 0.8 : 1.25)));
        double newScale = mapBounds.width / (radius * 2.0);
        centerX = worldXUnderCursor - (mouseX - mapBounds.getCenterX()) / newScale;
        centerZ = worldZUnderCursor - (mouseY - mapBounds.getCenterY()) / newScale;
        return true;
    }

    private void recenter() {
        if (MC.player != null) {
            centerX = MC.player.getX();
            centerZ = MC.player.getZ();
        } else {
            centerX = 0;
            centerZ = 0;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
