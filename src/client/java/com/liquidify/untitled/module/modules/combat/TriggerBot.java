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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TriggerBot extends Module {
    public final FloatSetting delay       = addSetting(new FloatSetting("Delay (ms)", 100f, 0f, 500f));
    public final BooleanSetting onlyMobs  = addSetting(new BooleanSetting("Only Mobs", false));
    public final BooleanSetting onlyPlayers = addSetting(new BooleanSetting("Only Players", false));
    public final KeybindSetting keybind   = addSetting(new KeybindSetting("Keybind"));

    private long lastAttack = 0;

    public TriggerBot() {
        super("TriggerBot", Category.COMBAT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (client.player.getAttackStrengthScale(0f) < 1.0f) return;

        long now = System.currentTimeMillis();
        if (now - lastAttack < delay.getValue()) return;

        // Only attack what the crosshair is looking at
        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;

        EntityHitResult entityHit = (EntityHitResult) hit;
        if (!(entityHit.getEntity() instanceof LivingEntity target)) return;
        if (target == client.player) return;
        if (!target.isAlive()) return;

        boolean isPlayer = target instanceof Player;
        if (onlyMobs.getValue() && isPlayer) return;
        if (onlyPlayers.getValue() && !isPlayer) return;
        if (isPlayer && ((Player) target).isCreative()) return;

        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
        lastAttack = now;
    }
}