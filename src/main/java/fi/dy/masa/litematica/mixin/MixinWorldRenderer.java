package fi.dy.masa.litematica.mixin;

import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.util.math.MatrixStack;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(net.minecraft.client.render.WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow
    private net.minecraft.client.world.ClientWorld world;

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == net.minecraft.client.MinecraftClient.getInstance().world)
        {
            LitematicaRenderer.getInstance().loadRenderers();
        }
    }

    @Inject(method = "setupTerrain", at = @At("TAIL"))
    private void onPostSetupTerrain(
            Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(frustum);
    }

    @Inject(method = "renderLayer", at = @At("TAIL"))
    private void onRenderLayer(RenderLayer renderLayer, double x, double y, double z, Matrix4f matrixStack, Matrix4f matrix4f, CallbackInfo ci)
    {
        MatrixStack newStack = new MatrixStack();
        newStack.multiplyPositionMatrix(matrixStack);

        if (renderLayer == RenderLayer.getSolid())
        {
            LitematicaRenderer.getInstance().piecewiseRenderSolid(newStack, matrix4f);
        }
        else if (renderLayer == RenderLayer.getCutoutMipped())
        {
            LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(newStack, matrix4f);
        }
        else if (renderLayer == RenderLayer.getCutout())
        {
            LitematicaRenderer.getInstance().piecewiseRenderCutout(newStack, matrix4f);
        }
        else if (renderLayer == RenderLayer.getTranslucent())
        {
            LitematicaRenderer.getInstance().piecewiseRenderTranslucent(newStack, matrix4f);
            LitematicaRenderer.getInstance().piecewiseRenderOverlay(newStack, matrix4f);
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE_STRING", args = "ldc=blockentities",
                     target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V"),
    locals = LocalCapture.CAPTURE_FAILHARD)
    private void onPostRenderEntities(float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci)
    {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(matrix4f);

        LitematicaRenderer.getInstance().piecewiseRenderEntities(matrixStack, tickDelta);
    }

    /*
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderWorldLast(
            net.minecraft.client.util.math.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.render.Camera camera,
            net.minecraft.client.render.GameRenderer gameRenderer,
            net.minecraft.client.render.LightmapTextureManager lightmapTextureManager,
            net.minecraft.client.util.math.Matrix4f matrix4f,
            CallbackInfo ci)
    {
        boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert &&
            Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() == false)
        {
            LitematicaRenderer.getInstance().renderSchematicWorld(matrices, matrix4f, tickDelta);
        }
    }
    */
}
