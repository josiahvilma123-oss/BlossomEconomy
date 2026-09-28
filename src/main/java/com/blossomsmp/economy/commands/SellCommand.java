package com.blossomsmp.economy.commands;

import com.blossomsmp.economy.BlossomEconomy;
import com.blossomsmp.economy.menus.Menus;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/** /sell - sells the item in your hand */
public class SellCommand implements TabExecutor {

    private final BlossomEconomy plugin;

    public SellCommand(BlossomEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("players-only"));
            return true;
        }
        if (!plugin.getConfig().getBoolean("allow-creative-selling", true)
                && player.getGameMode() == GameMode.CREATIVE) {
            player.sendMessage(plugin.msg("creative-sell-blocked"));
            return true;
        }
        Menus.sellHand(plugin, player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
