package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.nami.impl.feature.visuals.PlayerIntelFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

public class LocationMapScreen extends NamiScreen {
    private static final int MAP_RANGE = 128;
    private final PlayerIntelFeature intelFeature;

    public LocationMapScreen(PlayerIntelFeature intelFeature) {
        super(Component.literal("Player location heatmap"));
        this.intelFeature = intelFeature;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xE611151B);
        graphics.drawCenteredString(font, "Player Intel Heatmap", width / 2, 14, 0xFFFFFFFF);

        int size = Math.max(120, Math.min(Math.min(width - 48, height - 100), 440));
        int left = (width - size) / 2;
        int top = (height - size) / 2;
        int centerX = MC.player == null ? 0 : MC.player.blockPosition().getX();
        int centerZ = MC.player == null ? 0 : MC.player.blockPosition().getZ();
        float scale = (float) size / (MAP_RANGE * 2);

        graphics.fill(left, top, left + size, top + size, 0xFF202A2D);
        drawGrid(graphics, left, top, size);

        PlayerIntelFeature.WorldIntel world = intelFeature.getCurrentWorldIntel();
        if (world != null) {
            for (PlayerIntelFeature.HeatCell cell : world.heat.values()) {
                float worldX = cell.cellX * PlayerIntelFeature.HEAT_CELL_SIZE
                        + PlayerIntelFeature.HEAT_CELL_SIZE / 2.0f;
                float worldZ = cell.cellZ * PlayerIntelFeature.HEAT_CELL_SIZE
                        + PlayerIntelFeature.HEAT_CELL_SIZE / 2.0f;
                int mapX = left + size / 2 + Math.round((worldX - centerX) * scale);
                int mapY = top + size / 2 + Math.round((worldZ - centerZ) * scale);
                int cellSize = Math.max(2, Math.round(PlayerIntelFeature.HEAT_CELL_SIZE * scale));
                if (inside(mapX, mapY, left, top, size)) {
                    graphics.fill(mapX - cellSize / 2, mapY - cellSize / 2,
                            mapX + cellSize / 2, mapY + cellSize / 2, heatColor(cell.visits));
                }
            }

            drawPlayerSightings(graphics, world, left, top, size, centerX, centerZ, scale);
            drawBases(graphics, world, left, top, size, centerX, centerZ, scale);
        }

        int centerScreenX = left + size / 2;
        int centerScreenY = top + size / 2;
        graphics.fill(centerScreenX - 3, centerScreenY - 3, centerScreenX + 4, centerScreenY + 4, 0xFF60C8FF);
        graphics.drawCenteredString(font, "You", centerScreenX, centerScreenY + 7, 0xFFBFEAFF);
        graphics.drawCenteredString(font, "Use intel base/remove commands to manage markers   |   Esc: close",
                width / 2, height - 26, 0xFFCCCCCC);
    }

    private void drawPlayerSightings(GuiGraphics graphics, PlayerIntelFeature.WorldIntel world,
                                     int left, int top, int size, int centerX, int centerZ, float scale) {
        world.players.forEach((name, history) -> {
            if (history == null || history.latest() == null) {
                return;
            }
            PlayerIntelFeature.PlayerSighting sighting = history.latest();
            int x = left + size / 2 + Math.round((sighting.x - centerX) * scale);
            int y = top + size / 2 + Math.round((sighting.z - centerZ) * scale);
            if (inside(x, y, left, top, size)) {
                graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFF6B6B);
                graphics.drawString(font, name, x + 4, y - 4, 0xFFFFD6D6, true);
            }
        });
    }

    private void drawBases(GuiGraphics graphics, PlayerIntelFeature.WorldIntel world,
                           int left, int top, int size, int centerX, int centerZ, float scale) {
        for (PlayerIntelFeature.BaseMarker base : world.bases.values()) {
            int x = left + size / 2 + Math.round((base.x - centerX) * scale);
            int y = top + size / 2 + Math.round((base.z - centerZ) * scale);
            if (inside(x, y, left, top, size)) {
                graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFF65E58B);
                graphics.drawString(font, base.name, x + 5, y - 4, 0xFFD8FFE3, true);
            }
        }
    }

    private void drawGrid(GuiGraphics graphics, int left, int top, int size) {
        int gridStep = Math.max(1, size / 8);
        for (int offset = gridStep; offset < size; offset += gridStep) {
            graphics.fill(left + offset, top, left + offset + 1, top + size, 0x403C494B);
            graphics.fill(left, top + offset, left + size, top + offset + 1, 0x403C494B);
        }
    }

    private boolean inside(int x, int y, int left, int top, int size) {
        return x >= left && x < left + size && y >= top && y < top + size;
    }

    private int heatColor(int visits) {
        float intensity = Math.min(1.0f, (float) Math.log1p(visits) / (float) Math.log(16));
        int red = 255;
        int green = Math.round(210 * (1.0f - intensity));
        int blue = Math.round(45 * (1.0f - intensity));
        int alpha = Math.round(70 + 150 * intensity);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
