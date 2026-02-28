package com.liquidify.untitled.module.modules.movement;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.KeybindSetting;
import com.liquidify.untitled.module.settings.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class Jesus extends Module {
    public final StringSetting mode = addSetting(new StringSetting("Mode",
            "Solid", "Solid", "Bounce", "Vanilla"));
    public final KeybindSetting keybind = addSetting(new KeybindSetting("Keybind"));

    public Jesus() {
        super("Jesus", Category.MOVEMENT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        // Only apply when actually in/on water or lava
        boolean inFluid = client.player.isInWater() || client.player.isInLava();
        boolean onFluid = isOnFluidSurface(client);

        if (!inFluid && !onFluid) return;

        switch (mode.getValue()) {
            case "Solid" -> doSolid(client);
            case "Bounce" -> doBounce(client);
            // Vanilla: do nothing, just prevent sinking
        }
    }

    private void doSolid(Minecraft client) {
        Vec3 vel = client.player.getDeltaMovement();

        // If player is sinking, push them up to the surface
        if (vel.y < 0) {
            client.player.setDeltaMovement(vel.x, 0.1, vel.z);
        }

        // If on the surface, keep them there
        if (isOnFluidSurface(client)) {
            client.player.setDeltaMovement(vel.x, 0.0, vel.z);
            client.player.setOnGround(true);

            // Send onGround=true to server
            if (client.getConnection() != null) {
                client.getConnection().send(
                        new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
                                .StatusOnly(true, false)
                );
            }
        }
    }

    private void doBounce(Minecraft client) {
        Vec3 vel = client.player.getDeltaMovement();

        if (isOnFluidSurface(client)) {
            // Small bounce to stay above surface
            client.player.setDeltaMovement(vel.x, 0.2, vel.z);
        } else if (client.player.isInWater() && vel.y < 0) {
            client.player.setDeltaMovement(vel.x, 0.05, vel.z);
        }
    }

    private boolean isOnFluidSurface(Minecraft client) {
        // Check if the block at foot level is a fluid
        BlockPos feet = BlockPos.containing(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
        );
        BlockPos below = feet.below();

        FluidState feetFluid = client.level.getFluidState(feet);
        FluidState belowFluid = client.level.getFluidState(below);

        boolean feetInFluid = feetFluid.is(FluidTags.WATER) || feetFluid.is(FluidTags.LAVA);
        boolean belowIsFluid = belowFluid.is(FluidTags.WATER) || belowFluid.is(FluidTags.LAVA);

        // On surface = feet block is fluid but block above is not
        return feetInFluid || (belowIsFluid && !client.player.isInWater());
    }
}