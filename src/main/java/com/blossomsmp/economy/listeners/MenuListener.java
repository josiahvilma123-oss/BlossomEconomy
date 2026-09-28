package com.blossomsmp.economy.listeners;

import com.blossomsmp.economy.BlossomEconomy;
import com.blossomsmp.economy.ShopManager;
import com.blossomsmp.economy.menus.MenuHolder;
import com.blossomsmp.economy.menus.Menus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class MenuListener implements Listener {

    private final BlossomEconomy plugin;

    public MenuListener(BlossomEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }
        if (holder.getType() == MenuHolder.Type.SELL) {
            return; // players can freely move items in the sell menu
        }

        // Shop menus: nothing can be taken or moved
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT
                && click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) {
            return;
        }

        if (holder.getType() == MenuHolder.Type.SHOP_MAIN) {
            String categoryId = holder.getSlotActions().get(slot);
            if (categoryId != null) {
                Menus.openCategory(plugin, player, categoryId);
            }
            return;
        }

        // SHOP_CATEGORY
        if (Menus.ACTION_BACK.equals(holder.getSlotActions().get(slot))) {
            Menus.openShop(plugin, player);
            return;
        }
        ShopManager.Category category = plugin.getShop().getCategory(holder.getCategoryId());
        if (category == null) {
            player.closeInventory();
            return;
        }
        List<ShopManager.ShopItem> items = category.items();
        if (slot >= items.size() || slot >= ShopManager.MAX_ITEMS_PER_CATEGORY) {
            return;
        }
        int amount = click.isShiftClick() ? 64 : click.isRightClick() ? 16 : 1;
        Menus.buy(plugin, player, items.get(slot), amount);
        Menus.refreshBalance(plugin, player, top, holder);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof MenuHolder holder && holder.getType() != MenuHolder.Type.SELL) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < top.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof MenuHolder holder
                && holder.getType() == MenuHolder.Type.SELL
                && event.getPlayer() instanceof Player player) {
            Menus.sellMenuContents(plugin, player, inv);
        }
    }
}
