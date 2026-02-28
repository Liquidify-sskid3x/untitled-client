package com.liquidify.untitled.mixin;

import com.liquidify.untitled.UntitledClient;
import com.liquidify.untitled.module.modules.player.Safewalk;
import com.liquidify.untitled.module.modules.player.Velocity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LocalPlayerMixin {

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void onKnockback(double strength, double x, double z, CallbackInfo ci) {
        // Only apply to the local player
        if (!((LivingEntity)(Object)this instanceof net.minecraft.client.player.LocalPlayer)) return;

        Velocity vel = (Velocity) UntitledClient.moduleManager.getModules()
                .stream().filter(m -> m instanceof Velocity).findFirst().orElse(null);
        if (vel == null || !vel.isEnabled()) return;

        float h = vel.horizontal.getValue();
        float v = vel.vertical.getValue();
        if (h == 0.0f && v == 0.0f) {
            // Full cancel
            ci.cancel();
            return;
        }

        // Partial cancel — let it run but we'll scale it after
        Velocity.pendingCancel = true;
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        if (!((LivingEntity)(Object)this instanceof net.minecraft.client.player.LocalPlayer player))
            return;

        // Safewalk
        Safewalk sw = (Safewalk) UntitledClient.moduleManager.getModules()
                .stream().filter(m -> m instanceof Safewalk).findFirst().orElse(null);
        if (sw != null && sw.isEnabled() && player.onGround()) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null && isNearEdge(player, sw.edgeDistance.getValue(), mc.level)) {
                var v = player.getDeltaMovement();
                double nx = v.x, nz = v.z;
                if (nx > 0 && isEdge(mc.level, player.getX() + sw.edgeDistance.getValue(), player.getY(), player.getZ())) nx = 0;
                if (nx < 0 && isEdge(mc.level, player.getX() - sw.edgeDistance.getValue(), player.getY(), player.getZ())) nx = 0;
                if (nz > 0 && isEdge(mc.level, player.getX(), player.getY(), player.getZ() + sw.edgeDistance.getValue())) nz = 0;
                if (nz < 0 && isEdge(mc.level, player.getX(), player.getY(), player.getZ() - sw.edgeDistance.getValue())) nz = 0;
                player.setDeltaMovement(nx, v.y, nz);
                player.setShiftKeyDown(true);
            } else if (sw != null && sw.isEnabled()) {
                player.setShiftKeyDown(false);
            }
        }
    }

    private boolean isNearEdge(net.minecraft.client.player.LocalPlayer player,
                               float dist, net.minecraft.world.level.Level level) {
        double px = player.getX(), py = player.getY(), pz = player.getZ();
        float step = 0.15f;
        for (float ox = -dist; ox <= dist; ox += step) {
            for (float oz = -dist; oz <= dist; oz += step) {
                if (Math.abs(ox) < dist - step && Math.abs(oz) < dist - step) continue;
                BlockPos check = BlockPos.containing(px + ox, py - 0.5, pz + oz);
                if (level.getBlockState(check).isAir() &&
                        level.getBlockState(check.below()).isAir()) return true;
            }
        }
        return false;
    }

    private boolean isEdge(net.minecraft.world.level.Level level,
                           double x, double y, double z) {
        BlockPos check = BlockPos.containing(x, y - 0.5, z);
        return level.getBlockState(check).isAir() &&
                level.getBlockState(check.below()).isAir();
    }
}