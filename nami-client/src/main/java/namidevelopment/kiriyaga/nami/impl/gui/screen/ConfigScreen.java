package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.api.core.config.ConfigMode;
import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import namidevelopment.kiriyaga.nami.impl.gui.component.ConsolePanelComponent;
import namidevelopment.kiriyaga.nami.impl.gui.entry.ConfigEntry;
import namidevelopment.kiriyaga.nami.impl.gui.widget.ActionItem;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
public class ConfigScreen extends NamiScreen {

    private ConsolePanelComponent<ConfigEntry> console;

    public ConfigScreen() {
        super(Component.literal("Saturnic Configs"));
    }

    @Override
    protected void init() {
        super.init();

        if (console == null) {
            console = new ConsolePanelComponent<>(
                    "Configs",
                    20, 20, 550, 300,

                    entry -> {
                        CONFIG_SERVICE.saveConfig(entry.getName(), ConfigMode.SETTINGS);
                        refresh();
                    },

                    entry -> {
                        CONFIG_SERVICE.deleteConfig(entry.getName());
                        refresh();
                    },

                    entry -> {},

                    name -> new ConfigEntry(
                            name,
                            CONFIG_SERVICE.getConfigMeta(name)
                    )
            );
        }

        refresh();
    }

    private void refresh() {
        console.setEntries(
                CONFIG_SERVICE.listConfigs().stream().map(name -> new ConfigEntry(name, CONFIG_SERVICE.getConfigMeta(name))).toList());
    }

    private ConfigEntry getEntryAt(double mouseX, double mouseY) {
        int contentY = console.getY() + console.getHeaderHeight() + 4;
        int lineHeight = FONT_SERVICE.getHeight() + 4;
        int contentHeight = console.getHeight() - console.getHeaderHeight() - console.getInputHeight() - 8;

        int maxVisible = contentHeight / lineHeight;
        double scroll = console.getScrollOffset();
        int start = (int) Math.floor(scroll);
        double partial = scroll - start;
        int drawY = contentY - (int) (partial * lineHeight);

        for (int i = start; i < Math.min(console.getEntries().size(), start + maxVisible + 1); i++) {
            ConfigEntry entry = console.getEntries().get(i);
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
        console.render(context, FONT_SERVICE.rendererProvider.getRenderer(), (int) (mouseX / CLICK_GUI_SCREEN.scale), (int) (mouseY / CLICK_GUI_SCREEN.scale));

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
            ConfigEntry entry = getEntryAt(sx, sy);
            if (entry != null) {
                console.getActionWidget().clearItems();

                console.getActionWidget().addItem(new ActionItem(
                        "load all",
                        () -> CONFIG_SERVICE.loadConfig(entry.getName(), ConfigMode.ALL)
                ));

                console.getActionWidget().addItem(new ActionItem(
                        "load settings",
                        () -> CONFIG_SERVICE.loadConfig(entry.getName(), ConfigMode.SETTINGS)
                ));

                console.getActionWidget().addItem(new ActionItem(
                        "load keybinds",
                        () -> CONFIG_SERVICE.loadConfig(entry.getName(), ConfigMode.KEYBIND)
                ));

                console.getActionWidget().addItem(new ActionItem(
                        "load color",
                        () -> CONFIG_SERVICE.loadConfig(entry.getName(), ConfigMode.COLOR)
                ));

                console.getActionWidget().addItem(new ActionItem(
                        "delete",
                        () -> {
                            CONFIG_SERVICE.deleteConfig(entry.getName());
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

    @Override public boolean mouseScrolled(double x, double y, double h, double v) {
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
