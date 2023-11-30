package me.ford.salarymanager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.base.Charsets;

public enum Messages {
    PLAYER_NOT_FOUND("salary.player-not-found", "&cPlayer not found: &8{player}"),
    NO_PLAYER_OR_GROUP("salary.no-player-or-group", "&cNo player of group by the name &8{player}&c found!"),
    PROVIDE_SALARY("salary.provide-salary", "&cYou need to proivde a numerical salary!"),
    GOT_SALARY("salary.got", "&5You got a salary of &8{amount}"),
    LIST_START("salary.list-start", "&5Salaries on page &8{page}"),
    LIST_PLAYER_ITEM("salary.list-player-item", "&8{player}&5 has a salary of &7{salary}"),
    LIST_GROUP_ITEM("salary.list-group-item", "&6{group}&5 has a salary of &7{salary}"),
    LIST_NONE_FOUND("salary.list-none-found", "&cNo entries found on this page"),
    TOTAL_SALARIES("salary.total-salaries-log", "&6A total of &7{total}&6 was paid to &7{nr}&6 players"),
    PROVIDE_PERIOD_NUMBER("salary.provide_period_length", "&cYou need to provide a lengh (in seconds) for the period!"),
    TIME_LEFT("salary.time-until-next", "&6You've got &5{time}&6 until your next payday."),
    SET_NEW_PERIOD("salary.set-new-period",
            "&6You've set the new period as &5{newperiod}&6 (previously &7{oldperiod}&6)"),
    SET_PLAYER_SALARY("salary.set-player-salary", "&6Set &7{amount}&6 as the salary of &8{player}"),
    SET_GROUP_SALARY("salary.set-group-salary", "&6Set &7{amount}&6 as the salary of &9{group}"),
    SET_OFFLINE_SALARY("salary.set-offline-salary", "&6Set &7{amount}&6 as the &5offline&6 salary of &8{player}"),
    SET_NEW_FROM("salary.set-new-from", "&6Set new account to take money from: &7{player}"),
    TAKEN_FROM("salary.taken-from", "&6A total of &7{total}&6 was taken from you (the bank)"),
    SUMUP_HEADER("salary.sumup-header", "&6Total payments for {day} {options}"),
    NO_SUCH_FILE("salary.no-such-file", "&cNo such history file found: &7{file}"),
    GROUP_DAILY_TOTAL("salary.group-daily-total", "&6The group &7{group}&6 got a total of &8{amount}"),
    PLAYER_DAILY_TOTAL("salary.player-daily-total", "&6The player &7{player}&6 got a total of &8{amount}"),
    NOTHING_TO_SHOW("salary.nothing-to-show-daily", "&cThere were no entries for the day");

    private static FileConfiguration file;
    private static String filepath = "messages";// .yml";
    private final String path;
    private String message;
    private static SalaryManager SM;

    public static void init(SalaryManager plugin) {
        SM = plugin;
        if (SM.getSettings().getLanguage().equalsIgnoreCase("en")) {
            filepath = "messages.yml";
        } else {
            filepath = "messages_" + SM.getSettings().getLanguage() + ".yml";
        }
        File fileFile = new File(SM.getDataFolder(), filepath);
        file = YamlConfiguration.loadConfiguration(fileFile);
        TimeUtil.init();
        if (initDefaults()) {
            try {
                file.save(fileFile);
            } catch (IOException e) {
                SM.getLogger().warning("Unable to save " + filepath);
            }
        }
    }

    private static boolean initDefaults() {
        final InputStream defConfigStream = SM.getResource(filepath);
        if (defConfigStream == null) {
            return false;
        }
        file.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
        if (!file.getKeys(true).containsAll(file.getDefaults().getKeys(true))) {
            file.options().copyDefaults(true);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param path    path of message within file
     * @param message default message
     */
    private Messages(String path, String message) {
        this.path = path;
        this.message = message;
    }

    /**
     * @return coloured message
     */
    public String get() {
        if (file.contains(path)) {
            // message from file
            return c(file.getString(path));
        } else {
            // default message
            return c(message);
        }
    }

    /**
     * Get (coloured) message where a map is used to swap out {} tagged stuff
     * 
     * @param map map used to replace
     * @return the coloured message
     */
    public String get(Map<String, String> map) {
        String msg = message;
        if (file.contains(path)) {
            msg = file.getString(path);
        }
        for (Entry<String, String> entry : map.entrySet()) {
            msg = msg.replaceAll(entry.getKey(), entry.getValue());
        }
        return c(msg);
    }

    /**
     * Colours message
     * 
     * @param msg message to color
     * @return coloured message
     */
    public String c(String msg) {
        if (msg == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    protected static String getPath() {
        return filepath;
    }

    public static FileConfiguration getConfig() {
        return file;
    }

    public static class DoubleFormat {

        public static String format(final double value) {
            final NumberFormat nf;
            if (SM.getSettings().useSimplerMoneyFormat()) {
                nf = NumberFormat.getInstance(Locale.US);
            } else {
                nf = NumberFormat.getInstance();
            }
            nf.setMaximumFractionDigits(2);
            nf.setMinimumFractionDigits(2);
            return nf.format(value);
        }
    }

}
