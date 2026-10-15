package elytraplus.feature;

import elytraplus.config.ElytraPlusConfig;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Slots beside the hotbar, shown only while an elytra is worn, on the main-arm side (opposite the offhand slot).
 * Adjacent slots share one frame like the hotbar does; the frame is cut from vanilla's offhand slot sprite.
 * <ul>
 *   <li>Elytra Slot: the worn elytra with its durability bar.</li>
 *   <li>Firework Slot: the rocket Firework Hotkey will use next, with the total rocket count
 *       (shulker boxes and bundles included); hidden when there are none.</li>
 * </ul>
 */
public final class ElytraHud {
    private static final Identifier SLOT_LEFT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final Identifier SLOT_RIGHT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_right");
    private static final int SPRITE_WIDTH = 29;
    private static final int SPRITE_HEIGHT = 24;
    /** Where the 22x22 frame starts inside each sprite (the rest is the gap to the hotbar). */
    private static final int RIGHT_FRAME_U = 7;
    private static final int LEFT_FRAME_U = 0;
    private static final int FRAME_V = 1;
    private static final int FRAME_SIZE = 22;
    /** Distance between adjacent item slots, as in the hotbar. */
    private static final int SLOT_PITCH = 20;
    /** Gap between the hotbar and the frame, as for the vanilla offhand slot. */
    private static final int HOTBAR_GAP = 7;
    private static final int HOTBAR_HALF_WIDTH = 91;
    /** Vanilla's hotbar attack indicator: 18px wide, 6px from the hotbar, on the main-arm side. */
    private static final int ATTACK_INDICATOR_SPACE = 18 + 6;
    /** Two digits fit at full size; longer counts are shrunk to this width. */
    private static final float MAX_COUNT_WIDTH = 16.0F;

    private record Slot(ItemStack stack, String countText) {
    }

    private static WeakReference<Player> cachedPlayer = new WeakReference<>(null);
    private static int cachedTick = -1;
    private static Slot cachedRocketSlot;

    private ElytraHud() {
    }

    /** Called after vanilla draws the hotbar. */
    public static void extract(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        ElytraPlusConfig config = ElytraPlusConfig.get();
        if (!(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.has(DataComponents.GLIDER)) {
            return;
        }
        // Ordered from the hotbar outwards.
        List<Slot> slots = new ArrayList<>(2);
        if (config.elytraSlot) {
            slots.add(new Slot(chest, null));
        }
        if (config.fireworkSlot) {
            Slot rockets = rocketSlot(player);
            if (rockets != null) {
                slots.add(rockets);
            }
        }
        if (slots.isEmpty()) {
            return;
        }

        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        int center = graphics.guiWidth() / 2;
        int width = frameWidth(slots.size());
        // Keep clear of vanilla's hotbar attack indicator, which sits on this same side.
        int gap = HOTBAR_GAP + (minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR ? ATTACK_INDICATOR_SPACE : 0);
        int frameX = right ? center + HOTBAR_HALF_WIDTH + gap : center - HOTBAR_HALF_WIDTH - gap - width;
        int frameY = graphics.guiHeight() - FRAME_SIZE;
        drawFrame(graphics, right ? SLOT_RIGHT_SPRITE : SLOT_LEFT_SPRITE, right ? RIGHT_FRAME_U : LEFT_FRAME_U,
                frameX, frameY, slots.size());
        for (int i = 0; i < slots.size(); i++) {
            int position = right ? i : slots.size() - 1 - i;
            drawItem(minecraft, graphics, slots.get(i), frameX + 3 + position * SLOT_PITCH, frameY + 3);
        }
    }

    /**
     * The Firework Slot contents, or null when there are no rockets. Walking shulker boxes and bundles is too much
     * to repeat every frame, and the inventory only changes between ticks, so the result is cached per tick.
     */
    private static Slot rocketSlot(Player player) {
        if (player != cachedPlayer.get() || player.tickCount != cachedTick) {
            cachedPlayer = new WeakReference<>(player);
            cachedTick = player.tickCount;
            int total = FireworkHotkey.countRockets(player);
            if (total == 0) {
                cachedRocketSlot = null;
            } else {
                ItemStack rocket = FireworkHotkey.nextRocket(player);
                if (rocket.isEmpty()) {
                    // Only rockets inside shulker boxes or bundles.
                    rocket = Items.FIREWORK_ROCKET.getDefaultInstance();
                }
                cachedRocketSlot = new Slot(rocket, total == rocket.getCount() ? null : Integer.toString(total));
            }
        }
        return cachedRocketSlot;
    }

    private static int frameWidth(int slotCount) {
        return 2 + slotCount * SLOT_PITCH;
    }

    /**
     * A single-slot frame is: left border, 20 inner columns, right border. The first and last inner columns
     * hold the rounded-corner pixels, so between joined slots they are replaced by the plain column next to them.
     */
    private static void drawFrame(GuiGraphicsExtractor graphics, Identifier sprite, int u, int x, int y, int slotCount) {
        int plainStart = u + 2;
        int plainWidth = SLOT_PITCH - 2;
        blitColumns(graphics, sprite, u, x, y, 1);
        int slotX = x + 1;
        for (int i = 0; i < slotCount; i++) {
            blitColumns(graphics, sprite, i == 0 ? u + 1 : plainStart, slotX, y, 1);
            blitColumns(graphics, sprite, plainStart, slotX + 1, y, plainWidth);
            blitColumns(graphics, sprite, i == slotCount - 1 ? u + SLOT_PITCH : plainStart + plainWidth - 1, slotX + SLOT_PITCH - 1, y, 1);
            slotX += SLOT_PITCH;
        }
        blitColumns(graphics, sprite, u + FRAME_SIZE - 1, slotX, y, 1);
    }

    private static void blitColumns(GuiGraphicsExtractor graphics, Identifier sprite, int u, int x, int y, int width) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, SPRITE_WIDTH, SPRITE_HEIGHT, u, FRAME_V, x, y, width, FRAME_SIZE);
    }

    private static void drawItem(Minecraft minecraft, GuiGraphicsExtractor graphics, Slot slot, int x, int y) {
        graphics.item(slot.stack(), x, y);
        if (slot.countText() == null) {
            graphics.itemDecorations(minecraft.font, slot.stack(), x, y);
        } else {
            // Empty text suppresses vanilla's count; ours is shrunk so 3-4 digits stay inside the slot.
            graphics.itemDecorations(minecraft.font, slot.stack(), x, y, "");
            drawCount(minecraft, graphics, slot.countText(), x, y);
        }
    }

    /** Same anchor as vanilla's stack count (bottom-right of the item), scaled down to fit the 16px item area. */
    private static void drawCount(Minecraft minecraft, GuiGraphicsExtractor graphics, String text, int itemX, int itemY) {
        int width = minecraft.font.width(text);
        float scale = Math.min(1.0F, MAX_COUNT_WIDTH / width);
        graphics.pose().pushMatrix();
        graphics.pose().translate(itemX + 17, itemY + 17);
        graphics.pose().scale(scale, scale);
        graphics.text(minecraft.font, text, -width, -8, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
    }
}
