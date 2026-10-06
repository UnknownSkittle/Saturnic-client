package namidevelopment.kiriyaga.nami.impl.feature.client;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.Render2DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.util.ChatAnimationHelper;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.ArrayList;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class HudRenderService {
    private final HudFeature hudFeature;

    public HudRenderService(HudFeature hudFeature) {
        this.hudFeature = hudFeature;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRender2D(Render2DEvent event) {
        ChatAnimationHelper.setChatOpen(MC.screen instanceof ChatScreen);
        ChatAnimationHelper.tick();

        boolean chatAnimationEnabled = hudFeature.isEnabled() && hudFeature.chatAnimation.get();
        if (chatAnimationEnabled) {
            int offset = (int) ChatAnimationHelper.getAnimationOffset();
            if (offset > 0) {
                event.getDrawContext().fill(
                        2,
                        MC.getWindow().getGuiScaledHeight() - offset,
                        MC.getWindow().getGuiScaledWidth() - 2,
                        MC.getWindow().getGuiScaledHeight() - 2,
                        MC.options.getBackgroundColor(Integer.MIN_VALUE)
                );
            }
        }

        int screenHeight = MC.getWindow().getGuiScaledHeight();
        int chatZoneTop = screenHeight - (screenHeight / 8);
        int chatAnimationOffset = chatAnimationEnabled
                ? (int) ChatAnimationHelper.getAnimationOffset()
                : 0;

        if (MC.level == null || MC.getDebugOverlay().showDebugScreen() || MC.options.hideGui)
            return;

        for (Feature feature : FEATURE_SERVICE.getStorage().getAll()) {
            if (feature instanceof HudElementFeature hudElement && hudElement.isEnabled()) {
                int baseY = hudElement.getRenderY();

                for (HudElementFeature.TextElement element : new ArrayList<>(hudElement.getTextElements())) {
                    int drawX = hudElement.getRenderXForElement(element);
                    int drawY = baseY + element.offsetY();

                    if (drawY + MC.font.lineHeight >= chatZoneTop) {
                        drawY -= chatAnimationOffset;
                    }

                    FONT_SERVICE.drawText(event.getDrawContext(), element.text(), drawX, drawY, true);
                }

                hudElement.renderItems(event.getDrawContext());
            }
        }
    }
}
