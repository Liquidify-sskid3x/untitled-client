package com.liquidify.untitled.module.modules.movement;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class Fly extends Module {
    public final FloatSetting speed        = addSetting(new FloatSetting("Speed", 1.0f, 0.1f, 5.0f));
    public final BooleanSetting packetMode = addSetting(new BooleanSetting("Packet Mode", false));
    public final KeybindSetting keybind    = addSetting(new KeybindSetting("Keybind"));

    public Fly() {
        super("Fly", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlyingSpeed(speed.getValue() * 0.05f);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlyingSpeed(0.05f);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;
        client.player.getAbilities().flying = true;
        client.player.getAbilities().setFlyingSpeed(speed.getValue() * 0.05f);

        if (packetMode.getValue()) {
            client.getConnection().send(
                    new ServerboundMovePlayerPacket.StatusOnly(true, false)
            );
        }
    }
}