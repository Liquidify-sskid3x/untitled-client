package com.liquidify.untitled.module.modules.player;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.ColorSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class AirPlace extends Module {
    public final FloatSetting range       = addSetting(new FloatSetting("Range", 4.0f, 1.0f, 6.0f));
    public final BooleanSetting showBox   = addSetting(new BooleanSetting("Show Preview", true));
    public final ColorSetting boxColor    = addSetting(new ColorSetting("Box Color", 0x5500AAFF));
    public final BooleanSetting filled    = addSetting(new BooleanSetting("Filled", true));
    public final KeybindSetting keybind   = addSetting(new KeybindSetting("Keybind"));

    private BlockPos previewPos = null;

    public AirPlace() {
        super("AirPlace", Category.PLAYER);
        // Register HUD render for the preview box
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            if (!isEnabled()) return;
            if (previewPos == null) return;
            if (!showBox.getValue()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();

            Matrix4f proj = new Matrix4f(mc.gameRenderer.getProjectionMatrix(
                    mc.options.fov().get()));
            var cam = mc.gameRenderer.getMainCamera();
            Vec3 camPos = cam.position();
            Matrix4f view = new Matrix4f();
            cam.rotation().get(view);
            view.invert();

            // Build AABB for the preview block
            double minX = previewPos.getX() - camPos.x;
            double minY = previewPos.getY() - camPos.y;
            double minZ = previewPos.getZ() - camPos.z;
            double maxX = minX + 1, maxY = minY + 1, maxZ = minZ + 1;

            float[][] corners = {
                    {(float)minX,(float)minY,(float)minZ},
                    {(float)maxX,(float)minY,(float)minZ},
                    {(float)minX,(float)maxY,(float)minZ},
                    {(float)maxX,(float)maxY,(float)minZ},
                    {(float)minX,(float)minY,(float)maxZ},
                    {(float)maxX,(float)minY,(float)maxZ},
                    {(float)minX,(float)maxY,(float)maxZ},
                    {(float)maxX,(float)maxY,(float)maxZ}
            };

            float sMinX = Float.MAX_VALUE, sMinY = Float.MAX_VALUE;
            float sMaxX = -Float.MAX_VALUE, sMaxY = -Float.MAX_VALUE;
            boolean anyVisible = false;

            for (float[] corner : corners) {
                Vector4f pos = new Vector4f(corner[0], corner[1], corner[2], 1f);
                pos.mul(view).mul(proj);
                if (pos.w <= 0) continue;
                anyVisible = true;
                float sx = (pos.x / pos.w * 0.5f + 0.5f) * screenW;
                float sy = (1f - (pos.y / pos.w * 0.5f + 0.5f)) * screenH;
                if (sx < sMinX) sMinX = sx;
                if (sx > sMaxX) sMaxX = sx;
                if (sy < sMinY) sMinY = sy;
                if (sy > sMaxY) sMaxY = sy;
            }

            if (!anyVisible) return;

            sMinX = Math.max(0, sMinX);
            sMinY = Math.max(0, sMinY);
            sMaxX = Math.min(screenW, sMaxX);
            sMaxY = Math.min(screenH, sMaxY);

            int color = boxColor.getValue() | 0xFF000000;
            int fillColor = (boxColor.getValue() & 0x00FFFFFF) | 0x55000000;

            if (filled.getValue()) {
                graphics.fill((int)sMinX, (int)sMinY, (int)sMaxX, (int)sMaxY, fillColor);
            }
            graphics.renderOutline((int)sMinX, (int)sMinY,
                    (int)(sMaxX - sMinX), (int)(sMaxY - sMinY), color);
        });
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        // Find target block position in crosshair direction
        Vec3 eyePos = client.player.getEyePosition(1.0f);
        Vec3 lookVec = client.player.getLookAngle();
        Vec3 target = eyePos.add(lookVec.scale(range.getValue()));

        previewPos = BlockPos.containing(target);

        // Don't place if block already exists there
        if (!client.level.getBlockState(previewPos).isAir()) {
            previewPos = null;
            return;
        }

        // Check if holding a block
        ItemStack held = client.player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof BlockItem)) {
            held = client.player.getOffhandItem();
            if (held.isEmpty() || !(held.getItem() instanceof BlockItem)) {
                return;
            }
        }

        // Only place when right clicking
        boolean rightClick = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                client.getWindow().handle(),
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (!rightClick) return;

        // Try to place against any neighbor
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = previewPos.relative(dir);
            if (!client.level.getBlockState(neighbor).isSolid()) continue;

            Direction face = dir.getOpposite();
            Vec3 hitVec = Vec3.atCenterOf(neighbor).relative(face, 0.5);
            BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighbor, false);

            var result = client.gameMode.useItemOn(
                    client.player, InteractionHand.MAIN_HAND, hitResult);
            if (result.consumesAction()) {
                client.player.swing(InteractionHand.MAIN_HAND);
                return;
            }
        }

        // No neighbor found — air place by faking a hit on the closest face of the target block
        Direction bestDir = Direction.DOWN;
        double bestDist = Double.MAX_VALUE;
        Vec3 center = Vec3.atCenterOf(previewPos);
        for (Direction dir : Direction.values()) {
            Vec3 faceCenter = center.relative(dir, 0.5);
            double dist = faceCenter.distanceToSqr(eyePos);
            if (dist < bestDist) {
                bestDist = dist;
                bestDir = dir;
            }
        }

        Vec3 hitVec = center.relative(bestDir, 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, bestDir, previewPos, false);
        var result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hitResult);
        if (result.consumesAction()) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void onDisable() {
        previewPos = null;
    }
}