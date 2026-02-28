package com.liquidify.untitled.mixin;

import com.liquidify.untitled.hud.HudRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"))
    private void onGetFov(Camera camera, float partialTick, boolean useFovSetting,
                          CallbackInfoReturnable<Float> cir) {
        HudRenderer.currentFov = cir.getReturnValue();
    }
}