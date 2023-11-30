package me.ford.salarymanager.logging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.ford.salarymanager.SalaryManager;

public class PaymentsLogger {
    static final long MAX_FILE_SIZE = 100 * 1024; // 100 kb -> roughly 700 user payments
    private final SalaryManager SM;
    private static final String fileName = "history.yml";
    private final File configFile;
    private FileConfiguration config;
    private PerDayLogger perDayLogger;

    public PaymentsLogger(SalaryManager plugin) {
        SM = plugin;
        configFile = new File(SM.getDataFolder(), fileName);
        if (configFile.exists() && configFile.length() > MAX_FILE_SIZE) {
            backupHistory();
        }
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                SM.getLogger().severe("Problem creating history file");
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        if (SM.getSettings().doDailyLogs()) {
            perDayLogger = new PerDayLogger(SM);
        }
    }

    private boolean currentlyRenaming = false;

    private void backupHistory() {
        String nameAppend = new SimpleDateFormat("yyyy_MM_dd HH-mm").format(new Date());
        // TODO - technically, the below could fail
        String backupFileName = "history_backup_" + nameAppend + ".yml";
        File backupFile = new File(SM.getDataFolder(), backupFileName);
        currentlyRenaming = true;
        SM.getServer().getScheduler().runTaskAsynchronously(SM, () -> renameInAsync(configFile, backupFile));
    }

    private void renameInAsync(File configFile, File backupFile) {
        boolean success = false;
        try {
            success = configFile.renameTo(backupFile);
        } finally {
            currentlyRenaming = false;
        }
        SM.getLogger().info("Moved history.yml to " + backupFile.getName() + " because it was getting too large.");
        if (!success) {
            SM.getLogger().warning("Problem renaming to backup file!");
        }
    }

    private void attemptBackup() {
        if (currentlyRenaming) {
            return; // working in async thread
        }
        if (configFile.length() > MAX_FILE_SIZE) {
            backupHistory();
            config = new YamlConfiguration(); // empty config - no need to "read" from file
        }
    }

    public CurrentPayments startLog() {
        attemptBackup();
        ConfigurationSection section = config.createSection(String.valueOf(System.currentTimeMillis()));
        CurrentPayments pmnts = new CurrentPayments(section);
        if (SM.getSettings().doDailyLogs()) {
            if (perDayLogger == null) { // if originally disabled, but reloaded and changed
                perDayLogger = new PerDayLogger(SM);
            }
            pmnts.appendLogger(perDayLogger.startLog());
        }
        return pmnts;
    }

    public void doneLog(CurrentPayments curPay, boolean saveLoggers) {
        final File file = this.configFile;
        FileConfiguration config = this.config;
        if (!saveLoggers) { // ignore if no need to save
            return; // this happens when using the command to pay multiple times in a row
        }
        SM.getServer().getScheduler().runTaskAsynchronously(SM, () -> saveInAsync(config, file));
        if (SM.getSettings().doDailyLogs()) {
            if (perDayLogger == null) {
                SM.getLogger()
                        .info("The plugin was reloaded between starting payments "
                                + "(when per day logging was disabled)"
                                + " and finishing payments (when per day logging was enabled) "
                                + " so there's nowhere to save right now");
            } else {
                perDayLogger.save();
            }
        }
    }

    private void saveInAsync(FileConfiguration config, File file) {
        try {
            config.save(configFile);
        } catch (IOException e) {
            SM.getLogger().warning("Unable to save history!");
        }
    }

    public int getNumberOfPaymentsToday(OfflinePlayer player) {
        if (perDayLogger == null) {
            return -1;
        }
        return perDayLogger.getNumberOfPaymentsToday(player);
    }

    public boolean canGetPaidAgain(OfflinePlayer player) {
        if (perDayLogger == null) {
            return true;
        }
        return perDayLogger.canGetPaidAgain(player);
    }

    public void incrementPayments(OfflinePlayer player) {
        if (perDayLogger == null) {
            return;
        }
        perDayLogger.incrementPayments(player);
    }

}
