package com.liquidify.untitled.module.modules.combat;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class Reach extends Module {
    public final FloatSetting range    = addSetting(new FloatSetting("Range", 5.0f, 3.0f, 8.0f));
    public final KeybindSetting keybind = addSetting(new KeybindSetting("Keybind"));

    public Reach() {
        super("Reach", Category.COMBAT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (client.player.getAttackStrengthScale(0f) < 1.0f) return;

        // Only fire if player is left clicking
        if (GLFW.glfwGetMouseButton(client.getWindow().handle(),
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT)
                != org.lwjgl.glfw.GLFW.GLFW_PRESS) return;

        // Find entity within extended reach range
        var entities = client.level.getEntitiesOfClass(
                LivingEntity.class,
                client.player.getBoundingBox().inflate(range.getValue())
        );

        for (LivingEntity target : entities) {
            if (target == client.player) continue;
            if (target instanceof Player p && p.isCreative()) continue;
            if (!target.isAlive()) continue;

            double dist = client.player.distanceTo(target);
            if (dist <= range.getValue()) {
                client.gameMode.attack(client.player, target);
                client.player.swing(InteractionHand.MAIN_HAND);
                break;
            }
        }
    }
}