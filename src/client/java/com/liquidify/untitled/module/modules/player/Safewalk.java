package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Safewalk extends Module {
    public final KeybindSetting keybind = addSetting(new KeybindSetting("Keybind"));

    public Safewalk() {
        super("Safewalk", Category.PLAYER);
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        Vec3 vel = mc.player.getDeltaMovement();
        Vec3 pos = mc.player.position();
        Vec3 next = new Vec3(pos.x + vel.x, pos.y, pos.z + vel.z);

        BlockPos under = BlockPos.containing(next.x, next.y - 1, next.z);
        BlockState state = mc.level.getBlockState(under);

        boolean air = state.isAir();
        mc.player.setShiftKeyDown(air);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.setShiftKeyDown(false);
    }
}