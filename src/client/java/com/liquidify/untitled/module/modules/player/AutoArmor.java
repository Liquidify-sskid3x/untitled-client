package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import com.liquidify.untitled.module.settings.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;

public class AutoArmor extends Module {
    public final BooleanSetting autoSwap    = addSetting(new BooleanSetting("Auto Swap", true));
    public final BooleanSetting dropUseless = addSetting(new BooleanSetting("Drop Useless", true));
    public final StringSetting prefer       = addSetting(new StringSetting("Prefer",
            "Protection", "Protection", "Thorns", "Feather Falling"));
    public final KeybindSetting keybind     = addSetting(new KeybindSetting("Keybind"));

    private long lastAction = 0;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int[] ARMOR_CONTAINER_SLOTS = {5, 6, 7, 8};

    public AutoArmor() {
        super("AutoArmor", Category.PLAYER);
    }

    // Returns 0-6 rank (higher = better). -1 = not armor for this slot.
    private boolean fitsSlot(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return false;
        var equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable == null) return false;
        return equippable.slot() == slot;
    }

    private int getArmorRank(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return -1;
        if (!fitsSlot(stack, slot)) return -1;
        String regName = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).getPath();
        if (regName.startsWith("netherite_")) return 6;
        if (regName.startsWith("diamond_"))   return 5;
        if (regName.startsWith("iron_"))      return 4;
        if (regName.startsWith("golden_"))    return 3;
        if (regName.startsWith("chainmail_")) return 2;
        if (regName.startsWith("leather_"))   return 1;
        return 1;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;
        if (System.currentTimeMillis() - lastAction < 150) return;

        Inventory inv = client.player.getInventory();

        for (int si = 0; si < ARMOR_SLOTS.length; si++) {
            EquipmentSlot slot = ARMOR_SLOTS[si];
            ItemStack equipped = client.player.getItemBySlot(slot);
            int equippedRank = equipped.isEmpty() ? -1 : getArmorRank(equipped, slot);

            // Find best armor in inventory for this slot
            int bestSlot = -1;
            int bestRank = equippedRank;

            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty()) continue;
                int rank = getArmorRank(stack, slot);
                if (rank > bestRank) {
                    bestRank = rank;
                    bestSlot = i;
                }
            }

            // Drop all inferior armor for this slot from inventory
            if (dropUseless.getValue()) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (stack.isEmpty()) continue;
                    int rank = getArmorRank(stack, slot);
                    if (rank == -1) continue; // not armor for this slot

                    // Drop if strictly worse than best available
                    if (rank < bestRank) {
                        int containerSlot = i < 9 ? i + 36 : i;
                        client.gameMode.handleInventoryMouseClick(
                                client.player.containerMenu.containerId,
                                containerSlot, 1, ClickType.THROW, client.player);
                        lastAction = System.currentTimeMillis();
                        return;
                    }
                }

                // Also drop equipped if better exists in inventory
                if (!equipped.isEmpty() && bestSlot != -1 && bestRank > equippedRank) {
                    client.gameMode.handleInventoryMouseClick(
                            client.player.containerMenu.containerId,
                            ARMOR_CONTAINER_SLOTS[si],
                            1, ClickType.THROW, client.player);
                    lastAction = System.currentTimeMillis();
                    return;
                }
            }

            // Equip best piece
            if (autoSwap.getValue() && bestSlot != -1) {
                int containerSlot = bestSlot < 9 ? bestSlot + 36 : bestSlot;
                client.gameMode.handleInventoryMouseClick(
                        client.player.containerMenu.containerId,
                        containerSlot, 0, ClickType.QUICK_MOVE, client.player);
                lastAction = System.currentTimeMillis();
                return;
            }
        }
    }
}