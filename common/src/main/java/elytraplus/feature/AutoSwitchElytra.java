package elytraplus.feature;

import elytraplus.config.ElytraPlusConfig;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Auto Switch Elytra: jumping in mid-air puts on the best elytra from the inventory,
 * and the original chest item goes back on once gliding stops.
 * Only switches made here are undone; a manually worn elytra is left alone.
 */
public final class AutoSwitchElytra {
    /** Elytras with less durability left than this are skipped. */
    private static final int MIN_DURABILITY_LEFT = 10;

    /**
     * The connection the switch happened on. Keyed to the connection rather than the player, because the client
     * creates a new LocalPlayer on respawn and dimension change, and the restore should survive those.
     * Weak, so a pending restore does not keep a closed world in memory.
     */
    private static WeakReference<ClientPacketListener> switchedConnection;
    private static ItemStack previousChest = ItemStack.EMPTY;

    private AutoSwitchElytra() {
    }

    /** Called right before the client tries to start gliding (jump pressed in mid-air). */
    public static void beforeStartFallFlying(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ElytraPlusConfig.get().autoSwitchElytra || !Inventories.canClick(minecraft, player)) {
            return;
        }
        // Same preconditions as gliding itself, minus the elytra check.
        if (player.isFallFlying() || player.onGround() || player.isPassenger() || player.isInWater()
                || player.hasEffect(MobEffects.LEVITATION)) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (isUsableElytra(chest)) {
            return;
        }
        int slot = findBestElytra(player);
        if (slot < 0) {
            return;
        }
        // Replacing a worn-out elytra is not something to undo on landing; keep any earlier pending restore as is.
        if (!chest.has(DataComponents.GLIDER)) {
            switchedConnection = new WeakReference<>(minecraft.getConnection());
            previousChest = chest.copy();
        }
        Inventories.swapWithChest(minecraft, player, slot);
    }

    /** Called at the end of every player tick; puts the previous chest item back once gliding has stopped. */
    public static void tick(LocalPlayer player) {
        if (switchedConnection == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (switchedConnection.get() != minecraft.getConnection()) {
            // Joined another world or server: the remembered switch no longer applies.
            reset();
            return;
        }
        if (player.isFallFlying() || !Inventories.canClick(minecraft, player)) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) {
            if (previousChest.isEmpty()) {
                Inventories.quickMoveChest(minecraft, player);
            } else {
                int slot = findPreviousChest(player);
                if (slot >= 0) {
                    Inventories.swapWithChest(minecraft, player, slot);
                }
            }
        }
        reset();
    }

    private static void reset() {
        switchedConnection = null;
        previousChest = ItemStack.EMPTY;
    }

    private static boolean isUsableElytra(ItemStack stack) {
        return LivingEntity.canGlideUsing(stack, EquipmentSlot.CHEST) && durabilityLeft(stack) >= MIN_DURABILITY_LEFT;
    }

    private static int durabilityLeft(ItemStack stack) {
        return stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
    }

    /** Highest Unbreaking first, then most durability left. */
    private static int findBestElytra(LocalPlayer player) {
        int best = -1;
        int bestUnbreaking = -1;
        int bestDurability = -1;
        for (int slot = Inventories.FIRST_MAIN_SLOT; slot <= Inventories.OFFHAND_SLOT; slot++) {
            ItemStack stack = Inventories.item(player, slot);
            if (!isUsableElytra(stack)) {
                continue;
            }
            int unbreaking = unbreakingLevel(stack);
            int durability = durabilityLeft(stack);
            if (unbreaking > bestUnbreaking || (unbreaking == bestUnbreaking && durability > bestDurability)) {
                best = slot;
                bestUnbreaking = unbreaking;
                bestDurability = durability;
            }
        }
        return best;
    }

    private static int unbreakingLevel(ItemStack stack) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(Enchantments.UNBREAKING)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    /** The remembered chest item, matched while ignoring durability (it may have taken damage since). */
    private static int findPreviousChest(LocalPlayer player) {
        ItemStack wanted = withoutDamage(previousChest);
        for (int slot = Inventories.FIRST_MAIN_SLOT; slot <= Inventories.OFFHAND_SLOT; slot++) {
            ItemStack stack = Inventories.item(player, slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(withoutDamage(stack), wanted)) {
                return slot;
            }
        }
        return -1;
    }

    private static ItemStack withoutDamage(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        if (copy.isDamageableItem()) {
            copy.setDamageValue(0);
        }
        return copy;
    }
}
