package net.blueva.arcade.modules.race.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.List;

public class RaceLoadoutService {

    private final ModuleConfigAPI moduleConfig;

    public RaceLoadoutService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void giveStartingItems(Player player) {
        List<String> startingItems = moduleConfig.getStringList("items.starting_items");

        if (startingItems == null || startingItems.isEmpty()) {
            return;
        }

        for (String itemString : startingItems) {
            try {
                String[] parts = itemString.split(":");
                if (parts.length >= 2) {
                    String itemId = parts[0];
                    int amount = Integer.parseInt(parts[1]);
                    int slot = parts.length >= 3 ? Integer.parseInt(parts[2]) : -1;

                    ItemStack item = new ItemStack(itemId, amount);
                    addItem(player, item, slot);
                }
            } catch (Exception ignored) {
                // Ignore malformed entries
            }
        }
    }

    public void applyStartingEffects(Player player) {
        applyEffects(player, moduleConfig.getStringList("effects.starting_effects"));
    }

    public void applyRespawnEffects(Player player) {
        applyEffects(player, moduleConfig.getStringList("effects.respawn_effects"));
    }

    private void applyEffects(Player player, List<String> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }
        // Status effect application is not yet available in the Hytale runtime.
    }

    private void addItem(Player player, ItemStack item, int slot) {
        if (player == null || item == null || player.getInventory() == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (slot >= 0) {
            ItemContainer hotbar = inventory.getHotbar();
            if (slot < 9 && hotbar != null) {
                hotbar.addItemStackToSlot((short) slot, item);
                return;
            }

            ItemContainer storage = inventory.getStorage();
            if (storage != null) {
                short storageSlot = (short) Math.max(0, slot - 9);
                if (storageSlot < storage.getCapacity()) {
                    storage.addItemStackToSlot(storageSlot, item);
                } else {
                    storage.addItemStack(item);
                }
            }
            return;
        }

        ItemContainer storage = inventory.getStorage();
        if (storage != null) {
            storage.addItemStack(item);
        }
    }
}
