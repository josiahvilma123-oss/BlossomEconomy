package com.blossomsmp.economy;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads shop categories, buy prices and sell prices from config.yml. */
public class ShopManager {

    public static final int MAX_ITEMS_PER_CATEGORY = 45;

    public record ShopItem(Material material, double buyPrice) {
    }

    public record Category(String id, String name, Material icon, int slot, List<ShopItem> items) {
    }

    private final BlossomEconomy plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<Material, Double> buyPrices = new EnumMap<>(Material.class);
    private final Map<Material, Double> sellPrices = new EnumMap<>(Material.class);
    private double sellRatio = 0.3;

    public ShopManager(BlossomEconomy plugin) {
        this.plugin = plugin;
    }

    public void load() {
        categories.clear();
        buyPrices.clear();
        sellPrices.clear();
        FileConfiguration config = plugin.getConfig();
        sellRatio = Math.max(0, Math.min(1, config.getDouble("shop.sell-ratio", 0.3)));

        ConfigurationSection cats = config.getConfigurationSection("shop.categories");
        if (cats != null) {
            for (String id : cats.getKeys(false)) {
                ConfigurationSection section = cats.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                List<ShopItem> items = new ArrayList<>();
                ConfigurationSection itemSection = section.getConfigurationSection("items");
                if (itemSection != null) {
                    for (String key : itemSection.getKeys(false)) {
                        Material material = parseMaterial(key);
                        double price = itemSection.getDouble(key);
                        if (material == null || price <= 0) {
                            plugin.getLogger().warning("Shop: skipping invalid item '" + key + "' in category " + id);
                            continue;
                        }
                        if (items.size() >= MAX_ITEMS_PER_CATEGORY) {
                            plugin.getLogger().warning("Shop: category " + id + " has more than "
                                    + MAX_ITEMS_PER_CATEGORY + " items, the rest are ignored.");
                            break;
                        }
                        items.add(new ShopItem(material, price));
                        buyPrices.putIfAbsent(material, price);
                    }
                }
                Material icon = parseMaterial(section.getString("icon"));
                categories.put(id, new Category(id, section.getString("name", id),
                        icon != null ? icon : Material.CHEST, section.getInt("slot", -1),
                        Collections.unmodifiableList(items)));
            }
        }

        ConfigurationSection sell = config.getConfigurationSection("sell-prices");
        if (sell != null) {
            for (String key : sell.getKeys(false)) {
                Material material = parseMaterial(key);
                double price = sell.getDouble(key);
                if (material == null || price <= 0) {
                    plugin.getLogger().warning("Sell prices: skipping invalid item '" + key + "'");
                    continue;
                }
                sellPrices.put(material, price);
            }
        }
        plugin.getLogger().info("Loaded " + categories.size() + " shop categories and "
                + (buyPrices.size() + sellPrices.size()) + " prices.");
    }

    public Collection<Category> getCategories() {
        return categories.values();
    }

    public Category getCategory(String id) {
        return categories.get(id);
    }

    /** Sell price for ONE of this item, or 0 if it can't be sold. */
    public double getSellPrice(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Protect named, enchanted or damaged items from being sold by accident
            if (meta.hasDisplayName() || meta.hasEnchants()) {
                return 0;
            }
            if (meta instanceof Damageable damageable && damageable.hasDamage()) {
                return 0;
            }
        }
        Double price = sellPrices.get(item.getType());
        if (price != null) {
            return price;
        }
        Double buy = buyPrices.get(item.getType());
        return buy == null ? 0 : Math.round(buy * sellRatio * 100.0) / 100.0;
    }

    private static Material parseMaterial(String name) {
        if (name == null) {
            return null;
        }
        Material material = Material.matchMaterial(name);
        return material != null && material.isItem() && !material.isAir() ? material : null;
    }
}
