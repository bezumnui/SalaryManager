package me.ford.salarymanager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.World;

public class Settings {
    private long defaultPeriod = 0L;
    private String language;
    private boolean getMostProfitable;
    private boolean getAll;
    private boolean sendMessage;
    private boolean ignoreAFK;
    private boolean logPayments;
    private boolean logPaymentsToConsole;
    private boolean notifyStaff;
    private boolean onlyUserSalary;
    private boolean offlineOnlyOffline;
    private boolean reScheduleOnReload;
    private boolean allowFindingOfflinePlayers;
    private boolean simplerMoneyFormat;
    private boolean doDailyLogs;
    private boolean useBstats;
    private int maxDailyPayments;
    private boolean allowNegativeSalaries;
    private final Set<World> ignoredWorlds = new HashSet<>();
    private final SalaryManager plugin;

    public Settings(SalaryManager plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        plugin.getConfig().options().copyDefaults(true);
        defaultPeriod = plugin.getConfig().getLong("default-delay");
        getMostProfitable = plugin.getConfig().getBoolean("get-most-profitable");
        getAll = plugin.getConfig().getBoolean("get-all");
        sendMessage = plugin.getConfig().getBoolean("message-player");
        ignoreAFK = plugin.getConfig().getBoolean("ignore-afk");
        logPayments = plugin.getConfig().getBoolean("log-payments");
        logPaymentsToConsole = plugin.getConfig().getBoolean("log-payments-to-console");
        notifyStaff = plugin.getConfig().getBoolean("notify-staff");
        onlyUserSalary = plugin.getConfig().getBoolean("get-player-salary-when-possible");
        reScheduleOnReload = plugin.getConfig().getBoolean("reschedule-on-reload");
        allowFindingOfflinePlayers = plugin.getConfig().getBoolean("allow-finding-offline-players");
        simplerMoneyFormat = plugin.getConfig().getBoolean("simpler-money-format", false);
        useBstats = plugin.getConfig().getBoolean("use-bstats", true);
        offlineOnlyOffline = !plugin.getConfig().getBoolean("offline-salaries-paid-when-online", true);
        doDailyLogs = plugin.getConfig().getBoolean("do-daily-logs", false);
        maxDailyPayments = plugin.getConfig().getInt("max-payments-per-day", -1);
        allowNegativeSalaries = plugin.getConfig().getBoolean("allow-negative-salaries", false);
        // Language
        language = plugin.getConfig().getString("use-language", "default");
        if (language.equalsIgnoreCase("default") || language.equalsIgnoreCase("en")
                || language.equalsIgnoreCase("english")) {
            language = "en";
        } else if (language.equalsIgnoreCase("de") || language.equalsIgnoreCase("deutsch")
                || language.equalsIgnoreCase("german")) {
            language = "de";
        } else {
            plugin.getLogger().info("No native support for language:" + language);
            plugin.getLogger().info("Defaulting to English. You can manually edit messages within messages.yml");
            language = "en";
        }
        // disabled worlds
        ignoredWorlds.clear();
        List<String> findLater = new ArrayList<>();
        for (String worldName : plugin.getConfig().getStringList("ignored-worlds")) {
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                findLater.add(worldName);
            } else {
                ignoredWorlds.add(world);
            }
        }
        if (!findLater.isEmpty()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (String worldName : findLater) {
                    World world = plugin.getServer().getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("Unable to find (ignored) world:" + worldName);
                        plugin.getLogger().info("The world was not loaded at server startup nor 5 ticks later. "
                                + "If (for some reason) this world gets loaded at a later date, "
                                + "please contact the developer with a feature request. "
                                + "With that said, using the /salaryreload command when the world "
                                + "is loaded should also work in most cases.");
                    } else {
                        ignoredWorlds.add(world);
                    }
                }
            }, 5L);
        }
    }

    public void reload() {
        load();
    }

    public boolean rescheduleOnReload() {
        return reScheduleOnReload;
    }

    public boolean allowFindingOfflinePlayers() {
        return allowFindingOfflinePlayers;
    }

    public boolean doDailyLogs() {
        return doDailyLogs;
    }

    public long getPeriod() {
        return defaultPeriod;
    }

    public void setNewPeriod(long period) {
        defaultPeriod = period;
        plugin.getConfig().set("default-delay", defaultPeriod);
        plugin.saveConfig();
    }

    public boolean ignoreAFK() {
        return ignoreAFK;
    }

    public boolean logPayments() {
        return logPayments;
    }

    public boolean logPaymentsToConsole() {
        return logPaymentsToConsole;
    }

    public boolean notifyStaff() {
        return notifyStaff;
    }

    public String getLanguage() {
        return language;
    }

    public boolean sendMessage() {
        return sendMessage;
    }

    public boolean getAll() {
        return getAll;
    }

    public boolean getMostProfitable() {
        return getMostProfitable;
    }

    public boolean useSimplerMoneyFormat() {
        return simplerMoneyFormat;
    }

    public boolean onlyUserSalary() {
        return onlyUserSalary;
    }

    public boolean getOfflineOnlyOffline() {
        return offlineOnlyOffline;
    }

    public int getMaxDailyPayments() {
        return maxDailyPayments;
    }

    public boolean useBStats() {
        return useBstats;
    }

    public boolean allowNegativeSalaries() {
        return allowNegativeSalaries;
    }

    public boolean isWorldIgnored(World world) {
        return ignoredWorlds.contains(world);
    }

}
