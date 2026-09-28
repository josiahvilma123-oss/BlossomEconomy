package com.blossomsmp.economy.menus;

import com.blossomsmp.economy.BlossomEconomy;
import com.blossomsmp.economy.ShopManager;
import com.blossomsmp.economy.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds the menus and does the buying and selling. */
public final class Menus {

    public static final String ACTION_BACK = "back";
    public static final int CATEGORY_BACK_SLOT = 49;
    public static final int CATEGORY_BALANCE_SLOT = 53;
    private static final int MAIN_BALANCE_SLOT = 13;

    private Menus() {
    }

    // ------------------------------------------------------------------
    // Opening menus
    // ------------------------------------------------------------------

    public static void openShop(BlossomEconomy plugin, Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.Type.SHOP_MAIN, null);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Text.color(plugin.getConfig().getString("shop.title", "&#FF69B4&l❀ Blossom Shop")));
        holder.setInventory(inv);
        fill(plugin, inv, 0, 27);

        int nextFreeSlot = 10;
        for (ShopManager.Category category : plugin.getShop().getCategories()) {
            int slot = category.slot();
            if (slot < 0 || slot >= 27 || slot == MAIN_BALANCE_SLOT || holder.getSlotActions().containsKey(slot)) {
                while (nextFreeSlot < 27 && (nextFreeSlot == MAIN_BALANCE_SLOT
                        || holder.getSlotActions().containsKey(nextFreeSlot))) {
                    nextFreeSlot++;
                }
                if (nextFreeSlot >= 27) {
                    break;
                }
                slot = nextFreeSlot;
            }
            inv.setItem(slot, item(category.icon(), category.name(), List.of(
                    "&7" + category.items().size() + " items",
                    "",
                    "&#FFB6C1Click to browse")));
            holder.getSlotActions().put(slot, category.id());
        }
        inv.setItem(MAIN_BALANCE_SLOT, balanceItem(plugin, player));
        player.openInventory(inv);
        click(player);
    }

    public static void openCategory(BlossomEconomy plugin, Player player, String categoryId) {
        ShopManager.Category category = plugin.getShop().getCategory(categoryId);
        if (category == null) {
            openShop(plugin, player);
            return;
        }
        MenuHolder holder = new MenuHolder(MenuHolder.Type.SHOP_CATEGORY, categoryId);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.color(category.name()));
        holder.setInventory(inv);

        List<ShopManager.ShopItem> items = category.items();
        for (int i = 0; i < items.size() && i < ShopManager.MAX_ITEMS_PER_CATEGORY; i++) {
            ShopManager.ShopItem shopItem = items.get(i);
            double price = shopItem.buyPrice();
            inv.setItem(i, item(shopItem.material(), "&f" + Text.itemName(shopItem.material()), List.of(
                    "&7Price: &a" + Text.money(price) + " &7each",
                    "",
                    "&#FFB6C1Left-click &8» &7Buy 1 &8(&a" + Text.money(price) + "&8)",
                    "&#FFB6C1Right-click &8» &7Buy 16 &8(&a" + Text.money(price * 16) + "&8)",
                    "&#FFB6C1Shift-click &8» &7Buy 64 &8(&a" + Text.money(price * 64) + "&8)")));
        }
        fill(plugin, inv, 45, 54);
        inv.setItem(CATEGORY_BACK_SLOT, item(Material.ARROW, "&#FF69B4&lBack", List.of("&7Return to the shop")));
        holder.getSlotActions().put(CATEGORY_BACK_SLOT, ACTION_BACK);
        inv.setItem(CATEGORY_BALANCE_SLOT, balanceItem(plugin, player));
        player.openInventory(inv);
        click(player);
    }

    public static void openSell(BlossomEconomy plugin, Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.Type.SELL, null);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.color(plugin.getConfig().getString("menus.sell-title", "&#FF69B4&l❀ Sell")));
        holder.setInventory(inv);
        player.openInventory(inv);
        click(player);
    }

    // ------------------------------------------------------------------
    // Buying
    // ------------------------------------------------------------------

    public static void buy(BlossomEconomy plugin, Player player, ShopManager.ShopItem shopItem, int amount) {
        double cost = Math.round(shopItem.buyPrice() * amount * 100.0) / 100.0;
        if (!fits(player.getInventory(), shopItem.material(), amount)) {
            player.sendMessage(plugin.msg("inventory-full"));
            fail(player);
            return;
        }
        if (!plugin.getEconomy().withdraw(player.getUniqueId(), cost)) {
            player.sendMessage(plugin.msg("not-enough-money",
                    "%amount%", Text.money(plugin.getEconomy().getBalance(player.getUniqueId()))));
            fail(player);
            return;
        }
        give(player, shopItem.material(), amount);
        player.sendMessage(plugin.msg("bought",
                "%count%", String.valueOf(amount),
                "%item%", Text.itemName(shopItem.material()),
                "%amount%", Text.money(cost)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
    }

    /** Updates the balance display in an open shop menu. */
    public static void refreshBalance(BlossomEconomy plugin, Player player, Inventory inv, MenuHolder holder) {
        int slot = holder.getType() == MenuHolder.Type.SHOP_MAIN ? MAIN_BALANCE_SLOT : CATEGORY_BALANCE_SLOT;
        inv.setItem(slot, balanceItem(plugin, player));
    }

    private static boolean fits(PlayerInventory inventory, Material material, int amount) {
        int maxStack = material.getMaxStackSize();
        ItemStack sample = new ItemStack(material);
        int space = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                space += maxStack;
            } else if (stack.isSimilar(sample)) {
                space += Math.max(0, maxStack - stack.getAmount());
            }
            if (space >= amount) {
                return true;
            }
        }
        return space >= amount;
    }

    private static void give(Player player, Material material, int amount) {
        int maxStack = material.getMaxStackSize();
        int left = amount;
        while (left > 0) {
            int size = Math.min(left, maxStack);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack(material, size));
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            left -= size;
        }
    }

    // ------------------------------------------------------------------
    // Selling
    // ------------------------------------------------------------------

    /**
     * Sells everything sellable in the given sell-menu inventory, clears it and
     * gives unsellable items back to the player.
     */
    public static void sellMenuContents(BlossomEconomy plugin, Player player, Inventory inv) {
        List<ItemStack> unsold = new ArrayList<>();
        double total = 0;
        int count = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            double each = plugin.getShop().getSellPrice(stack);
            if (each > 0) {
                total += each * stack.getAmount();
                count += stack.getAmount();
            } else {
                unsold.add(stack);
            }
        }
        inv.clear();

        if (!unsold.isEmpty()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(unsold.toArray(new ItemStack[0]));
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        finishSale(plugin, player, total, count, !unsold.isEmpty(), true);
    }

    /** Sells every sellable item in the player's inventory (/sell all). */
    public static void sellAll(BlossomEconomy plugin, Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        double total = 0;
        int count = 0;
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack stack = storage[slot];
            double each = plugin.getShop().getSellPrice(stack);
            if (each > 0) {
                total += each * stack.getAmount();
                count += stack.getAmount();
                inventory.setItem(slot, null);
            }
        }
        finishSale(plugin, player, total, count, false, false);
    }

    /** Sells the stack in the player's main hand (/sell hand). */
    public static void sellHand(BlossomEconomy plugin, Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            player.sendMessage(plugin.msg("hold-item"));
            return;
        }
        double each = plugin.getShop().getSellPrice(hand);
        if (each <= 0) {
            player.sendMessage(plugin.msg("worthless"));
            fail(player);
            return;
        }
        int count = hand.getAmount();
        player.getInventory().setItemInMainHand(null);
        finishSale(plugin, player, each * count, count, false, false);
    }

    private static void finishSale(BlossomEconomy plugin, Player player, double total, int count,
                                   boolean returned, boolean silentIfEmpty) {
        total = Math.round(total * 100.0) / 100.0;
        if (count > 0 && total > 0) {
            plugin.getEconomy().deposit(player.getUniqueId(), total);
            player.sendMessage(plugin.msg("sold", "%count%", String.valueOf(count), "%amount%", Text.money(total)));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
            if (returned) {
                player.sendMessage(plugin.msg("returned"));
            }
            return;
        }
        if (returned) {
            player.sendMessage(plugin.msg("nothing-sold"));
            player.sendMessage(plugin.msg("returned"));
            fail(player);
        } else if (!silentIfEmpty) {
            player.sendMessage(plugin.msg("nothing-sold"));
            fail(player);
        }
    }

    // ------------------------------------------------------------------
    // Item helpers
    // ------------------------------------------------------------------

    private static void fill(BlossomEconomy plugin, Inventory inv, int from, int to) {
        Material filler = Material.matchMaterial(plugin.getConfig().getString("menus.filler", "PINK_STAINED_GLASS_PANE"));
        if (filler == null || !filler.isItem() || filler.isAir()) {
            filler = Material.PINK_STAINED_GLASS_PANE;
        }
        ItemStack glass = item(filler, " ", List.of());
        for (int i = from; i < to; i++) {
            inv.setItem(i, glass);
        }
    }

    private static ItemStack balanceItem(BlossomEconomy plugin, Player player) {
        double balance = plugin.getEconomy().getBalance(player.getUniqueId());
        return item(Material.SUNFLOWER, "&#FF69B4&lYour Balance", List.of(
                "&a" + Text.money(balance),
                "&8(" + Text.shortMoney(balance) + ")"));
    }

    @SuppressWarnings("deprecation")
    static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            List<String> coloured = new ArrayList<>();
            for (String line : lore) {
                coloured.add(Text.color(line));
            }
            meta.setLore(coloured);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    private static void fail(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
    }
}
