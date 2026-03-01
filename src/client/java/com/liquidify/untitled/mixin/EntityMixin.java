package com.liquidify.untitled.mixin;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.module.modules.player.NoFall;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (!((Entity)(Object)this instanceof net.minecraft.client.player.LocalPlayer player))
            return;

        NoFall nf = (NoFall) UntitledClient.moduleManager.getModules()
                .stream().filter(m -> m instanceof NoFall).findFirst().orElse(null);
        if (nf == null || !nf.isEnabled()) return;

        player.fallDistance = 0f;
    }
}