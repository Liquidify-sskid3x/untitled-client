package com.liquidify.untitled.module.modules.combat;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class KillAura extends Module {
    public final FloatSetting range           = addSetting(new FloatSetting("Range", 4.0f, 1.0f, 6.0f));
    public final BooleanSetting hitIfInRange  = addSetting(new BooleanSetting("Hit if in range", true));
    public final BooleanSetting hitIfInCrosshair = addSetting(new BooleanSetting("Hit if in crosshair", false));
    public final KeybindSetting keybind       = addSetting(new KeybindSetting("Keybind"));

    public KillAura() {
        super("KillAura", Category.COMBAT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (client.player.getAttackStrengthScale(0f) < 1.0f) return;

        List<LivingEntity> targets = client.level.getEntitiesOfClass(
                LivingEntity.class,
                client.player.getBoundingBox().inflate(range.getValue())
        );

        for (LivingEntity target : targets) {
            if (target == client.player) continue;
            if (target instanceof Player p && p.isCreative()) continue;
            if (!target.isAlive()) continue;
            if (hitIfInCrosshair.getValue() && client.crosshairPickEntity != target) continue;
            if (!hitIfInRange.getValue() && !hitIfInCrosshair.getValue()) continue;

            client.gameMode.attack(client.player, target);
            client.player.swing(InteractionHand.MAIN_HAND);
            break;
        }
    }
}