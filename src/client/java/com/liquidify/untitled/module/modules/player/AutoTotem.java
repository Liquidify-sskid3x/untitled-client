package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;

public class AutoTotem extends Module {
    public final FloatSetting hpThreshold = addSetting(new FloatSetting("HP Threshold", 10f, 1f, 20f));
    public final BooleanSetting always     = addSetting(new BooleanSetting("Always Hold", false));
    public final KeybindSetting keybind    = addSetting(new KeybindSetting("Keybind"));

    public AutoTotem() {
        super("AutoTotem", Category.PLAYER);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;

        boolean shouldEquip = always.getValue() ||
                client.player.getHealth() <= hpThreshold.getValue();

        if (!shouldEquip) return;

        // Check if already holding totem in offhand
        if (client.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) return;

        // Find totem in inventory
        Inventory inv = client.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                // Swap to offhand slot (slot 40 in full inventory)
                client.gameMode.handleInventoryMouseClick(
                        client.player.containerMenu.containerId,
                        i < 9 ? i + 36 : i, // hotbar slots offset
                        0,
                        net.minecraft.world.inventory.ClickType.PICKUP,
                        client.player
                );
                // Pick up into cursor then place in offhand
                client.gameMode.handleInventoryMouseClick(
                        client.player.containerMenu.containerId,
                        45, // offhand slot
                        0,
                        net.minecraft.world.inventory.ClickType.PICKUP,
                        client.player
                );
                break;
            }
        }
    }
}