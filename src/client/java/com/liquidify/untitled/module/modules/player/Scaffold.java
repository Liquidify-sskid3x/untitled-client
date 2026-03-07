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
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
    public final BooleanSetting tower       = addSetting(new BooleanSetting("Fast Tower", false));
    public final FloatSetting towerSpeed    = addSetting(new FloatSetting("Tower Speed", 0.5f, 0.0f, 1.0f));
    public final BooleanSetting onlyOnClick = addSetting(new BooleanSetting("Only on Click", false));
    public final BooleanSetting swing       = addSetting(new BooleanSetting("Swing", true));
    public final KeybindSetting keybind     = addSetting(new KeybindSetting("Keybind"));

    public Scaffold() {
        super("Scaffold", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.setShiftKeyDown(false);
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        long window = mc.getWindow().handle();

        if (onlyOnClick.getValue()) {
            boolean rightClick = org.lwjgl.glfw.GLFW.glfwGetMouseButton(window,
                    org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (!rightClick) return;
        }

        // Block directly below player's feet
// Block directly below player's feet
        BlockPos below = BlockPos.containing(
                mc.player.getX(),
                mc.player.getY() - 1.0,
                mc.player.getZ());

// Only place if that block is air
        if (!mc.level.getBlockState(below).isAir()) {
            handleTower(mc, window);
            return;
        }

        int blockSlot = findBlockSlot(mc);
        if (blockSlot == -1) return;

        int prevSlot = getSelected(mc);
        switchSlot(mc, blockSlot);

        place(mc, below);

        handleTower(mc, window);

        switchSlot(mc, prevSlot);
    }

    private void handleTower(Minecraft mc, long window) {
        if (!tower.getValue()) return;
        boolean jumping = org.lwjgl.glfw.GLFW.glfwGetKey(window,
                org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        boolean sneaking = org.lwjgl.glfw.GLFW.glfwGetKey(window,
                org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (!jumping || sneaking) return;

        Vec3 vel = mc.player.getDeltaMovement();

        net.minecraft.world.phys.AABB playerBox = mc.player.getBoundingBox().move(0, 1, 0);
        boolean blockAbove = false;
        for (var ignored : mc.level.getBlockCollisions(mc.player, playerBox)) {
            blockAbove = true;
            break;
        }

        if (!blockAbove) {
            mc.player.setDeltaMovement(vel.x, towerSpeed.getValue(), vel.z);
        } else {
            double snapY = Math.ceil(mc.player.getY()) - mc.player.getY();
            mc.player.setDeltaMovement(vel.x, snapY, vel.z);
            mc.player.setOnGround(true);
        }
    }

    private boolean place(Minecraft mc, BlockPos bp) {
        if (!mc.level.getBlockState(bp).isAir()) return false;

        Direction side = getPlaceSide(mc, bp);
        if (side == null) return false;

        BlockPos neighbor = bp.relative(side);
        Direction face = side.getOpposite();
        Vec3 hitVec = Vec3.atCenterOf(neighbor).relative(face, 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighbor, false);

        InteractionResult result = mc.gameMode.useItemOn(
                mc.player, InteractionHand.MAIN_HAND, hitResult);

        if (result.consumesAction()) {
            if (swing.getValue()) mc.player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private Direction getPlaceSide(Minecraft mc, BlockPos bp) {
        if (!mc.level.getBlockState(bp).isAir()) return null;
        Direction[] priority = {
                Direction.DOWN,
                Direction.NORTH, Direction.SOUTH,
                Direction.EAST,  Direction.WEST,
                Direction.UP
        };
        for (Direction dir : priority) {
            if (mc.level.getBlockState(bp.relative(dir)).isSolid()) return dir;
        }
        return null;
    }

    private boolean validItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        return !(bi.getBlock() instanceof FallingBlock);
    }

    private int findBlockSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (validItem(mc.player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private int getSelected(Minecraft mc) {
        try {
            var f = mc.player.getInventory().getClass().getDeclaredField("selected");
            f.setAccessible(true);
            return (int) f.get(mc.player.getInventory());
        } catch (Exception e) { return 0; }
    }

    private void switchSlot(Minecraft mc, int slot) {
        try {
            var f = mc.player.getInventory().getClass().getDeclaredField("selected");
            f.setAccessible(true);
            f.set(mc.player.getInventory(), slot);
        } catch (Exception ignored) {}
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
    }
}