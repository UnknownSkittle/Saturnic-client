package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import namidevelopment.kiriyaga.nami.impl.gui.component.ConsolePanelComponent;
import namidevelopment.kiriyaga.nami.impl.gui.entry.PluginEntry;
import namidevelopment.kiriyaga.nami.impl.gui.widget.ActionItem;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.stream.StreamSupport;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.nami.Nami.*;

public class PluginScreen extends NamiScreen {

    private ConsolePanelComponent<PluginEntry> console;

    public PluginScreen() {
        super(Component.literal("Saturnic Plugins"));
    }

    @Override
    protected void init() {
        super.init();

        if (console == null) {
            console = new ConsolePanelComponent<>(
                    "Plugins",
                    20, 20, 600, 300,

                    entry -> {
                        if (entry == null) return;

                        if (entry.getPlugin().isEnabled()) {
                            PLUGIN_SERVICE.disablePlugin(entry.getId());
                        } else {
                            PLUGIN_SERVICE.enablePlugin(entry.getId());
                        }

                        CONFIG_SERVICE.savePluginsState();
                        refresh();
                    },


                    entry -> {
                        if (entry == null) return;

                        PLUGIN_SERVICE.disablePlugin(entry.getId());
                        PLUGIN_SERVICE.unregisterPlugin(entry.getId());

                        CONFIG_SERVICE.savePluginsState();
                        refresh();
                    },


                    entry -> {},

                    name -> null
            );
        }

        refresh();
    }

    private void refresh() {
        console.setEntries(StreamSupport.stream(PLUGIN_SERVICE.getAllPlugins().spliterator(), false).map(PluginEntry::new).toList());
    } // crazy StreamSupport use

    private PluginEntry getEntryAt(double mouseX, double mouseY) {
        int contentY = console.getY() + console.getHeaderHeight() + 4;
        int lineHeight = FONT_SERVICE.getHeight() + 4;
        int contentHeight = console.getHeight() - console.getHeaderHeight() - console.getInputHeight() - 8;

        int maxVisible = contentHeight / lineHeight;
        double scroll = console.getScrollOffset();
        int start = (int) Math.floor(scroll);
        double partial = scroll - start;
        int drawY = contentY - (int) (partial * lineHeight);

        for (int i = start; i < Math.min(console.getEntries().size(), start + maxVisible + 1); i++) {
            PluginEntry entry = console.getEntries().get(i);
            if (mouseY >= drawY && mouseY <= drawY + lineHeight)
                return entry;
            drawY += lineHeight;
        }
        return null;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class) != null && FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).background.get()) {
            renderMenuBackground(context);
        }

        NAVIGATE_PANEL.render(context, FONT_SERVICE.rendererProvider.getRenderer(), mouseX, mouseY);

        context.pose().pushMatrix();
        context.pose().scale(CLICK_GUI_SCREEN.scale, CLICK_GUI_SCREEN.scale);

        console.render(
                context,
                FONT_SERVICE.rendererProvider.getRenderer(),
                (int) (mouseX / CLICK_GUI_SCREEN.scale),
                (int) (mouseY / CLICK_GUI_SCREEN.scale)
        );

        context.pose().popMatrix();
    }

    @Override
    public void renderBackground(GuiGraphics context, int i, int j, float f) {
        if (MC.level != null && FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).blur.get())
            this.renderBlurredBackground(context);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        NAVIGATE_PANEL.mouseClicked(click.x(), click.y(), FONT_SERVICE.rendererProvider.getRenderer());

        double sx = click.x() / CLICK_GUI_SCREEN.scale;
        double sy = click.y() / CLICK_GUI_SCREEN.scale;

        if (click.button() == 1) {
            PluginEntry entry = getEntryAt(sx, sy);

            if (entry != null) {
                console.getActionWidget().clearItems();

                if (entry.isEnabled()) {
                    console.getActionWidget().addItem(new ActionItem(
                            "disable",
                            () -> {
                                PLUGIN_SERVICE.disablePlugin(entry.getId());
                                CONFIG_SERVICE.savePluginsState();
                                refresh();
                            }
                    ));
                } else {
                    console.getActionWidget().addItem(new ActionItem(
                            "enable",
                            () -> {
                                PLUGIN_SERVICE.enablePlugin(entry.getId());
                                CONFIG_SERVICE.savePluginsState();
                                refresh();
                            }
                    ));
                }

                console.getActionWidget().addItem(new ActionItem(
                        "unregister",
                        () -> {
                            PLUGIN_SERVICE.disablePlugin(entry.getId());
                            PLUGIN_SERVICE.unregisterPlugin(entry.getId());
                            CONFIG_SERVICE.savePluginsState();
                            refresh();
                        }
                ));

                console.getActionWidget().setPosition((int) sx, (int) sy);
                console.getActionWidget().setVisible(true);

                return true;
            }
        }

        if (console.getActionWidget().isVisible() && console.getActionWidget().mouseClicked(sx, sy, click.button()))
            return true;

        return console.mouseClicked(sx, sy, click.button()) || super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        return console.mouseScrolled(x / CLICK_GUI_SCREEN.scale, y / CLICK_GUI_SCREEN.scale, v)
                || super.mouseScrolled(x, y, h, v);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double d, double e) {
        return console.mouseDragged(click.x() / CLICK_GUI_SCREEN.scale, click.y() / CLICK_GUI_SCREEN.scale, d, e)
                || super.mouseDragged(click, d, e);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        console.mouseReleased(click.x() / CLICK_GUI_SCREEN.scale, click.y() / CLICK_GUI_SCREEN.scale, click.button());
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        int keycode = keyInput.input();
        int scancode = keyInput.scancode();
        int modifiers = keyInput.modifiers();

        return console.keyPressed(keycode, scancode, modifiers) || super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharacterEvent charInput) {
        String character = charInput.codepointAsString();
        int modifiers = charInput.modifiers();

        return console.charTyped(character.charAt(0), modifiers) || super.charTyped(charInput);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
