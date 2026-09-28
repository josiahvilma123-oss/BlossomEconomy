package com.blossomsmp.economy.menus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/** Marks an inventory as one of our menus and remembers what each slot does. */
public class MenuHolder implements InventoryHolder {

    public enum Type {
        SHOP_MAIN,
        SHOP_CATEGORY,
        SELL
    }

    private final Type type;
    private final String categoryId;
    private final Map<Integer, String> slotActions = new HashMap<>();
    private Inventory inventory;

    public MenuHolder(Type type, String categoryId) {
        this.type = type;
        this.categoryId = categoryId;
    }

    public Type getType() {
        return type;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public Map<Integer, String> getSlotActions() {
        return slotActions;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
