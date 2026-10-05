package namidevelopment.kiriyaga.nami.impl.gui;

import namidevelopment.kiriyaga.nami.impl.feature.visuals.PlayerIntelFeature;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.awt.Rectangle;
import java.util.Collection;

public final class IntelMapRenderer {
    private static final int GRID_BLOCKS = 32;

    private IntelMapRenderer() {}

    public static void drawMap(GuiGraphics graphics, Font font, PlayerIntelFeature.WorldIntel world,
                               Rectangle bounds, double centerX, double centerZ, double radius,
                               boolean showPlayer, double playerX, double playerZ, boolean labels) {
        int left = bounds.x;
        int top = bounds.y;
        int size = bounds.width;
        int right = left + size;
        int bottom = top + size;
        double scale = size / (radius * 2.0);

        graphics.fill(left, top, right, bottom, 0xFF11191C);
        drawGrid(graphics, left, top, size, centerX, centerZ, radius);
        graphics.enableScissor(left, top, right, bottom);

        if (world != null) {
            int maxVisits = world.heat.values().stream()
                    .mapToInt(cell -> cell.visits)
                    .max()
                    .orElse(1);
            long now = System.currentTimeMillis();
            for (PlayerIntelFeature.HeatCell cell : world.heat.values()) {
                if (cell == null || cell.visits <= 0) continue;

                double worldX = (double) cell.cellX * PlayerIntelFeature.HEAT_CELL_SIZE
                        + PlayerIntelFeature.HEAT_CELL_SIZE / 2.0;
                double worldZ = (double) cell.cellZ * PlayerIntelFeature.HEAT_CELL_SIZE
                        + PlayerIntelFeature.HEAT_CELL_SIZE / 2.0;
                int x = worldToScreenX(left, size, worldX, centerX, scale);
                int y = worldToScreenY(top, size, worldZ, centerZ, scale);
                int cellSize = Math.max(2, (int) Math.ceil(PlayerIntelFeature.HEAT_CELL_SIZE * scale));
                int half = cellSize / 2;
                graphics.fill(x - half, y - half, x + cellSize - half, y + cellSize - half,
                        heatColor(cell.visits, maxVisits, cell.lastVisit, now));
            }

            drawSightings(graphics, font, world, left, top, size, centerX, centerZ, scale, labels, now);
            drawBases(graphics, font, world.bases.values(), left, top, size,
                    centerX, centerZ, scale, labels);
        }

        if (showPlayer) {
            int playerScreenX = worldToScreenX(left, size, playerX, centerX, scale);
            int playerScreenY = worldToScreenY(top, size, playerZ, centerZ, scale);
            graphics.fill(playerScreenX - 3, playerScreenY - 3,
                    playerScreenX + 4, playerScreenY + 4, 0xFF63D7FF);
            graphics.fill(playerScreenX - 1, top, playerScreenX + 1, bottom, 0x5063D7FF);
            graphics.fill(left, playerScreenY - 1, right, playerScreenY + 1, 0x5063D7FF);
            if (labels) graphics.drawString(font, "You", playerScreenX + 5, playerScreenY - 4, 0xFFBFEAFF, true);
        }

        graphics.disableScissor();
        graphics.fill(left, top, right, top + 1, 0xFF68777B);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF68777B);
        graphics.fill(left, top, left + 1, bottom, 0xFF68777B);
        graphics.fill(right - 1, top, right, bottom, 0xFF68777B);
    }

    private static void drawGrid(GuiGraphics graphics, int left, int top, int size,
                                 double centerX, double centerZ, double radius) {
        int center = size / 2;
        graphics.fill(left, top, left + size, top + size, 0xFF151E21);
        double minX = centerX - radius;
        double maxX = centerX + radius;
        double minZ = centerZ - radius;
        double maxZ = centerZ + radius;
        int firstX = (int) Math.floor(minX / GRID_BLOCKS) * GRID_BLOCKS;
        int firstZ = (int) Math.floor(minZ / GRID_BLOCKS) * GRID_BLOCKS;
        double scale = size / (radius * 2.0);

        for (int worldX = firstX; worldX <= maxX; worldX += GRID_BLOCKS) {
            int x = left + center + (int) Math.round((worldX - centerX) * scale);
            graphics.fill(x, top, x + 1, top + size, 0x303C494B);
        }
        for (int worldZ = firstZ; worldZ <= maxZ; worldZ += GRID_BLOCKS) {
            int y = top + center + (int) Math.round((worldZ - centerZ) * scale);
            graphics.fill(left, y, left + size, y + 1, 0x303C494B);
        }
    }

    private static void drawSightings(GuiGraphics graphics, Font font, PlayerIntelFeature.WorldIntel world,
                                      int left, int top, int size, double centerX, double centerZ,
                                      double scale, boolean labels, long now) {
        world.players.forEach((name, history) -> {
            if (history == null || history.latest() == null) return;
            PlayerIntelFeature.PlayerSighting sighting = history.latest();
            int x = worldToScreenX(left, size, sighting.x, centerX, scale);
            int y = worldToScreenY(top, size, sighting.z, centerZ, scale);
            int alpha = Math.max(0x40, 0xFF - (int) (Math.max(0, now - sighting.timestamp) / 3_600_000L) * 8);
            int color = (alpha << 24) | 0x00FF7777;
            graphics.fill(x - 2, y - 2, x + 3, y + 3, color);
            if (labels && alpha > 0x70)
                graphics.drawString(font, name, x + 4, y - 4, 0xFFFFD6D6, true);
        });
    }

    private static void drawBases(GuiGraphics graphics, Font font,
                                  Collection<PlayerIntelFeature.BaseMarker> bases,
                                  int left, int top, int size, double centerX, double centerZ,
                                  double scale, boolean labels) {
        for (PlayerIntelFeature.BaseMarker base : bases) {
            if (base == null) continue;
            int x = worldToScreenX(left, size, base.x, centerX, scale);
            int y = worldToScreenY(top, size, base.z, centerZ, scale);
            graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFF65E58B);
            if (labels)
                graphics.drawString(font, base.name, x + 5, y - 4, 0xFFD8FFE3, true);
        }
    }

    private static int worldToScreenX(int left, int size, double worldX, double centerX, double scale) {
        return left + size / 2 + (int) Math.round((worldX - centerX) * scale);
    }

    private static int worldToScreenY(int top, int size, double worldZ, double centerZ, double scale) {
        return top + size / 2 + (int) Math.round((worldZ - centerZ) * scale);
    }

    private static int heatColor(int visits, int maxVisits, long lastVisit, long now) {
        float intensity = maxVisits <= 1 ? 0.35f
                : (float) (Math.log1p(visits) / Math.log1p(maxVisits));
        float hue = 0.62f * (1.0f - Math.min(1.0f, intensity));
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.9f, 1.0f);
        double age = Math.max(0, now - lastVisit);
        float freshness = lastVisit <= 0 ? 0.5f : (float) Math.max(0.25, 1.0 - age / (7.0 * 86_400_000.0));
        int alpha = Math.round((55 + 160 * intensity) * freshness);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
