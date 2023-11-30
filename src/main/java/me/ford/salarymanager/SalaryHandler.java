package me.ford.salarymanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.ford.salarymanager.bank.BankAccount;
import me.ford.salarymanager.bank.NoBank;
import me.ford.salarymanager.bank.UserBank;

public class SalaryHandler {
    private final static String groupsalary = "groups";
    private final static String usersalary = "users";
    private final static String offlinesalary = "offline";
    private static SalaryHandler instance = null;

    public static SalaryHandler getInstance() {
        if (instance == null) {
            instance = new SalaryHandler();
        }
        return instance;
    }

    private SalaryHandler() {
        init(JavaPlugin.getPlugin(SalaryManager.class));
    }

    private Map<String, Double> groupSalaries = new HashMap<String, Double>();
    private Map<UUID, Double> userSalaries = new HashMap<UUID, Double>();
    private Map<UUID, Double> offlineSalaries = new HashMap<UUID, Double>();
    private BankAccount bank = NoBank.getInstance();
    private SalaryManager SM;
    private final String salaryFileName = "salaries.yml";
    private File salaryFile;
    private FileConfiguration config;

    public void init(SalaryManager plugin) {
        groupSalaries.clear();
        userSalaries.clear();
        offlineSalaries.clear();
        SM = plugin;
        salaryFile = new File(SM.getDataFolder(), salaryFileName);
        config = YamlConfiguration.loadConfiguration(salaryFile);
        if (config.contains("from")) {
            String from = config.getString("from");
            UUID id;
            try {
                id = UUID.fromString(from);
                bank = new UserBank(SM.getEcon(), id);
            } catch (IllegalArgumentException e) {
                SM.getLogger().warning("Unable to parse owner UUID:" + from);
            }
        }

        if (SM.getConfig().contains("salary")) {
            SM.getLogger().info(
                    "Migrating salaries from config.yml to salaries.yml and creating a backup of config.yml (just in case something goes missing in the migrating)");
            backupConfig();
            migrateSalaries(SM.getConfig().getConfigurationSection("salary"), config);
        }
        if (config.isConfigurationSection(groupsalary)) {
            ConfigurationSection groupSection = config.getConfigurationSection(groupsalary);
            for (String groupname : groupSection.getKeys(false)) {
                groupSalaries.put(groupname, groupSection.getDouble(groupname));
            }
        }
        if (config.isConfigurationSection(usersalary)) {
            populateUUIDSalaries(usersalary, userSalaries);
        }
        if (config.isConfigurationSection(offlinesalary)) {
            populateUUIDSalaries(offlinesalary, offlineSalaries);
        }
    }

    private void populateUUIDSalaries(final String path, Map<UUID, Double> map) {
        ConfigurationSection userSection = config.getConfigurationSection(path);
        for (String userID : userSection.getKeys(false)) {
            UUID id = UUID.fromString(userID);
            OfflinePlayer oplayer = SM.getServer().getOfflinePlayer(id);
            if (oplayer == null) {
                SM.LOGGER.warning(
                        "Could not load from config player with UUID:" + userID + " (while loading '" + path + "')");
            } else {
                map.put(id, userSection.getDouble(userID));
            }
        }
    }

    public void save() {
        try {
            config.save(salaryFile);
        } catch (IOException e) {
            SM.getLogger().warning("Unable to save Salaries file: " + salaryFile);
        }
    }

    public void setBank(BankAccount bank) {
        if (bank == null) {
            bank = NoBank.getInstance();
        }
        this.bank = bank;
        config.set("from", bank.asSaveableString());
        save();
    }

    public BankAccount getBank() {
        return bank;
    }

    public Map<String, Double> getGroupSalaries() {
        return groupSalaries;
    }

    public Map<UUID, Double> getUserSalaries() {
        return userSalaries;
    }

    public Map<UUID, Double> getOfflineSalaries() {
        return offlineSalaries;
    }

    public void setUserSalary(OfflinePlayer player, double salary) {
        setUserSalary(player.getUniqueId(), salary);
    }

    public void setUserSalary(UUID playerID, double salary) {
        userSalaries.put(playerID, salary);
        saveUsers();
    }

    public void setOfflineSalary(OfflinePlayer player, double salary) {
        setOfflineSalary(player.getUniqueId(), salary);
    }

    public void setOfflineSalary(UUID playerID, double salary) {
        offlineSalaries.put(playerID, salary);
        saveOffline();
    }

    public void setGroupSalary(String groupname, double salary) {
        groupSalaries.put(groupname, salary);
        saveGroups();
    }

    public PaymentDescription getPayableTo(Player onlinePlayer) {
        return getPayableTo(onlinePlayer, SalaryType.getType(SM));
    }

    public PaymentDescription getPayableTo(Player onlinePlayer, SalaryType type) {
        SalaryReason reason = SalaryReason.GROUP;
        if (type == SalaryType.SUM_ALL) {
            reason = SalaryReason.ALL;
        }
        if (!onlinePlayer.hasPermission("salarymanager.get")) {
            return PaymentDescription.NULL_PAYMENT; // not for those with no permissions
        }
        if (SM.getSettings().ignoreAFK() && SM.hasEssentials() && SM.getEssentials().getUser(onlinePlayer).isAfk()) {
            return PaymentDescription.NULL_PAYMENT; // not AFK
        }
        if (SM.getSettings().isWorldIgnored(onlinePlayer.getWorld())) {
            return PaymentDescription.NULL_PAYMENT; // world ignored -> don't pay
        }
        boolean canBePaid = SalaryPaymentManager.getInstance().canGetPaidAgain(onlinePlayer);
        if (!canBePaid) {
            return PaymentDescription.NULL_PAYMENT; // too many payments for this player
        }
        double sum = 0.;
        double max = 0.;
        String[] playerGroups = null;
        try {
            playerGroups = SM.getPerms().getPlayerGroups(onlinePlayer);
        } catch (UnsupportedOperationException e) {
            SM.LOGGER.warning("No permissions plugin detected!");
        }
        if (playerGroups != null) {
            for (String groupname : playerGroups) {
                double cur = 0.;
                if (groupSalaries.containsKey(groupname)) {
                    cur = groupSalaries.get(groupname);
                    sum += cur;
                    if (type == SalaryType.SUM_ALL) {
                        reason.addGroup(groupname, cur);
                    }
                    if (cur > max) {
                        if (type != SalaryType.SUM_ALL) {
                            reason.setGroup(groupname);
                        }
                        max = cur;
                    }
                }
            }
        }
        if (userSalaries.containsKey(onlinePlayer.getUniqueId())) {
            double cur = userSalaries.get(onlinePlayer.getUniqueId());
            sum += cur;
            if (type == SalaryType.SUM_ALL) {
                reason.addGroup("PLAYER-" + onlinePlayer.getUniqueId(), cur);
            }
            if (cur > max) {
                reason = SalaryReason.USER;
                max = cur;
            }
            if (type == SalaryType.USER_SALARY) {
                max = cur; // hax
            }
        }
        double amount = 0.;
        if (type == SalaryType.SUM_ALL) {
            amount = sum;
        } else if (type == SalaryType.MOST_PROFITABLE) {
            amount = max;
        } else if (type == SalaryType.USER_SALARY) {
            amount = max; // this is a sort of hack
        } else {
            // nothing for now
        }
        return new PaymentDescription(amount, reason);
    }

    public PaymentDescription getPayableToOffline(OfflinePlayer player) {
        return getPayableToOffline(player, null);
    }

    public PaymentDescription getPayableToOffline(OfflinePlayer player, Entry<UUID, Double> expected) {
        if (!offlineSalaries.containsKey(player.getUniqueId())) {
            return PaymentDescription.NULL_PAYMENT; // no offline payments for you
        }
        if (SM.getSettings().getOfflineOnlyOffline() && player.isOnline()) {
            return PaymentDescription.NULL_PAYMENT; // skip onlines if requested
        }
        boolean canBePaid = SalaryPaymentManager.getInstance().canGetPaidAgain(player);
        if (!canBePaid) {
            return PaymentDescription.NULL_PAYMENT; // too many payments for this player
        }
        UUID id = player.getUniqueId();
        if (expected == null) { // regular query
            return new PaymentDescription(offlineSalaries.get(id), SalaryReason.OFFLINE);
        }
        // query that could be done later where the expected value is already known
        // this will generally pay out what the expected value was at the time at which
        // the payment was scheduled. If the amount has changed in the meantime, an
        // error will be shown.
        double exp = expected.getValue();
        Double current = offlineSalaries.get(id);
        if (current == null) {
            SM.getLogger()
                    .warning("Offline salary of player " + player.getName()
                            + " removed while they were supposed to be paid " + exp
                            + ". Paying according to what was expected before.");
        } else if (current != exp) {
            SM.getLogger()
                    .warning("Offline salary of player " + player.getName()
                            + String.format(" was changed from %.2f to %.2f ", exp, current)
                            + "while they were supposed to be paid. Paying according to what was expected before");
        } // otherwise expectation and reality are equal
        return new PaymentDescription(current, SalaryReason.OFFLINE);
    }

    private void saveUsers() {
        updateUUIDSection(usersalary, userSalaries);
        save();
    }

    private void saveOffline() {
        updateUUIDSection(offlinesalary, offlineSalaries);
        save();
    }

    private void updateUUIDSection(final String path, Map<UUID, Double> map) {
        ConfigurationSection userSection = config.getConfigurationSection(path);
        if (userSection == null) {
            userSection = config.createSection(path);
        }
        for (Entry<UUID, Double> entry : map.entrySet()) {
            userSection.set(entry.getKey().toString(), entry.getValue());
        }
    }

    private void saveGroups() {
        ConfigurationSection groupSection = config.getConfigurationSection(groupsalary);
        if (groupSection == null) {
            groupSection = config.createSection(groupsalary);
        }
        for (Entry<String, Double> entry : groupSalaries.entrySet()) {
            groupSection.set(entry.getKey(), entry.getValue());
        }
        save();
    }

    private void migrateSalaries(ConfigurationSection from, ConfigurationSection to) {
        for (String path : from.getKeys(true)) {
            to.set(path, from.get(path));
        }
        SM.getConfig().set("salary", null); // remove from old
        SM.saveConfig();
        save();
    }

    private void backupConfig() {
        File toFile = new File(SM.getDataFolder(), "config_backup.yml");
        if (toFile.exists()) {
            toFile = new File(SM.getDataFolder(), "config_backup_" + System.currentTimeMillis() / 1000L + ".yml");
        }
        try {
            Files.copy(new File(SM.getDataFolder(), "config.yml").toPath(), toFile.toPath());
        } catch (IOException e1) {
            SM.getLogger().warning("Unable to create backup of config.yml");
        }
    }

    public static enum SalaryType {
        MOST_PROFITABLE, USER_SALARY, SUM_ALL;

        public static SalaryType getType(SalaryManager SM) {
            Settings settings = SM.getSettings();
            if (settings.getAll() && !settings.getMostProfitable() && !settings.onlyUserSalary()) {
                return SalaryType.SUM_ALL;
            } else if (settings.getMostProfitable() && !settings.getAll() && !settings.onlyUserSalary()) {
                return SalaryType.MOST_PROFITABLE;
            } else if (settings.onlyUserSalary() && !settings.getAll() && !settings.getMostProfitable()) {
                return SalaryType.USER_SALARY;
            } else { // multiple options selected
                String msg = "Found multiple types of Salary selected(";
                msg += (settings.getMostProfitable() ? "most profitable, " : "") + (settings.getAll() ? "sum, " : "")
                        + (settings.onlyUserSalary() ? "UserSalary" : "");
                msg += "), falling back to ";
                if (settings.getMostProfitable()) {
                    SM.getLogger().warning(msg + "most profitable");
                    return SalaryType.MOST_PROFITABLE;
                } else if (settings.onlyUserSalary()) {
                    SM.getLogger().warning(msg + "user salaries");
                    return SalaryType.USER_SALARY;
                } else if (settings.onlyUserSalary()) {
                    SM.getLogger().warning(msg + "sum");
                    return SalaryType.SUM_ALL;
                } else { // default
                    SM.getLogger().warning(msg + "most profitable(DEFAULT)");
                    return SalaryType.MOST_PROFITABLE;
                }
            }
        }
    }

    public static enum SalaryReason {
        GROUP("group"), USER("user"), ALL("all"), OFFLINE("offline");

        private final String reason;
        private String group = "N/A";
        private Map<String, Double> groups = new HashMap<String, Double>();

        private SalaryReason(String reason) {
            this.reason = reason;
        }

        public void setGroup(String group) {
            if (this == GROUP) {
                this.group = group;
            } else {
                throw new IllegalArgumentException("Not a GROUP instance. Can't set group.");
            }
        }

        public String getGroup() {
            if (this == GROUP) {
                return group;
            } else {
                throw new IllegalArgumentException("Not a GROUP instance. Can't get group.");
            }
        }

        public void addGroup(String group, double amount) {
            if (this == ALL) {
                groups.put(group, amount);
            } else {
                throw new IllegalArgumentException("Not an ALL instance. Can't add groups.");
            }
        }

        public Map<String, Double> getGroups() {
            if (this == ALL) {
                return groups;
            } else {
                throw new IllegalArgumentException("Not an ALL instance. Can't get groups.");
            }
        }

        public String getReason() {
            return reason;
        }
    }

    public static class PaymentDescription {
        public static final PaymentDescription NULL_PAYMENT = new PaymentDescription(0.0, null);
        private final double amount;
        private final SalaryReason reason;

        private PaymentDescription(double amount, SalaryReason reason) {
            this.reason = reason;
            this.amount = amount;
        }

        public double getAmount() {
            return amount;
        }

        public SalaryReason getReason() {
            return reason;
        }

    }

}
