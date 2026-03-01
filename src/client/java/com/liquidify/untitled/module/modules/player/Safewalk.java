package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class Safewalk extends Module {
    public final FloatSetting edgeDistance = addSetting(new FloatSetting("Edge Distance", 0.5f, 0.1f, 1.5f));
    public final KeybindSetting keybind    = addSetting(new KeybindSetting("Keybind"));

    public Safewalk() {
        super("Safewalk", Category.PLAYER);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (!client.player.onGround()) {
            client.player.setShiftKeyDown(false);
            return;
        }
        client.player.setShiftKeyDown(isNearEdge(client));
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.setShiftKeyDown(false);
    }

    private boolean isNearEdge(Minecraft client) {
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();
        float d = edgeDistance.getValue();

        // Check a grid of points within edgeDistance radius
        float step = 0.1f;
        for (float ox = -d; ox <= d; ox += step) {
            for (float oz = -d; oz <= d; oz += step) {
                // Only check the edge ring, not the center
                if (Math.abs(ox) < d - step && Math.abs(oz) < d - step) continue;

                BlockPos check = BlockPos.containing(px + ox, py - 0.5, pz + oz);
                if (client.level.getBlockState(check).isAir() &&
                        client.level.getBlockState(check.below()).isAir()) {
                    return true;
                }
            }
        }
        return false;
    }
}