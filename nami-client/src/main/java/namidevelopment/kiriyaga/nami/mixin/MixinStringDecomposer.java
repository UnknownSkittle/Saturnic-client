package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.nami.impl.feature.miscellaneous.NameProtectFeature;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;

@Mixin(StringDecomposer.class)
public class MixinStringDecomposer {

    @ModifyVariable(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static String replaceText(String value) {
        if (FEATURE_SERVICE.getStorage() == null || FEATURE_SERVICE.getStorage().getByClass(NameProtectFeature.class) == null)
            return value;

        if (FEATURE_SERVICE.getStorage().getByClass(NameProtectFeature.class).isEnabled()) return value.replace(MC.getUser().getName(), "Saturnic");
        return value;
    }
}