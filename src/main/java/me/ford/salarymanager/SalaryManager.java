package me.ford.salarymanager;

import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.ford.salarymanager.commands.SalaryCommand;
import me.ford.salarymanager.commands.SalaryFromCommand;
import me.ford.salarymanager.commands.SalaryListCommand;
import me.ford.salarymanager.commands.SalaryNextCommand;
import me.ford.salarymanager.commands.SalaryPayCommand;
import me.ford.salarymanager.commands.SalaryReloadCommand;
import me.ford.salarymanager.commands.SalarySetPeriodCommand;
import me.ford.salarymanager.commands.SalarySumupCommand;
import me.ford.salarymanager.hooks.PAPIHook;
import me.ford.salarymanager.logging.PaymentsLogger;

import org.bstats.bukkit.Metrics;

import com.earth2me.essentials.IEssentials;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

public class SalaryManager extends JavaPlugin {
    private Settings settings;
    Logger LOGGER = Logger.getLogger("SalaryManager");
    private Permission perms = null;
    private Economy econ = null;
    private IEssentials ess = null;
    private PaymentsLogger logger = null;

    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();
        if (!setupEconomy()) {
            LOGGER.severe("Disabled due to no Vault dependency found or no Economy provided!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        loadSoftDependencies();
        setupEssentials();
        setupPermissions();
        if (settings.logPayments()) {
            logger = new PaymentsLogger(this);
        }
        Scheduler.getInstance(); // initialize
        if (settings.useBStats()) {
            new Metrics(this);
        }
        getCommand("salary").setExecutor(new SalaryCommand(this));
        getCommand("salarypay").setExecutor(new SalaryPayCommand());
        getCommand("salaryreload").setExecutor(new SalaryReloadCommand(this));
        getCommand("salarylist").setExecutor(new SalaryListCommand(this));
        getCommand("salarysetperiod").setExecutor(new SalarySetPeriodCommand(this));
        getCommand("salarynext").setExecutor(new SalaryNextCommand());
        getCommand("salaryfrom").setExecutor(new SalaryFromCommand(this));
        getCommand("salarysumup").setExecutor(new SalarySumupCommand(this));
    }

    public void loadConfiguration() {
        settings = new Settings(this);
        Messages.init(this);
    }

    public void reload() {
        reloadConfig();
        settings.reload();
        Messages.init(this);
    }

    private void loadSoftDependencies() {
        Plugin papi = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled() && papi.getClass().getSimpleName().equals("PlaceholderAPIPlugin")) {
            getLogger().info("Registering PlaceholderAPI hook");
            new PAPIHook(this);
        }

    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    private boolean setupEssentials() {
        Plugin essPlugin = getServer().getPluginManager().getPlugin("Essentials");
        if (essPlugin != null) {
            ess = (IEssentials) essPlugin;
        } else {
            getLogger().info("Could not find Essentials. Unable to register AFK players");
        }
        return ess != null;
    }

    public Settings getSettings() {
        return settings;
    }

    public static enum SalaryType {
        GROUP, USER, OFFLINE;
    }

    public boolean hasEssentials() {
        return ess != null;
    }

    public IEssentials getEssentials() {
        return ess;
    }


    public Economy getEcon() {
        return econ;
    }

    public Permission getPerms() {
        return perms;
    }

    public PaymentsLogger getPaymentsLogger() {
        return logger;
    }

}
