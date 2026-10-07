package namidevelopment.kiriyaga.nami.mixin;

import namidevelopment.kiriyaga.nami.impl.feature.visuals.NametagsFeature;
import namidevelopment.kiriyaga.nami.impl.util.SaturnicBadge;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void getNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        NametagsFeature nametagsFeature = FEATURE_SERVICE.getStorage() != null ? FEATURE_SERVICE.getStorage().getByClass(NametagsFeature.class) : null;

        if (nametagsFeature != null && nametagsFeature.isEnabled()) {
            cir.setReturnValue(null);
            return;
        }
    }

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void addSaturnicBadge(T entity, CallbackInfoReturnable<Component> cir) {
        if (entity != MC.player || cir.getReturnValue() == null) return;

        cir.setReturnValue(SaturnicBadge.appendTo(cir.getReturnValue()));
    }
}
