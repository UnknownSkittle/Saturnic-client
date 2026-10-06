package namidevelopment.kiriyaga.nami;

import namidevelopment.kiriyaga.nami.contract.ClientFeatureContracts;
import namidevelopment.kiriyaga.nami.impl.feature.client.HudFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.HudRenderService;
import namidevelopment.kiriyaga.nami.impl.gui.component.NavigatePanelComponent;
import namidevelopment.kiriyaga.nami.impl.gui.screen.*;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.PlayerIntelFeature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.Tuple;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class Nami implements ClientModInitializer {
    public static String DISPLAY_NAME = "Saturnic";
    public static long START_TIME = 0;

    public static final String VERSION;
    static {
        ModContainer mod = FabricLoader.getInstance().getModContainer("saturnic-client").orElse(null);
        if (mod != null) {
            VERSION = mod.getMetadata().getVersion().getFriendlyString();
        } else {
            VERSION = "dev-environment";
        }
    }

    public static final boolean FUTURE;
    static {
        ModContainer mod = FabricLoader.getInstance().getModContainer("future").orElse(null);
        FUTURE = mod != null;
    }

    public static Tuple<ServerAddress, ServerData> LAST_CONNECTION = null;

    public static ClickGuiScreen CLICK_GUI_SCREEN;
    public static HudEditorScreen HUD_EDITOR_SCREEN;
    public static SocialsScreen SOCIALS_SCREEN;
    public static ConfigScreen CONFIG_SCREEN;
    public static PluginScreen PLUGIN_SCREEN;
    public static NavigatePanelComponent NAVIGATE_PANEL;

    @Override
    public void onInitializeClient() {
        FEATURE_SERVICE.init();
        COMMAND_SERVICE.init();

        ClientFeatureContracts.register(FEATURE_SERVICE.getStorage());
        EVENT_SERVICE.register(new HudRenderService(
                FEATURE_SERVICE.getStorage().getByClass(HudFeature.class)));

        CLICK_GUI_SCREEN = new ClickGuiScreen();
        HUD_EDITOR_SCREEN = new HudEditorScreen();
        SOCIALS_SCREEN = new SocialsScreen();
        CONFIG_SCREEN = new ConfigScreen();
        PLUGIN_SCREEN = new PluginScreen();

        NAVIGATE_PANEL = new NavigatePanelComponent();

        LOGGER.info(NAME + " " + VERSION + " has been initialized\n");

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            START_TIME = System.currentTimeMillis();

            CONFIG_SERVICE.loadPluginsState();

            CONFIG_SERVICE.loadFeatures();
            FEATURE_SERVICE.getStorage().getByClass(
                    namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature.class)
                    .ensureKeyboardKeyBind();
            CONFIG_SERVICE.loadSocials();

            if (CONFIG_SERVICE.loadName() == null)
                CONFIG_SERVICE.saveName(DISPLAY_NAME);
            else
                DISPLAY_NAME = CONFIG_SERVICE.loadName();
            COMMAND_SERVICE.getExecutor().setPrefix(CONFIG_SERVICE.loadPrefix());
            CONFIG_SERVICE.loadMacros();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            FEATURE_SERVICE.getStorage().getByClass(PlayerIntelFeature.class).saveData();
            CONFIG_SERVICE.saveFeatures();
            CONFIG_SERVICE.saveMacros();
            CONFIG_SERVICE.savePluginsState();
        });
    }
}
