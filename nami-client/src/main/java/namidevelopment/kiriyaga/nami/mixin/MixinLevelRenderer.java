package namidevelopment.kiriyaga.nami.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.NoRenderFeature;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.NoWeatherFeature;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Unique
    private Matrix4f saturnic$positionMatrix;

    @Unique
    private Matrix4f saturnic$projectionMatrix;

    @Shadow
    public abstract Gizmos.TemporaryCollection collectPerFrameGizmos();

    @Inject(method = "finalizeGizmoCollection()V", at = @At("HEAD"))
    private void onRenderTail(CallbackInfo ci) {
        Camera camera = MC.gameRenderer.getMainCamera();
        PoseStack matrices = new PoseStack();
        matrices.pushPose();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));

        try (Gizmos.TemporaryCollection ignored = collectPerFrameGizmos()) {
            EVENT_SERVICE.post(new Render3DEvent(
                    matrices,
                    MC.getDeltaTracker().getGameTimeDeltaPartialTick(true),
                    camera,
                    saturnic$positionMatrix,
                    saturnic$projectionMatrix
            ));
        }

        matrices.popPose();
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void captureMatrices(GraphicsResourceAllocator objectAllocator, DeltaTracker renderTickCounter, boolean bl, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3, GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl2, CallbackInfo ci) {
        saturnic$positionMatrix = new Matrix4f(matrix4f3);
        saturnic$projectionMatrix = new Matrix4f(matrix4f);
        RenderUtil.PROJECTION_MATRIX.set(new Matrix4f(matrix4f2));
        //RenderUtil.MODEL_VIEW_MATRIX.set(new Matrix4f(matrix4f2));
        //  RenderUtil.POSITION_MATRIX.set(new Matrix4f(matrix4f3));
        RenderUtil.CAMERA = camera;
    }

    @Inject(method = "doesMobEffectBlockSky(Lnet/minecraft/client/Camera;)Z", at = @At("HEAD"), cancellable = true)
    private void doesMobEffectBlockSky(Camera camera, CallbackInfoReturnable<Boolean> cir) {
        NoRenderFeature nr = FEATURE_SERVICE.getStorage().getByClass(NoRenderFeature.class);

        if (nr != null && nr.isEnabled() && nr.noDarkness.get())
            cir.setReturnValue(false);
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true)
    private void addWeatherPass(FrameGraphBuilder frameGraphBuilder, GpuBufferSlice gpuBufferSlice, CallbackInfo ci) {
        NoWeatherFeature nr = FEATURE_SERVICE.getStorage().getByClass(NoWeatherFeature.class);

        if (nr != null && nr.isEnabled())
            ci.cancel();
    }
}
