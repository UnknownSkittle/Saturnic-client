package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render2DEvent;
import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.model.setting.KeyBindSetting;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.PlayerIntelFeature;
import namidevelopment.kiriyaga.nami.impl.gui.IntelMapRenderer;
import namidevelopment.kiriyaga.nami.impl.gui.screen.HudEditorScreen;
import namidevelopment.kiriyaga.nami.impl.gui.screen.LocationMapScreen;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Rectangle;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.KEYBIND_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class IntelMinimapFeature extends HudElementFeature {
    public final IntSetting size = addSetting(new IntSetting("Size", 112, 64, 192));
    public final IntSetting range = addSetting(new IntSetting("Range", 128, 32, 1024));
    public final BoolSetting labels = addSetting(new BoolSetting("Labels", true));
    public final KeyBindSetting openMapKey = addSetting(new KeyBindSetting("OpenMap", KeyBindSetting.KEY_NONE));

    public IntelMinimapFeature() {
        super("IntelMinimap", "Shows player intel heat, sightings, and saved bases on the HUD.", 0, 0, 112, 112);
    }

    @Override
    public List<TextElement> getTextElements() {
        width = size.get();
        height = size.get();
        return List.of();
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null || !KEYBIND_SERVICE.isPressedToggle(openMapKey)) return;
        PlayerIntelFeature intel = FEATURE_SERVICE.getStorage().getByClass(PlayerIntelFeature.class);
        if (intel != null) MC.setScreen(new LocationMapScreen(intel));
    }

    @SubscribeEvent
    public void onRender(Render2DEvent event) {
        if (MC.screen instanceof HudEditorScreen) return;
        renderMinimap(event.getDrawContext());
    }

    public void renderMinimap(GuiGraphics graphics) {
        if (MC.player == null || MC.level == null || MC.options.hideGui || MC.getDebugOverlay().showDebugScreen()) return;

        int left = getRenderX();
        int top = getRenderY();
        int mapSize = size.get();
        int playerX = MC.player.blockPosition().getX();
        int playerZ = MC.player.blockPosition().getZ();
        PlayerIntelFeature intel = FEATURE_SERVICE.getStorage().getByClass(PlayerIntelFeature.class);
        PlayerIntelFeature.WorldIntel world = intel == null ? null : intel.getCurrentWorldIntel();

        graphics.fill(left - 2, top - 14, left + mapSize + 2, top + mapSize + 2, 0xB9161D20);
        graphics.drawString(MC.font, "INTEL", left + 3, top - 12, 0xFFEAF4F4, true);
        graphics.drawString(MC.font, "N", left + mapSize / 2 - 3, top + 2, 0xFFEAF4F4, true);
        IntelMapRenderer.drawMap(graphics, MC.font, world, new Rectangle(left, top, mapSize, mapSize),
                playerX, playerZ, range.get(), true, playerX, playerZ, labels.get());
    }
}
