package com.blossomsmp.economy.listeners;

import com.blossomsmp.economy.BlossomEconomy;
import com.blossomsmp.economy.MarketManager;
import com.blossomsmp.economy.util.Text;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * When a player switches to a different item in their hand, shows its value
 * above the hotbar for a few seconds. Runs every half second.
 */
public class PriceDisplayTask implements Runnable {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('\u00A7')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final BlossomEconomy plugin;
    private final Map<UUID, String> lastShown = new HashMap<>();

    public PriceDisplayTask(BlossomEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("price-display.enabled", true)) {
            lastShown.clear();
            return;
        }
        MarketManager market = plugin.getMarket();
        String format = plugin.getConfig().getString("price-display.format",
                "&7Worth: &a%each% &8each &8| &a%stack% &7stack &8| %trend%");
        Set<UUID> online = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            online.add(player.getUniqueId());
            ItemStack hand = player.getInventory().getItemInMainHand();
            String key = hand.getType().name() + ":" + hand.getAmount() + ":" + hand.hashCode();
            if (key.equals(lastShown.put(player.getUniqueId(), key))) {
                continue; // same item as before, don't spam
            }
            if (hand.getType().isAir() || !market.canSell(hand)) {
                continue;
            }
            String text = format
                    .replace("%each%", Text.money(market.sellPrice(hand)))
                    .replace("%stack%", Text.money(market.quoteSell(hand)))
                    .replace("%trend%", market.trend(hand.getType()))
                    .replace("%item%", Text.itemName(hand.getType()));
            player.sendActionBar(LEGACY.deserialize(Text.color(text)));
        }
        lastShown.keySet().retainAll(online);
    }
}
