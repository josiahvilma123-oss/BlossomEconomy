package com.blossomsmp.economy;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Live market prices.
 *
 * Every item has a "worth" (its normal sell price, from config.yml).
 * Every item also has a market factor that starts at 1.0:
 *   - selling an item lowers its factor (price goes down)
 *   - buying an item from /shop raises its factor (price goes up)
 *   - over time every factor drifts back towards 1.0
 *
 * Sell price = worth x factor
 * Buy price  = worth x buy-multiplier x max(1, factor)   (buy prices never drop below normal,
 *                                                         which stops buy-cheap/craft/sell tricks)
 */
public class MarketManager {

    private final BlossomEconomy plugin;
    private final File file;
    private final Map<Material, Double> worth = new EnumMap<>(Material.class);
    private final Map<Material, Double> factors = new EnumMap<>(Material.class);

    private double buyMultiplier = 3.0;
    private double impact = 0.001;
    private double minFactor = 0.25;
    private double maxFactor = 3.0;
    private double recoveryPercent = 5;
    private boolean dirty = false;

    public MarketManager(BlossomEconomy plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "market.yml");
    }

    // ------------------------------------------------------------------
    // Loading / saving
    // ------------------------------------------------------------------

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        buyMultiplier = Math.max(1.0, config.getDouble("market.buy-multiplier", 3.0));
        impact = clamp(config.getDouble("market.impact-per-item", 0.001), 0, 0.5);
        minFactor = clamp(config.getDouble("market.min-price-percent", 25) / 100.0, 0.01, 1);
        maxFactor = Math.max(1, config.getDouble("market.max-price-percent", 300) / 100.0);
        recoveryPercent = clamp(config.getDouble("market.recovery-percent", 5), 0, 100);

        worth.clear();
        ConfigurationSection prices = config.getConfigurationSection("prices");
        if (prices != null) {
            for (String key : prices.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                double value = prices.getDouble(key);
                if (material == null || !material.isItem() || material.isAir() || value <= 0) {
                    plugin.getLogger().warning("Prices: skipping invalid item '" + key + "'");
                    continue;
                }
                worth.put(material, value);
            }
        }
        plugin.getLogger().info("Loaded " + worth.size() + " item prices.");
    }

    public void loadData() {
        factors.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("factors");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material != null) {
                factors.put(material, clamp(section.getDouble(key, 1.0), minFactor, maxFactor));
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<Material, Double> entry : factors.entrySet()) {
            yaml.set("factors." + entry.getKey().name(), Math.round(entry.getValue() * 10000.0) / 10000.0);
        }
        try {
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save market.yml: " + e.getMessage());
        }
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    // ------------------------------------------------------------------
    // Prices
    // ------------------------------------------------------------------

    public boolean hasPrice(Material material) {
        return worth.containsKey(material);
    }

    public double getFactor(Material material) {
        return factors.getOrDefault(material, 1.0);
    }

    /** Can this exact item be sold? (Named, enchanted and damaged items are protected.) */
    public boolean canSell(ItemStack item) {
        if (item == null || item.getType().isAir() || !worth.containsKey(item.getType())) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName() || meta.hasEnchants()) {
                return false;
            }
            if (meta instanceof Damageable damageable && damageable.hasDamage()) {
                return false;
            }
        }
        return true;
    }

    /** Current sell price for one item. */
    public double sellPrice(Material material) {
        return round(worth.getOrDefault(material, 0.0) * getFactor(material));
    }

    /** Current buy price for one item. */
    public double buyPrice(Material material) {
        return round(worth.getOrDefault(material, 0.0) * buyMultiplier * Math.max(1.0, getFactor(material)));
    }

    public double quoteSell(Material material, int amount) {
        return simulate(material, amount, true, false);
    }

    /** Sells and moves the price down. Returns the money earned. */
    public double sell(Material material, int amount) {
        return simulate(material, amount, true, true);
    }

    public double quoteBuy(Material material, int amount) {
        return simulate(material, amount, false, false);
    }

    /** Buys and moves the price up. Returns the cost. */
    public double buy(Material material, int amount) {
        return simulate(material, amount, false, true);
    }

    private double simulate(Material material, int amount, boolean selling, boolean apply) {
        Double base = worth.get(material);
        if (base == null || amount <= 0) {
            return 0;
        }
        double factor = getFactor(material);
        double total = 0;
        for (int i = 0; i < amount; i++) {
            if (selling) {
                total += base * factor;
                factor = Math.max(minFactor, factor * (1 - impact));
            } else {
                total += base * buyMultiplier * Math.max(1.0, factor);
                factor = Math.min(maxFactor, factor * (1 + impact));
            }
        }
        if (apply) {
            factors.put(material, factor);
            dirty = true;
        }
        return round(total);
    }

    /** "▲ +12%", "▼ -30%" or "● 0%" with colours. */
    public String trend(Material material) {
        double factor = getFactor(material);
        long percent = Math.round((factor - 1) * 100);
        if (percent > 0) {
            return "&a▲ +" + percent + "%";
        }
        if (percent < 0) {
            return "&c▼ " + percent + "%";
        }
        return "&7● normal";
    }

    /** Moves every price a little back towards normal. Called on a timer. */
    public void recover() {
        if (recoveryPercent <= 0 || factors.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Material, Double>> it = factors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Material, Double> entry = it.next();
            double factor = entry.getValue();
            factor += (1.0 - factor) * (recoveryPercent / 100.0);
            if (Math.abs(1.0 - factor) < 0.005) {
                it.remove();
            } else {
                entry.setValue(factor);
            }
        }
        dirty = true;
    }

    /** Puts every price back to normal. */
    public void resetAll() {
        factors.clear();
        dirty = true;
        save();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
