package namidevelopment.kiriyaga.nami.impl.util;

import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.awt.Color;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;

public final class SaturnicBadge {
    private static final Color DEFAULT_SIGNATURE_COLOR = new Color(255, 0, 106);

    private SaturnicBadge() {}

    public static MutableComponent appendTo(Component name) {
        ColorFeature colorFeature = FEATURE_SERVICE == null || FEATURE_SERVICE.getStorage() == null
                ? null
                : FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        Color signatureColor = colorFeature == null
                ? DEFAULT_SIGNATURE_COLOR
                : colorFeature.getStyledGlobalColor();

        return name.copy().append(Component.literal(" [SAT]")
                .withStyle(style -> style.withColor(signatureColor.getRGB() & 0xFFFFFF)));
    }
}
