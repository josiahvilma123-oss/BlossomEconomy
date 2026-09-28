package com.blossomsmp.economy.commands;

import com.blossomsmp.economy.BlossomEconomy;
import com.blossomsmp.economy.menus.Menus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/** /sell (menu), /sell hand, /sell all */
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
        if (args.length == 0) {
            Menus.openSell(plugin, player);
        } else if (args[0].equalsIgnoreCase("hand")) {
            Menus.sellHand(plugin, player);
        } else if (args[0].equalsIgnoreCase("all")) {
            Menus.sellAll(plugin, player);
        } else {
            player.sendMessage(plugin.msg("sell-usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? CommandUtil.filter(List.of("hand", "all"), args[0]) : List.of();
    }
}
