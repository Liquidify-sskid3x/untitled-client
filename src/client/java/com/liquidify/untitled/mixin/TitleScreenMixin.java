package com.liquidify.untitled.mixin;

import com.liquidify.untitled.gui.AccountScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends net.minecraft.client.gui.screens.Screen {

    protected TitleScreenMixin() {
        super(Component.literal(""));
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // Top left corner, well within screen bounds
        addRenderableWidget(Button.builder(
                Component.literal("Untitled Client"),
                btn -> mc.setScreen(new AccountScreen((TitleScreen)(Object)this))
        ).pos(4, 4).size(100, 20).build());
    }
}