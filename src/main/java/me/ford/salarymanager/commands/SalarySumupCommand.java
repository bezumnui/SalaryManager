package me.ford.salarymanager.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.StringUtil;

import me.ford.salarymanager.Messages;
import me.ford.salarymanager.SalaryManager;
import me.ford.salarymanager.logging.PerDayLogger;
import net.md_5.bungee.api.ChatColor;

public class SalarySumupCommand implements TabExecutor {
    private final List<String> availableFileNames = new ArrayList<>();
    private final SalaryManager plugin;

    public SalarySumupCommand(SalaryManager plugin) {
        this.plugin = plugin;
        // check every 20 minutes
        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> findAvailableFileNames(), 0L, 20 * 60 * 20L);
    }

    private void findAvailableFileNames() {
        availableFileNames.clear();
        for (File file : plugin.getDataFolder().listFiles()) {
            String fileName = file.getName();
            if (fileName.startsWith(PerDayLogger.FILE_NAME_START) && fileName.endsWith(".yml")) {
                availableFileNames.add(fileName);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], availableFileNames, list);
        }
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], Arrays.asList("-g", "-p"), list);
        }
        return list;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        } // ignore whether or not we're doing daily logs - we might have been doing them
          // at some point
        String fileName = args[0];
        File file = new File(plugin.getDataFolder(), fileName);
        Map<String, String> map = new HashMap<>();
        map.put("\\{file\\}", fileName);
        if (!file.exists()) {
            sender.sendMessage(Messages.NO_SUCH_FILE.get(map));
            return true;
        }
        boolean showGroups = true;
        boolean showPlayers = true;
        if (args.length > 1) {
            if (args[1].equals("-p")) {
                showPlayers = true;
                showGroups = false;
            } else if (args[1].equals("-g")) {
                showPlayers = false;
                showGroups = true;
            }
        }
        // TOOD - move parsing elsewhere (SRP)
        parseAndShowResults(sender, file, showGroups, showPlayers);
        return true;
    }

    private void parseAndShowResults(CommandSender sender, File file, boolean showGroups, boolean showPlayers) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("problem loading history file:");
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Problem loading file! Refer to console for details!");
            return;
        }
        ParseResults globalResults = new ParseResults();
        for (String timestamp : config.getKeys(false)) {
            ConfigurationSection timeSection = config.getConfigurationSection(timestamp);
            if (timeSection == null) {
                plugin.getLogger()
                        .warning("Misconfigured history file:" + file.getName() + " at timestampe:" + timestamp);
                continue;
            }
            ParseResults results = parseTimeStamp(timeSection);
            addFromToUsers(results.perPlayerAmounts, globalResults);
            addFromToGroups(results.perGroupAmounts, globalResults);
        }
        Map<String, String> header = new HashMap<>();
        String day = file.getName().replace(PerDayLogger.FILE_NAME_START, "").replace(".yml", "").replace("_", "/");
        header.put("\\{day\\}", day);
        String options;
        if (showGroups && showPlayers) {
            options = "Players + Groups";
        } else if (showGroups) {
            options = "Groups";
        } else if (showPlayers) {
            options = "Players";
        } else {
            throw new IllegalStateException("Weird situation where not showing players nor groups"); // shouldn't happen
        }
        header.put("\\{options\\}", options);
        sender.sendMessage(Messages.SUMUP_HEADER.get(header));
        int shownLines = 0;
        if (showGroups) {
            for (Entry<String, Double> entry : globalResults.perGroupAmounts.entrySet()) {
                String group = entry.getKey();
                double total = entry.getValue();
                Map<String, String> map = new HashMap<>();
                map.put("\\{group\\}", group);
                map.put("\\{amount\\}", String.format("%3.2f", total));
                sender.sendMessage(Messages.GROUP_DAILY_TOTAL.get(map));
                shownLines++;
            }
        }
        if (showPlayers) {
            for (Entry<UUID, Double> entry : globalResults.perPlayerAmounts.entrySet()) {
                OfflinePlayer player = plugin.getServer().getOfflinePlayer(entry.getKey());
                double total = entry.getValue();
                Map<String, String> map = new HashMap<>();
                map.put("\\{player\\}", player.getName());
                map.put("\\{amount\\}", String.format("%3.2f", total));
                sender.sendMessage(Messages.PLAYER_DAILY_TOTAL.get(map));
                shownLines++;
            }
        }
        if (shownLines == 0) {
            sender.sendMessage(Messages.NOTHING_TO_SHOW.get());
        }
    }

    private ParseResults parseTimeStamp(ConfigurationSection section) {
        ParseResults timeStampResults = new ParseResults();
        for (String id : section.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unable to parse UUID '" + id + "' in section:" + section.getCurrentPath());
                continue;
            }
            ConfigurationSection playerSection = section.getConfigurationSection(id);
            if (playerSection == null) {
                plugin.getLogger().warning("Misconfigured history file at:" + section.getCurrentPath());
                continue;
            }
            ParseResults perPlayerResults = parseIndividual(uuid, playerSection);
            addFromToUsers(perPlayerResults.perPlayerAmounts, timeStampResults);
            addFromToGroups(perPlayerResults.perGroupAmounts, timeStampResults);
        }
        return timeStampResults;
    }

    private ParseResults parseIndividual(UUID id, ConfigurationSection section) {
        ParseResults results = new ParseResults();
        double playerAmount = section.getDouble("payment-amount", 0.0D);
        results.perPlayerAmounts.put(id, playerAmount);
        ConfigurationSection reasons = section.getConfigurationSection("reasons");
        if (reasons != null) {
            for (String group : reasons.getKeys(false)) {
                double perGroup = reasons.getDouble(group, 0.0D);
                results.addForGroup(group, perGroup);
            }
        }
        if (section.getString("payment-reason", "N/A").equals("GROUP")) {
            String group = section.getString("group", "N/A");
            results.addForGroup(group, playerAmount);
        }
        return results;
    }

    private void addFromToUsers(Map<UUID, Double> from, ParseResults to) {
        for (Entry<UUID, Double> entry : from.entrySet()) {
            UUID group = entry.getKey();
            double amount = entry.getValue();
            to.addForUUID(group, amount);
        }
    }

    private void addFromToGroups(Map<String, Double> from, ParseResults to) {
        for (Entry<String, Double> entry : from.entrySet()) {
            String group = entry.getKey();
            double amount = entry.getValue();
            to.addForGroup(group, amount);
        }
    }

    private class ParseResults {
        private final Map<UUID, Double> perPlayerAmounts = new HashMap<>();
        private final Map<String, Double> perGroupAmounts = new HashMap<>();

        private void addForUUID(UUID id, double amount) {
            perPlayerAmounts.compute(id, (key, prev) -> prev == null ? amount : prev + amount);
        }

        private void addForGroup(String group, double amount) {
            perGroupAmounts.compute(group, (key, prev) -> prev == null ? amount : prev + amount);
        }
    }

}
