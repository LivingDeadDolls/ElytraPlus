package elytraplus.feature;

import elytraplus.config.ElytraPlusConfig;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
 * Slots shown only while an elytra is worn. By default they sit beside the hotbar on the main-arm side (opposite the
 * offhand slot); the settings can move them to a corner of the screen, to either side, stack them vertically and
 * offset them. The first slot is always the one nearest the anchor (the hotbar, or the screen edge).
 * <p>
 * Adjacent slots share one frame, in one of two styles:
 * <ul>
 *   <li>Hotbar: built from vanilla's hotbar sprite: its left end and first slots, then its last slot and right end.
 *       A vertical frame is a column of one-slot frames sharing their top and bottom ends: the hotbar's top edge, its
 *       slot rows once per slot, then its bottom edge. Cuts fall only on the hotbar's own slot boundaries.</li>
 *   <li>Inventory: vanilla's inventory slots ({@code container/slot}) in a panel whose 4px border and corners are cut
 *       from the straight edges and corners of the inventory screen's background.</li>
 * </ul>
 * Either way, resource packs that restyle those textures (including higher-resolution ones) restyle this frame the
 * same way. Items and their decorations are drawn the way the hotbar draws them.
 * <ul>
 *   <li>Elytra Slot: the worn elytra with its durability bar.</li>
 *   <li>Firework Slot: the rocket Firework Hotkey will use next, with the total rocket count
 *       (shulker boxes and bundles included); hidden when there are none.</li>
 * </ul>
 */
public final class ElytraHud {
    private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int HOTBAR_HALF_WIDTH = HOTBAR_WIDTH / 2;
    /** Distance between adjacent item slots, as in the hotbar. */
    private static final int SLOT_PITCH = 20;
    /** The hotbar's last slot plus its right end. */
    private static final int RIGHT_END_WIDTH = SLOT_PITCH + 1;
    /** Gap between the hotbar and the frame, as for the vanilla offhand slot. */
    private static final int HOTBAR_GAP = 7;
    /** Vanilla's offhand slot with its gap to the hotbar, which the frame steps past when both are on one side. */
    private static final int OFFHAND_WIDTH = 29;
    /** Vanilla's hotbar attack indicator (18px tall) starts 20 rows above the bottom of the screen and ends 2 above it. */
    private static final int ATTACK_INDICATOR_TOP = 20;
    private static final int ATTACK_INDICATOR_BOTTOM = 2;
    /** Vanilla seeds the 9 hotbar slots and the offhand slot with 1-10; continue after them. */
    private static final int FIRST_SEED = 11;
    /**
     * Count text this wide starts at the item's left edge. Wider, it would cross the slot's border on the left, where
     * vanilla's own counts (at most two digits) never reach.
     */
    private static final int COUNT_TEXT_MAX_WIDTH = 17;
    /** Where vanilla's count text ends, from the item's top-left corner, and how tall it is with its shadow. */
    private static final int COUNT_TEXT_RIGHT = 17;
    private static final int COUNT_TEXT_BOTTOM = 17;
    private static final int COUNT_TEXT_HEIGHT = 8;
    private static final Identifier INVENTORY_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final Identifier INVENTORY_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    /** The inventory screen's panel within its 256x256 texture. */
    private static final int INVENTORY_PANEL_WIDTH = 176;
    private static final int INVENTORY_PANEL_HEIGHT = 166;
    private static final int INVENTORY_TEXTURE_SIZE = 256;
    /** The panel's outline, highlight or shadow, and one row of its fill; its rounded corners fit in this square. */
    private static final int INVENTORY_BORDER = 4;
    private static final int INVENTORY_SLOT = 18;

    /**
     * A frame's measurements: slots {@code pitch} apart, an end {@code end} wide before the first and after the last,
     * and items {@code itemInset} in from the frame's edge. Its short side is one slot plus both ends.
     */
    private record Style(boolean inventory, int pitch, int end, int itemInset) {
        static final Style HOTBAR = new Style(false, SLOT_PITCH, 1, 3);
        static final Style INVENTORY = new Style(true, INVENTORY_SLOT, INVENTORY_BORDER, INVENTORY_BORDER + 1);

        int length(int slotCount) {
            return 2 * end + slotCount * pitch;
        }

        int thickness() {
            return length(1);
        }
    }

    private record Slot(ItemStack stack, String countText) {
    }

    /**
     * Where the frame goes. {@code reversed} means the first slot is at the frame's far end (right or bottom), so
     * that it is the one nearest the anchor.
     */
    private record Layout(Style style, int x, int y, int width, int height, boolean vertical, boolean reversed,
                          boolean besideAttackIndicator) {
    }

    private static WeakReference<Player> cachedPlayer = new WeakReference<>(null);
    private static int cachedTick = -1;
    private static Slot cachedRocketSlot;

    private ElytraHud() {
    }

    /** Called after vanilla draws the hotbar. */
    public static void extract(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }
        List<Slot> slots = slots(player);
        if (slots.isEmpty()) {
            return;
        }

        int count = slots.size();
        Layout layout = layout(player, count, graphics.guiWidth(), graphics.guiHeight());
        Style style = layout.style();
        if (style.inventory()) {
            drawInventoryFrame(graphics, layout, count);
        } else if (layout.vertical()) {
            drawVerticalFrame(graphics, layout.x(), layout.y(), count);
        } else {
            drawFrame(graphics, layout.x(), layout.y(), layout.width());
        }
        for (int i = 0; i < count; i++) {
            int position = layout.reversed() ? count - 1 - i : i;
            // Same offsets as vanilla's slots: the item inset into the frame, then one slot pitch each.
            int x = layout.x() + style.itemInset() + (layout.vertical() ? 0 : position * style.pitch());
            int y = layout.y() + style.itemInset() + (layout.vertical() ? position * style.pitch() : 0);
            drawItem(minecraft, graphics, player, slots.get(i), x, y, FIRST_SEED + i);
        }
    }

    /**
     * How far vanilla's hotbar attack indicator moves outwards, so it sits beyond the frame rather than under it.
     * The indicator sits on the main-arm side, so the caller moves it right for a right main arm and left otherwise.
     * <p>
     * It moves only when the frame is beside the hotbar on that side and still covers any of the indicator's rows.
     * It then keeps the distance to the frame's outer edge that it has to the hotbar in vanilla: the frame's width
     * plus its gap to the hotbar, plus the X offset when that moves the frame outwards (minus it when inwards). It
     * never moves towards the hotbar.
     */
    public static int attackIndicatorShift() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.getCameraEntity() instanceof Player player)) {
            return 0;
        }
        List<Slot> slots = slots(player);
        if (slots.isEmpty()) {
            return 0;
        }
        // With the screen size 0 the layout is relative to the hotbar's bottom centre, as the indicator's rows are.
        Layout layout = layout(player, slots.size(), 0, 0);
        boolean coversIndicator = layout.y() < -ATTACK_INDICATOR_BOTTOM && layout.y() + layout.height() > -ATTACK_INDICATOR_TOP;
        if (!layout.besideAttackIndicator() || !coversIndicator) {
            return 0;
        }
        int offsetX = ElytraPlusConfig.get().hudOffsetX;
        int outwards = player.getMainArm() == HumanoidArm.RIGHT ? offsetX : -offsetX;
        return Math.max(0, HOTBAR_GAP + layout.width() + outwards);
    }

    private static Layout layout(Player player, int count, int guiWidth, int guiHeight) {
        ElytraPlusConfig config = ElytraPlusConfig.get();
        ElytraPlusConfig.HudAnchor anchor = Objects.requireNonNullElse(config.hudAnchor, ElytraPlusConfig.HudAnchor.HOTBAR);
        ElytraPlusConfig.HudSide side = Objects.requireNonNullElse(config.hudSide, ElytraPlusConfig.HudSide.MAIN_HAND);
        boolean vertical = config.hudOrientation == ElytraPlusConfig.HudOrientation.VERTICAL;
        HumanoidArm arm = switch (side) {
            case MAIN_HAND -> player.getMainArm();
            case LEFT -> HumanoidArm.LEFT;
            case RIGHT -> HumanoidArm.RIGHT;
        };
        boolean right = arm == HumanoidArm.RIGHT;
        Style style = config.hudStyle == ElytraPlusConfig.HudStyle.INVENTORY ? Style.INVENTORY : Style.HOTBAR;
        int length = style.length(count);
        int width = vertical ? style.thickness() : length;
        int height = vertical ? length : style.thickness();

        int x;
        int y;
        boolean reversed;
        boolean besideAttackIndicator = false;
        if (anchor == ElytraPlusConfig.HudAnchor.HOTBAR) {
            int center = guiWidth / 2;
            int distance = HOTBAR_HALF_WIDTH + HOTBAR_GAP;
            if (arm == player.getMainArm()) {
                besideAttackIndicator = true;
            } else if (!player.getOffhandItem().isEmpty()) {
                // Vanilla's offhand slot is on this side; go past it.
                distance += OFFHAND_WIDTH;
            }
            x = right ? center + distance : center - distance - width;
            y = guiHeight - height;
            // Nearest the hotbar: the inner end across, the bottom (level with the hotbar) down.
            reversed = vertical || !right;
        } else {
            x = right ? guiWidth - width : 0;
            boolean top = anchor == ElytraPlusConfig.HudAnchor.TOP;
            y = top ? 0 : guiHeight - height;
            // Nearest the screen edge: the side edge across, the top or bottom edge down.
            reversed = vertical ? !top : right;
        }
        return new Layout(style, x + config.hudOffsetX, y + config.hudOffsetY, width, height, vertical, reversed, besideAttackIndicator);
    }

    /** The slots to show, ordered from the hotbar outwards; empty when no elytra is worn. */
    private static List<Slot> slots(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.has(DataComponents.GLIDER)) {
            return List.of();
        }
        ElytraPlusConfig config = ElytraPlusConfig.get();
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
        return slots;
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

    /**
     * How much to shrink count text {@code width} wide so it stays within its slot. Vanilla ends the count text 17px
     * right of the item's left edge, so up to {@link #COUNT_TEXT_MAX_WIDTH} (two digits in the vanilla font) it is
     * drawn as is (1). Wider text is drawn as large as still fits: a little smaller for three digits, about seven
     * tenths for four.
     */
    public static float countTextScale(int width) {
        return width <= COUNT_TEXT_MAX_WIDTH ? 1 : (float) COUNT_TEXT_MAX_WIDTH / width;
    }

    /**
     * The hotbar's left end with its first {@code slotCount - 1} slots, then its last slot with its right end.
     * Both cuts are on slot boundaries, so the frame looks like a shorter hotbar.
     */
    private static void drawFrame(GuiGraphicsExtractor graphics, int x, int y, int width) {
        int leftWidth = width - RIGHT_END_WIDTH;
        blitHotbar(graphics, 0, 0, x, y, leftWidth, HOTBAR_HEIGHT);
        blitHotbar(graphics, HOTBAR_WIDTH - RIGHT_END_WIDTH, 0, x + leftWidth, y, RIGHT_END_WIDTH, HOTBAR_HEIGHT);
    }

    /**
     * A column of one-slot frames (the hotbar's left end, then its last slot and right end) sharing their ends: the
     * hotbar's top row, its 20 slot rows once per slot, then its bottom row.
     */
    private static void drawVerticalFrame(GuiGraphicsExtractor graphics, int x, int y, int slotCount) {
        drawOneSlotRows(graphics, x, y, 0, 1);
        for (int i = 0; i < slotCount; i++) {
            drawOneSlotRows(graphics, x, y + 1 + i * SLOT_PITCH, 1, SLOT_PITCH);
        }
        drawOneSlotRows(graphics, x, y + 1 + slotCount * SLOT_PITCH, HOTBAR_HEIGHT - 1, 1);
    }

    /** Rows {@code v} to {@code v + height} of a one-slot frame. */
    private static void drawOneSlotRows(GuiGraphicsExtractor graphics, int x, int y, int v, int height) {
        blitHotbar(graphics, 0, v, x, y, 1, height);
        blitHotbar(graphics, HOTBAR_WIDTH - RIGHT_END_WIDTH, v, x + 1, y, RIGHT_END_WIDTH, height);
    }

    private static void blitHotbar(GuiGraphicsExtractor graphics, int u, int v, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, HOTBAR_WIDTH, HOTBAR_HEIGHT, u, v, x, y, width, height);
    }

    /**
     * The inventory screen's panel cut down to fit the slots: its four corners, then its straight edges next to each
     * corner, stretched no further than they run in the texture. Inside, one inventory slot sprite per slot.
     */
    private static void drawInventoryFrame(GuiGraphicsExtractor graphics, Layout layout, int slotCount) {
        int b = INVENTORY_BORDER;
        int x = layout.x();
        int y = layout.y();
        int width = layout.width();
        int height = layout.height();
        int right = INVENTORY_PANEL_WIDTH - b;
        int bottom = INVENTORY_PANEL_HEIGHT - b;
        blitInventory(graphics, 0, 0, x, y, b, b);
        blitInventory(graphics, right, 0, x + width - b, y, b, b);
        blitInventory(graphics, 0, bottom, x, y + height - b, b, b);
        blitInventory(graphics, right, bottom, x + width - b, y + height - b, b, b);
        blitInventory(graphics, b, 0, x + b, y, width - 2 * b, b);
        blitInventory(graphics, b, bottom, x + b, y + height - b, width - 2 * b, b);
        blitInventory(graphics, 0, b, x, y + b, b, height - 2 * b);
        blitInventory(graphics, right, b, x + width - b, y + b, b, height - 2 * b);
        for (int i = 0; i < slotCount; i++) {
            int slotX = x + b + (layout.vertical() ? 0 : i * INVENTORY_SLOT);
            int slotY = y + b + (layout.vertical() ? i * INVENTORY_SLOT : 0);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INVENTORY_SLOT_SPRITE, slotX, slotY, INVENTORY_SLOT, INVENTORY_SLOT);
        }
    }

    private static void blitInventory(GuiGraphicsExtractor graphics, int u, int v, int x, int y, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, x, y, u, v, width, height, INVENTORY_TEXTURE_SIZE, INVENTORY_TEXTURE_SIZE);
    }

    /**
     * As vanilla draws a hotbar item; the count, when given, goes through vanilla's own count text. A count too wide
     * for the slot (four digits or more) is drawn smaller instead, ending where vanilla's would, at its right and
     * bottom edges.
     */
    private static void drawItem(Minecraft minecraft, GuiGraphicsExtractor graphics, Player player, Slot slot, int x, int y, int seed) {
        Font font = minecraft.font;
        graphics.item(player, slot.stack(), x, y, seed);
        String count = slot.countText();
        int width = count == null ? 0 : font.width(count);
        float scale = countTextScale(width);
        if (scale == 1) {
            graphics.itemDecorations(font, slot.stack(), x, y, count);
            return;
        }
        // Everything but the count (a single item shows no count), then the count.
        graphics.itemDecorations(font, slot.stack().copyWithCount(1), x, y, null);
        graphics.pose().pushMatrix();
        // Vanilla's count text ends 17px right of the item and its shadow 17px below it (7px glyphs + 1px shadow).
        graphics.pose().translate(x + COUNT_TEXT_RIGHT - width * scale, y + COUNT_TEXT_BOTTOM - COUNT_TEXT_HEIGHT * scale);
        graphics.pose().scale(scale, scale);
        graphics.text(font, count, 0, 0, -1, true);
        graphics.pose().popMatrix();
    }
}
