package com.liquidify.untitled.module.modules.render;

import com.liquidify.untitled.module.Category;
import com.liquidify.untitled.module.Module;
import com.liquidify.untitled.module.settings.BooleanSetting;
import com.liquidify.untitled.module.settings.FloatSetting;
import com.liquidify.untitled.module.settings.KeybindSetting;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Nametags extends Module {
    public final BooleanSetting showArmor      = addSetting(new BooleanSetting("Armor", true));
    public final BooleanSetting showDurability = addSetting(new BooleanSetting("Durability", true));
    public final BooleanSetting showHealth     = addSetting(new BooleanSetting("Health", true));
    public final BooleanSetting showEnchants   = addSetting(new BooleanSetting("Enchants", true));
    public final BooleanSetting showHands      = addSetting(new BooleanSetting("Hands", true));
    public final BooleanSetting showSelf       = addSetting(new BooleanSetting("Show Self", false));
    public final BooleanSetting playersOnly    = addSetting(new BooleanSetting("Players Only", false));
    public final FloatSetting range            = addSetting(new FloatSetting("Range", 32f, 4f, 64f));
    public final KeybindSetting keybind        = addSetting(new KeybindSetting("Keybind"));

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private static final int BG_FILL    = 0xAA050510;
    private static final int BG_OUTLINE = 0xBB0a1a3a;
    private static final int BG_ACCENT  = 0xFF0a84ff;
    private static final int BG_INNER   = 0x88081525;

    public Nametags() {
        super("Nametags", Category.RENDER);
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onHudRender(GuiGraphics g, DeltaTracker delta) {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        float aspect = (float) screenW / screenH;
        float fov = mc.options.fov().get().floatValue();

        Matrix4f proj = new Matrix4f().perspective(
                (float) Math.toRadians(fov), aspect, 0.05f, 1000f);

        var cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.position();
        Matrix4f view = new Matrix4f();
        cam.rotation().get(view);
        view.invert();

        for (LivingEntity entity : mc.level.getEntitiesOfClass(
                LivingEntity.class,
                mc.player.getBoundingBox().inflate(range.getValue()))) {

            if (entity == mc.player && !showSelf.getValue()) continue;
            if (playersOnly.getValue() && !(entity instanceof Player)) continue;
            if (!entity.isAlive()) continue;

            double wx = entity.getX() - camPos.x;
            double wy = (entity.getY() + entity.getBbHeight() + 0.35) - camPos.y;
            double wz = entity.getZ() - camPos.z;

            Vector4f pos = new Vector4f((float) wx, (float) wy, (float) wz, 1f);
            pos.mul(view).mul(proj);
            if (pos.w <= 0) continue;

            float sx = (pos.x / pos.w * 0.5f + 0.5f) * screenW;
            float sy = (1f - (pos.y / pos.w * 0.5f + 0.5f)) * screenH;
            if (sx < -300 || sx > screenW + 300 || sy < -300 || sy > screenH + 300) continue;

            double dist = entity.distanceTo(mc.player);
            float scale = (float) Math.max(0.3, Math.min(1.0, 10.0 / dist));

            drawNametag(g, mc, entity, (int) sx, (int) sy, scale);
        }
    }

    private void drawNametag(GuiGraphics g, Minecraft mc,
                             LivingEntity entity, int cx, int cy, float scale) {
        int iconSize  = (int)(14 * scale);
        int padding   = (int)(2  * scale);
        int lineH     = (int)(mc.font.lineHeight * scale) + 1;
        int durBarH   = Math.max(2, (int)(3 * scale));
        int durOffset = Math.max(1, (int)(2 * scale)); // how many px inside bottom of icon

        // ── Pre-calculate total height for background box ──────────
        int totalH = 0;
        boolean hasArmor = false;
        for (EquipmentSlot slot : ARMOR_SLOTS)
            if (!entity.getItemBySlot(slot).isEmpty()) { hasArmor = true; break; }

        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offHand  = entity.getItemBySlot(EquipmentSlot.OFFHAND);
        boolean hasHands   = showHands.getValue()
                && (!mainHand.isEmpty() || !offHand.isEmpty());

        if (showArmor.getValue() && hasArmor)
            totalH += iconSize + padding;
        if (hasHands)
            totalH += iconSize + padding;
        totalH += lineH; // name
        if (showHealth.getValue())
            totalH += lineH + durBarH + 2;

        int slots    = ARMOR_SLOTS.length;
        int totalW = Math.max(60, Math.min(160, slots * (iconSize + padding)));
        int startX   = cx - totalW / 2;
        int startY   = cy;

        // ── Outer background box ───────────────────────────────────
        // Shadow layer
        g.fill(startX - 1, startY - 1,
                startX + totalW + 1, startY + totalH + 3, 0x55000010);
        // Main background
        g.fill(startX, startY, startX + totalW, startY + totalH, BG_FILL);
        // Outer dark blue outline (depth)
        g.renderOutline(startX - 1, startY - 1,
                totalW + 2, totalH + 2, BG_OUTLINE);
        // Inner accent outline
        g.renderOutline(startX, startY, totalW, totalH, 0xFF061228);
        // Top accent bar
        g.fill(startX, startY, startX + totalW, startY + 1, BG_ACCENT);

        int currentY = startY + 2;

        // ── Armor row ─────────────────────────────────────────────
        if (showArmor.getValue() && hasArmor) {
            int armorTotalW = slots * iconSize + (slots - 1) * padding;
            int armorStartX = cx - armorTotalW / 2;

            // Section bg
            g.fill(armorStartX - 1, currentY - 1,
                    armorStartX + armorTotalW + 1,
                    currentY + iconSize + 1, BG_INNER);
            g.renderOutline(armorStartX - 1, currentY - 1,
                    armorTotalW + 2, iconSize + 2, 0xFF061830);

            int ix = armorStartX;
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    g.renderFakeItem(stack, ix, currentY);

                    // Enchant glow + labels
                    if (showEnchants.getValue()) {
                        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
                        if (enchants != null && !enchants.isEmpty()) {
                            g.fill(ix, currentY,
                                    ix + iconSize, currentY + iconSize, 0x330088FF);
                            int ey = currentY - enchants.size() * mc.font.lineHeight;
                            for (var entry : enchants.entrySet()) {
                                String key = entry.getKey().unwrapKey()
                                        .map(k -> k.registry().getPath())
                                        .orElse("unk");
                                String abbr = key.length() >= 3
                                        ? key.substring(0, 3).toUpperCase()
                                        : key.toUpperCase();
                                String enchStr = abbr + entry.getIntValue();
                                int ew = mc.font.width(enchStr);
                                boolean isCurse = key.startsWith("curse");
                                int enchColor = isCurse ? 0xFFFF5555 : 0xFF55FFFF;
                                g.fill(ix + iconSize / 2 - ew / 2 - 1, ey - 1,
                                        ix + iconSize / 2 + ew / 2 + 1,
                                        ey + mc.font.lineHeight - 1, 0xAA000000);
                                g.drawString(mc.font, enchStr,
                                        ix + iconSize / 2 - ew / 2,
                                        ey, enchColor, true);
                                ey += mc.font.lineHeight;
                            }
                        }
                    }

                    // Durability bar — clipped inside bottom of icon
                    if (showDurability.getValue() && stack.isDamageableItem()) {
                        int maxDur = stack.getMaxDamage();
                        int curDur = maxDur - stack.getDamageValue();
                        float pct  = (float) curDur / maxDur;
                        int durColor = pct > 0.6f ? 0xFF55FF55
                                : pct > 0.3f ? 0xFFFFAA00 : 0xFFFF5555;
                        int barY = currentY + iconSize - durOffset - durBarH;
                        // Track bg
                        g.fill(ix, barY, ix + iconSize, barY + durBarH, 0x88000000);
                        // Fill
                        g.fill(ix, barY, ix + (int)(iconSize * pct),
                                barY + durBarH, durColor);
                    }
                }
                ix += iconSize + padding;
            }
            currentY += iconSize + padding;
        }

        // ── Hands row ─────────────────────────────────────────────
        if (hasHands) {
            int handsTotalW = 2 * iconSize + padding;
            int handsStartX = cx - handsTotalW / 2;

            g.fill(handsStartX - 1, currentY - 1,
                    handsStartX + handsTotalW + 1,
                    currentY + iconSize + 1, BG_INNER);
            g.renderOutline(handsStartX - 1, currentY - 1,
                    handsTotalW + 2, iconSize + 2, 0xFF061830);

            // Main hand
            if (!mainHand.isEmpty()) {
                g.renderFakeItem(mainHand, handsStartX, currentY);
                if (showDurability.getValue() && mainHand.isDamageableItem()) {
                    int maxDur = mainHand.getMaxDamage();
                    int curDur = maxDur - mainHand.getDamageValue();
                    float pct  = (float) curDur / maxDur;
                    int durColor = pct > 0.6f ? 0xFF55FF55
                            : pct > 0.3f ? 0xFFFFAA00 : 0xFFFF5555;
                    int barY = currentY + iconSize - durOffset - durBarH;
                    g.fill(handsStartX, barY,
                            handsStartX + iconSize, barY + durBarH, 0x88000000);
                    g.fill(handsStartX, barY,
                            handsStartX + (int)(iconSize * pct),
                            barY + durBarH, durColor);
                }
            }

            // Off hand
            if (!offHand.isEmpty()) {
                int offX = handsStartX + iconSize + padding;
                g.renderFakeItem(offHand, offX, currentY);
                if (showDurability.getValue() && offHand.isDamageableItem()) {
                    int maxDur = offHand.getMaxDamage();
                    int curDur = maxDur - offHand.getDamageValue();
                    float pct  = (float) curDur / maxDur;
                    int durColor = pct > 0.6f ? 0xFF55FF55
                            : pct > 0.3f ? 0xFFFFAA00 : 0xFFFF5555;
                    int barY = currentY + iconSize - durOffset - durBarH;
                    g.fill(offX, barY, offX + iconSize, barY + durBarH, 0x88000000);
                    g.fill(offX, barY,
                            offX + (int)(iconSize * pct),
                            barY + durBarH, durColor);
                }
            }

            currentY += iconSize + padding;
        }

        // ── Name ──────────────────────────────────────────────────
        String name = entity.getName().getString();
        int nameW     = mc.font.width(name);
        int nameColor = entity instanceof Player ? 0xFF5BB8FF : 0xFFCCDDFF;

        // Name section bg
        g.fill(startX, currentY - 1,
                startX + totalW, currentY + mc.font.lineHeight + 1, BG_INNER);
        g.fill(startX, currentY - 1, startX + 2,
                currentY + mc.font.lineHeight + 1, BG_ACCENT); // left accent stripe
        g.drawString(mc.font, name, cx - nameW / 2, currentY, nameColor, true);
        currentY += lineH + 1;

        // ── Health ────────────────────────────────────────────────
        if (showHealth.getValue()) {
            float hp    = entity.getHealth();
            float maxHp = entity.getMaxHealth();
            float pct   = hp / maxHp;
            int hpColor = pct > 0.6f ? 0xFF55FF55
                    : pct > 0.3f ? 0xFFFFAA00 : 0xFFFF5555;

            int hearts    = (int) Math.ceil(hp / 2f);
            int maxHearts = (int) Math.ceil(maxHp / 2f);
            String hpText = "❤ " + hearts + "/" + maxHearts;
            int hpW = mc.font.width(hpText);

            g.fill(startX, currentY - 1, startX + totalW, currentY + mc.font.lineHeight + 1, BG_INNER);
            g.drawString(mc.font, hpText, cx - hpW / 2, currentY, hpColor, true);
            currentY += mc.font.lineHeight + 1;

            // Health bar
            int barW = totalW - 4;
            int barX = startX + 2;
            // Track
            g.fill(barX, currentY, barX + barW, currentY + durBarH, 0xFF0a1a2a);
            // Fill
            g.fill(barX, currentY, barX + (int)(barW * pct),
                    currentY + durBarH, hpColor | 0xFF000000);
            // Bar outline
            g.renderOutline(barX, currentY, barW, durBarH, 0xFF061020);
        }
    }
}