package me.ford.salarymanager.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import me.ford.salarymanager.Messages;
import me.ford.salarymanager.SalaryHandler;
import me.ford.salarymanager.SalaryManager;

public class SalaryListCommand implements TabExecutor {
    private final SalaryManager plugin;

    public SalaryListCommand(SalaryManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("-p", "-g"), list);
        }
        return list;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        boolean players = false;
        if (args[0].equalsIgnoreCase("-p")) {
            players = true;
        } else if (args[0].equalsIgnoreCase("-g")) {
            players = false;
        } else {
            return false;
        }
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                // default to 1
            }
        }
        int i = 0;
        boolean found = false;
        Map<String, String> map = new HashMap<String, String>();
        map.put("\\{page\\}", String.valueOf(page));
        sender.sendMessage(Messages.LIST_START.get(map));
        if (players) {
            for (Entry<UUID, Double> entry : SalaryHandler.getInstance().getUserSalaries().entrySet()) {
                if (i >= (page - 1) * 6 && i < page * 6) { // 6 per page
                    map.put("\\{player\\}", plugin.getServer().getOfflinePlayer(entry.getKey()).getName());
                    map.put("\\{salary\\}", Messages.DoubleFormat.format(entry.getValue()));
                    sender.sendMessage(Messages.LIST_PLAYER_ITEM.get(map));
                    found = true;
                }
                i++;
            }
        } else { // default
            for (Entry<String, Double> entry : SalaryHandler.getInstance().getGroupSalaries().entrySet()) {
                if (i >= (page - 1) * 6 && i < page * 6) { // 6 per page
                    map.put("\\{group\\}", entry.getKey());
                    map.put("\\{salary\\}", Messages.DoubleFormat.format(entry.getValue()));
                    sender.sendMessage(Messages.LIST_GROUP_ITEM.get(map));
                    found = true;
                }
            }
        }
        if (!found) {
            sender.sendMessage(Messages.LIST_NONE_FOUND.get(map));
        }
        return true;
    }

}
