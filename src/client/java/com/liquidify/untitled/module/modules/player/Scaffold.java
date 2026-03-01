package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
    public final BooleanSetting tower        = addSetting(new BooleanSetting("Fast Tower", false));
    public final FloatSetting towerSpeed     = addSetting(new FloatSetting("Tower Speed", 0.42f, 0.1f, 1.0f));
    public final BooleanSetting safe         = addSetting(new BooleanSetting("Safe", true));
    public final BooleanSetting onlyOnClick  = addSetting(new BooleanSetting("Only on Click", false));
    public final BooleanSetting swing        = addSetting(new BooleanSetting("Swing", true));
    public final FloatSetting pathLength     = addSetting(new FloatSetting("Path Length", 3.0f, 1.0f, 10.0f));
    public final KeybindSetting keybind      = addSetting(new KeybindSetting("Keybind"));

    public Scaffold() {
        super("Scaffold", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.setShiftKeyDown(false);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (onlyOnClick.getValue()) {
            boolean rightClick = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                    client.getWindow().handle(),
                    org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (!rightClick) {
                client.player.setShiftKeyDown(false);
                return;
            }
        }

        if (safe.getValue()) client.player.setShiftKeyDown(true);

        int blockSlot = findBlockSlot(client);
        if (blockSlot == -1) return;

        int prevSlot = getSelected(client);
        switchSlot(client, blockSlot);

        // Get player look direction projected onto horizontal plane
        float yaw = (float) Math.toRadians(client.player.getYRot());
        double lookX = -Math.sin(yaw);
        double lookZ =  Math.cos(yaw);

        int len = Math.round(pathLength.getValue());

        // Place blocks along path in look direction — no break so all gaps filled each tick
        for (int i = 0; i < len; i++) {
            double offsetX = lookX * i;
            double offsetZ = lookZ * i;

            BlockPos target = BlockPos.containing(
                    client.player.getX() + offsetX,
                    client.player.getY() - 1.0,
                    client.player.getZ() + offsetZ
            );

            if (client.level.getBlockState(target).isAir()) {
                tryPlace(client, target);
                // No break — fill every gap this tick
            }
        }

        // Tower mode
        if (tower.getValue()) {
            boolean jumpPressed = org.lwjgl.glfw.GLFW.glfwGetKey(
                    client.getWindow().handle(),
                    org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (jumpPressed && client.player.onGround()) {
                BlockPos above = BlockPos.containing(
                        client.player.getX(),
                        client.player.getY() + client.player.getBbHeight(),
                        client.player.getZ());
                if (client.level.getBlockState(above).isAir()) {
                    Vec3 v = client.player.getDeltaMovement();
                    client.player.setDeltaMovement(v.x, towerSpeed.getValue(), v.z);
                }
            }
        }

        switchSlot(client, prevSlot);
    }

    private boolean tryPlace(Minecraft client, BlockPos bp) {
        Direction[] priority = {
                Direction.DOWN,
                Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST,
                Direction.UP
        };

        for (Direction dir : priority) {
            BlockPos neighbor = bp.relative(dir);
            if (!client.level.getBlockState(neighbor).isSolid()) continue;

            Direction face = dir.getOpposite();
            Vec3 hitVec = Vec3.atCenterOf(neighbor).relative(face, 0.5);
            BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighbor, false);

            InteractionResult result = client.gameMode.useItemOn(
                    client.player, InteractionHand.MAIN_HAND, hitResult);

            if (result.consumesAction()) {
                if (swing.getValue()) client.player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    private int findBlockSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    private int getSelected(Minecraft client) {
        try {
            var f = client.player.getInventory().getClass().getDeclaredField("selected");
            f.setAccessible(true);
            return (int) f.get(client.player.getInventory());
        } catch (Exception e) {
            return 0;
        }
    }

    private void switchSlot(Minecraft client, int slot) {
        try {
            var f = client.player.getInventory().getClass().getDeclaredField("selected");
            f.setAccessible(true);
            f.set(client.player.getInventory(), slot);
        } catch (Exception ignored) {}
        client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
    }
}