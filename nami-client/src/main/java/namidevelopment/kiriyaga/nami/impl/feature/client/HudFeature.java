package namidevelopment.kiriyaga.nami.impl.feature.client;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;

@RegisterFeature
public class HudFeature extends Feature {

    public final BoolSetting chatAnimation = addSetting(new BoolSetting("ChatAnimation", true));

    public HudFeature() {
        super("HUD", "Controls in-game HUD chat animation.", FeatureCategory.of("Client"));
    }
}