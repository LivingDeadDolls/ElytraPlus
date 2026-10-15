package elytraplus.feature;

import elytraplus.config.ElytraPlusConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Firework Hotkey: one key fires a rocket while gliding, wherever it is in the inventory.
 * A rocket outside the hands is swapped into the selected hotbar slot, used, and swapped back.
 */
public final class FireworkHotkey {
    private static final int MAX_BUNDLE_DEPTH = 8;
    private static final int NONE = -1;
    private static final int IN_MAIN_HAND = -2;
    private static final int IN_OFFHAND = -3;

    private FireworkHotkey() {
    }

    /** Called once per press of the firework key. */
    public static void use(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || !ElytraPlusConfig.get().fireworkHotkey || !player.isFallFlying()) {
            return;
        }
        int source = findRocket(player);
        if (source == IN_MAIN_HAND) {
            minecraft.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        } else if (source == IN_OFFHAND) {
            minecraft.gameMode.useItem(player, InteractionHand.OFF_HAND);
        } else if (source != NONE && Inventories.canClick(minecraft, player)) {
            int slot = Inventories.menuSlotOf(source);
            int selected = player.getInventory().getSelectedSlot();
            Inventories.swap(minecraft, player, slot, selected);
            minecraft.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            Inventories.swap(minecraft, player, slot, selected);
        }
    }

    /** The rocket the next key press will use, or EMPTY. */
    public static ItemStack nextRocket(Player player) {
        int source = findRocket(player);
        if (source == IN_MAIN_HAND) {
            return player.getMainHandItem();
        }
        if (source == IN_OFFHAND) {
            return player.getOffhandItem();
        }
        return source == NONE ? ItemStack.EMPTY : player.getInventory().getItem(source);
    }

    /**
     * Rockets across the hotbar, main inventory and offhand, including those inside shulker boxes and bundles
     * (counted only; {@link #use} cannot take rockets out of them).
     */
    public static int countRockets(Player player) {
        int total = rocketsIn(player.getOffhandItem());
        for (int index = 0; index < 36; index++) {
            total += rocketsIn(player.getInventory().getItem(index));
        }
        return total;
    }

    private static int rocketsIn(ItemStack stack) {
        if (stack.is(Items.FIREWORK_ROCKET)) {
            return stack.getCount();
        }
        int total = 0;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            for (ItemStackTemplate item : container.nonEmptyItems()) {
                total += rocketsIn(item, 0);
            }
        }
        total += rocketsInBundle(stack.get(DataComponents.BUNDLE_CONTENTS), 0);
        return total;
    }

    /** Container contents are templates; bundles may hold bundles, so recurse a few levels. */
    private static int rocketsIn(ItemStackTemplate item, int depth) {
        if (item.item().value() == Items.FIREWORK_ROCKET) {
            return item.count();
        }
        return rocketsInBundle(item.get(DataComponents.BUNDLE_CONTENTS), depth + 1);
    }

    private static int rocketsInBundle(BundleContents bundle, int depth) {
        if (bundle == null || depth > MAX_BUNDLE_DEPTH) {
            return 0;
        }
        int total = 0;
        for (ItemStackTemplate item : bundle.items()) {
            total += rocketsIn(item, depth);
        }
        return total;
    }

    /**
     * Where the next rocket comes from: main hand, then offhand, then the hotbar and main inventory.
     * Returns {@link #IN_MAIN_HAND}, {@link #IN_OFFHAND}, an Inventory index (0..35) or {@link #NONE}.
     */
    private static int findRocket(Player player) {
        if (player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
            return IN_MAIN_HAND;
        }
        if (player.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
            return IN_OFFHAND;
        }
        for (int index = 0; index < 36; index++) {
            if (player.getInventory().getItem(index).is(Items.FIREWORK_ROCKET)) {
                return index;
            }
        }
        return NONE;
    }
}
