package elytraplus.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Moves items by sending the same inventory clicks a player could make, so it works on vanilla servers.
 * Slot numbers are {@link net.minecraft.world.inventory.InventoryMenu} slots.
 */
public final class Inventories {
    public static final int CHEST_SLOT = 6;
    public static final int FIRST_MAIN_SLOT = 9;
    public static final int FIRST_HOTBAR_SLOT = 36;
    public static final int OFFHAND_SLOT = 45;
    /** SWAP button that targets the offhand instead of a hotbar slot. */
    private static final int OFFHAND_BUTTON = 40;

    private Inventories() {
    }

    /** True when no screen is open and nothing is held on the cursor, so clicks cannot disturb the player. */
    public static boolean canClick(Minecraft minecraft, LocalPlayer player) {
        return minecraft.gameMode != null
                && minecraft.gui.screen() == null
                && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu
                && player.inventoryMenu.getCarried().isEmpty();
    }

    public static ItemStack item(LocalPlayer player, int slot) {
        return player.inventoryMenu.getSlot(slot).getItem();
    }

    /** Menu slot for an {@link net.minecraft.world.entity.player.Inventory} index in 0..35. */
    public static int menuSlotOf(int inventoryIndex) {
        return inventoryIndex < 9 ? FIRST_HOTBAR_SLOT + inventoryIndex : inventoryIndex;
    }

    /** Swaps a menu slot (main inventory, hotbar or offhand) with the chest armor slot. */
    public static void swapWithChest(Minecraft minecraft, LocalPlayer player, int slot) {
        if (slot >= FIRST_HOTBAR_SLOT && slot < OFFHAND_SLOT) {
            swap(minecraft, player, CHEST_SLOT, slot - FIRST_HOTBAR_SLOT);
        } else if (slot == OFFHAND_SLOT) {
            swap(minecraft, player, CHEST_SLOT, OFFHAND_BUTTON);
        } else {
            // Main inventory: go through hotbar slot 0 and put its item back afterwards.
            swap(minecraft, player, slot, 0);
            swap(minecraft, player, CHEST_SLOT, 0);
            swap(minecraft, player, slot, 0);
        }
    }

    /** Swaps a menu slot with a hotbar slot (0..8). */
    public static void swap(Minecraft minecraft, LocalPlayer player, int slot, int hotbarSlot) {
        minecraft.gameMode.handleContainerInput(player.inventoryMenu.containerId, slot, hotbarSlot, ContainerInput.SWAP, player);
    }

    /** Shift-clicks the chest armor slot, moving its item into the inventory if there is room. */
    public static void quickMoveChest(Minecraft minecraft, LocalPlayer player) {
        minecraft.gameMode.handleContainerInput(player.inventoryMenu.containerId, CHEST_SLOT, 0, ContainerInput.QUICK_MOVE, player);
    }
}
