package me.ford.salarymanager.logging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.ford.salarymanager.SalaryManager;

public class PerDayLogger {
    public static final String FILE_NAME_START = "per_day_history_";
    private final SalaryManager SM;
    private final PerDayPlayerTotals perPlayerTotals;
    private String curFileName;
    private File file;
    private FileConfiguration config;
    private int sub = 0; // part of the day in case the size gets large

    public PerDayLogger(SalaryManager plugin) {
        SM = plugin;
        perPlayerTotals = new PerDayPlayerTotals(plugin, this);
        SM.getServer().getPluginManager().registerEvents(perPlayerTotals, SM);
    }

    private void checkFile() {
        String name = getTodaysFileName(sub);
        if (curFileName == null || !curFileName.equals(name)) {
            if (changedDay(curFileName, name)) {
                sub = 0; // start from 0 in new day
            } else {
                SM.getLogger().info("Moving to new per day file since the old one got too big.");
            }
            curFileName = getTodaysFileName(sub);
            file = new File(SM.getDataFolder(), curFileName);
            config = new YamlConfiguration();
            if (file.exists()) {
                if (sub > 0 && file.length() > PaymentsLogger.MAX_FILE_SIZE) {
                    checkFile();
                    return;
                }
                try {
                    config.load(file);
                } catch (IOException | InvalidConfigurationException e) {
                    SM.getLogger().warning("Problem loading new file from config: " + curFileName);
                    e.printStackTrace();
                }
            }
            perPlayerTotals.resetDay();
        } else { // within the same day
            if (file.length() > PaymentsLogger.MAX_FILE_SIZE) {
                sub++;
                checkFile(); // ubdate to new sub
            }
        }
    }

    public CurrentPayments startLog() {
        checkFile();
        ConfigurationSection section = config.createSection(String.valueOf(System.currentTimeMillis()));
        return new CurrentPayments(section);
    }

    private int getNumberOfPaymentsInConfig(OfflinePlayer player, FileConfiguration config) {
        String uuidStr = player.getUniqueId().toString();
        int count = 0;
        for (String date : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(date);
            if (section.contains(uuidStr)) {
                count++;
            }
        }
        return count;
    }

    int getNumberOfPaymentsToday(OfflinePlayer player) {
        checkFile();
        int count = getNumberOfPaymentsInConfig(player, config);
        // older ones for the same date
        if (sub > 0) {
            int curSub = sub - 1;
            while (curSub >= 0) {
                String name = getTodaysFileName(curSub);
                File file = new File(SM.getDataFolder(), name);
                FileConfiguration config = new YamlConfiguration();
                if (file.exists()) {
                    try {
                        config.load(file);
                    } catch (IOException | InvalidConfigurationException e) {
                        SM.getLogger().warning("Problem loading file from config: " + name);
                        e.printStackTrace();
                    }
                    count += getNumberOfPaymentsInConfig(player, config);
                } else {
                    SM.getLogger().warning("Old config file " + name
                            + " does not exist while checking for number of payments during the day");
                }
                curSub--;
            }
        }
        return count;
    }

    public boolean canGetPaidAgain(OfflinePlayer player) {
        int maxPayments = SM.getSettings().getMaxDailyPayments();
        if (maxPayments < 0) { // functionality disabled
            return true;
        }
        int cur = getPaymentsToday(player);
        return cur < maxPayments;
    }

    public int getPaymentsToday(OfflinePlayer player) {
        return perPlayerTotals.getNumberOfPaymentsToday(player);
    }

    public void incrementPayments(OfflinePlayer player) {
        perPlayerTotals.increment(player);
    }

    public void save() {
        final File file = this.file;
        FileConfiguration config = this.config;
        SM.getServer().getScheduler().runTaskAsynchronously(SM, () -> saveInAsync(config, file));
    }

    private void saveInAsync(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            SM.getLogger().warning("Unable to save history!");
        }
    }

    private static final Pattern DATE_PATTERN = Pattern.compile(".*(\\d{4})_(\\d{2})_(\\d{2}).*");

    private static boolean changedDay(String prev, String cur) {
        if (prev == null) {
            return false; // but should have sub = 0 either way
        }
        Matcher m1 = DATE_PATTERN.matcher(prev);
        Matcher m2 = DATE_PATTERN.matcher(cur);
        if (!m1.matches() || !m2.matches()) {
            SalaryManager plugin = SalaryManager.getPlugin(SalaryManager.class);
            plugin.getLogger()
                    .severe("Checking files saved for the daily logger and they do not match the date format");
            plugin.getLogger().severe(prev + " -> " + cur);
            return false; // doesn't matter much either way
        } // different year, month or day
        return Integer.parseInt(m1.group(1)) != Integer.parseInt(m2.group(1))
                || Integer.parseInt(m1.group(2)) != Integer.parseInt(m2.group(2))
                || Integer.parseInt(m1.group(3)) != Integer.parseInt(m2.group(3));
    }

    private static String getTodaysFileName(int copy) {
        return FILE_NAME_START + new SimpleDateFormat("yyyy_MM_dd").format(new Date())
                + (copy == 0 ? "" : String.format("_%d", copy)) + ".yml";
    }

}
