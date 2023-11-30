package me.ford.salarymanager.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import me.ford.salarymanager.Messages;
import me.ford.salarymanager.SalaryHandler;
import me.ford.salarymanager.SalaryManager;
import me.ford.salarymanager.bank.BankAccount;
import me.ford.salarymanager.bank.NoBank;
import me.ford.salarymanager.bank.UserBank;

public class SalaryFromCommand implements TabExecutor {
    private final SalaryManager plugin;

    public SalaryFromCommand(SalaryManager plugin) {
        this.plugin = plugin;
    }

    private List<String> getVisiblePlayers(CommandSender sender, String lastWord) {
        Player senderPlayer = sender instanceof Player ? (Player) sender : null;

        ArrayList<String> matchedPlayers = new ArrayList<String>();
        for (Player player : sender.getServer().getOnlinePlayers()) {
            String name = player.getName();
            if ((senderPlayer == null || senderPlayer.canSee(player))
                    && StringUtil.startsWithIgnoreCase(name, lastWord)) {
                matchedPlayers.add(name);
            }
        }

        Collections.sort(matchedPlayers, String.CASE_INSENSITIVE_ORDER);
        return matchedPlayers;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            List<String> players = getVisiblePlayers(sender, args[0]);

            players.add("none");
            players.add("default");
            return StringUtil.copyPartialMatches(args[0], players, list);
        }
        return list;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        boolean isUUID = args[0].length() == 36 && args[0].replace("-", "").length() == 32;
        boolean isNone = args[0].equalsIgnoreCase("none") || args[0].equalsIgnoreCase("default");
        Map<String, String> map = new HashMap<>();
        if (isNone) {
            SalaryHandler.getInstance().setBank(NoBank.getInstance()); // NO BANK
            map.put("\\{player\\}", "SERVER");
        } else {

            OfflinePlayer target = null;
            if (!isUUID) {
                target = plugin.getServer().getPlayer(args[0]);
                if (target == null && plugin.getSettings().allowFindingOfflinePlayers()) {
                    target = plugin.getServer().getOfflinePlayer(args[0]);
                }
            } else {
                UUID uuid = UUID.fromString(args[0]);
                target = plugin.getServer().getOfflinePlayer(uuid);
            }
            if (target == null || !target.hasPlayedBefore()) {
                map.put("\\{player\\}", args[0]);
                sender.sendMessage(Messages.PLAYER_NOT_FOUND.get(map));
                return true;
            }
            UserBank bank = new UserBank(plugin.getEcon(), target);
            SalaryHandler.getInstance().setBank(bank);
            map.put("\\{player\\}", target.getName());
        }
        sender.sendMessage(Messages.SET_NEW_FROM.get(map));
        return true;
    }


}
