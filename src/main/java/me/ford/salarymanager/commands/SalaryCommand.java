package me.ford.salarymanager.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import me.ford.salarymanager.Messages;
import me.ford.salarymanager.SalaryHandler;
import me.ford.salarymanager.SalaryManager;
import me.ford.salarymanager.SalaryManager.SalaryType;

public class SalaryCommand implements TabExecutor {
    private final SalaryManager plugin;

    public SalaryCommand(SalaryManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            List<String> originals = new ArrayList<>();
            originals.addAll(Arrays.asList(plugin.getPerms().getGroups()));
            Player playerSender = sender instanceof Player ? (Player) sender : null;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (playerSender != null && !playerSender.canSee(player)) {
                    continue;
                }
                originals.add(player.getName());
            }
            return StringUtil.copyPartialMatches(args[0], originals, list);
        }
        if (args.length == 3) {
            List<String> options = Arrays.asList("-g", "-p");
            return StringUtil.copyPartialMatches(args[2], options, list);
        }
        return list;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /salary <group or player> <amount> [-g or -p] [delay]
        if (args.length < 2) {
            return false;
        }
        SalaryType which = null;
        boolean canPlayer = false;
        boolean canGroup = false;
        String playername = args[0];
        OfflinePlayer player = plugin.getServer().getPlayer(playername);
        if (player == null && plugin.getSettings().allowFindingOfflinePlayers()) {
            player = plugin.getServer().getOfflinePlayer(playername); // should be only deprecated method
        }
        if (player != null && player.hasPlayedBefore()) {
            canPlayer = true;
        }
        String groupname = args[0];
        for (String group : plugin.getPerms().getGroups()) {
            if (groupname.equalsIgnoreCase(group)) {
                canGroup = true;
                groupname = group; // the original name
            }
        }
        if (canPlayer && canGroup) {
            if (args.length > 2) {
                if (args[2].equalsIgnoreCase("-g")) {
                    which = SalaryType.GROUP;
                } else if (args[2].equalsIgnoreCase("-p")) {
                    which = SalaryType.USER;
                } else if (args[2].equalsIgnoreCase("-o")) {
                    which = SalaryType.OFFLINE;
                }
            }
        } else if (canPlayer) {
            which = SalaryType.USER;
            if (args[args.length - 1].equalsIgnoreCase("-o")) {
                which = SalaryType.OFFLINE;
            }
        } else if (canGroup) {
            which = SalaryType.GROUP;
        }
        double salary;
        try {
            salary = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Messages.PROVIDE_SALARY.get());
            return true;
        }
        if (!plugin.getSettings().allowNegativeSalaries() && salary < 0) {
            sender.sendMessage(Messages.PROVIDE_SALARY.get());
            return true;
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put("\\{group\\}", groupname);
        map.put("\\{player\\}", playername);
        map.put("\\{amount\\}", Messages.DoubleFormat.format(salary));
        SalaryHandler salaries = SalaryHandler.getInstance();
        if (which == SalaryType.USER) {
            salaries.setUserSalary(player, salary);
            sender.sendMessage(Messages.SET_PLAYER_SALARY.get(map));
        } else if (which == SalaryType.GROUP) {
            salaries.setGroupSalary(groupname, salary);
            sender.sendMessage(Messages.SET_GROUP_SALARY.get(map));
        } else if (which == SalaryType.OFFLINE && sender.hasPermission("salarymanager.set.offline")) {
            salaries.setOfflineSalary(player, salary);
            sender.sendMessage(Messages.SET_OFFLINE_SALARY.get(map));
        } else {
            sender.sendMessage(Messages.NO_PLAYER_OR_GROUP.get(map));
        }
        return true;
    }

}
