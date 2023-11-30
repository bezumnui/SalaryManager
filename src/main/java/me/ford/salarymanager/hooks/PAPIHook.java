package me.ford.salarymanager.hooks;

import java.text.NumberFormat;
import java.util.Locale;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ford.salarymanager.SalaryHandler;
import me.ford.salarymanager.SalaryManager;
import me.ford.salarymanager.Scheduler;
import me.ford.salarymanager.TimeUtil;
import me.ford.salarymanager.SalaryHandler.PaymentDescription;
import me.ford.salarymanager.SalaryHandler.SalaryReason;
import me.ford.salarymanager.logging.PaymentsLogger;

public class PAPIHook extends PlaceholderExpansion {
    private static final String NO_SALARY_REASON = "N/A"; // TODO - confgiruable
    private static final String IDENTIFIER = "salarymanager";
    private final SalaryManager plugin;
    private final String k; // thousand - 1000
    private final String m; // million - 1 000 000
    private final String b; // billion - 1 000 000 000
    private final String t; // trillion - 1 000 000 000 000
    private final String q; // quadrillion - 1 000 000 000 000 000

    public PAPIHook(SalaryManager plugin) {
        this.plugin = plugin;
        register();
        this.k = "k"; // TODO - configurable
        this.m = "M"; // TODO - configurable
        this.b = "B"; // TODO - configurable
        this.t = "T"; // TODO - configurable
        this.q = "Q"; // TODO - configurable
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    private String format(double d) {
        NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return format.format(d);
    }

    private String fixMoney(double d) {
        if (d < 1000.0D) {
            return this.format(d);
        } else if (d < 1000000.0D) {
            return this.format(d / 1000.0D) + this.k;
        } else if (d < 1.0E9D) {
            return this.format(d / 1000000.0D) + this.m;
        } else if (d < 1.0E12D) {
            return this.format(d / 1.0E9D) + this.b;
        } else if (d < 1.0E15D) {
            return this.format(d / 1.0E12D) + this.t;
        } else {
            return d < 1.0E18D ? this.format(d / 1.0E15D) + this.q : String.valueOf((long) d);
        }
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        // %salarymanager_salary% - salary amount - normal
        // %salarymanager_salary_formatted% - salary amount - formatted
        // %salarymanager_salary_reason% - salary reason
        // %salarymanager_salary_time% - time till next payment
        if (identifier.startsWith("salary")) {
            PaymentDescription desc = SalaryHandler.getInstance().getPayableTo(player);
            if (identifier.equals("salary")) {
                return String.valueOf(desc.getAmount());
            } else if (identifier.equals("salary_formatted")) {
                return fixMoney(desc.getAmount());
            } else if (identifier.equals("salary_reason")) {
                SalaryReason reason = desc.getReason();
                return reason == null ? NO_SALARY_REASON : reason.name();
            } else if (identifier.equals("salary_time")) {
                return TimeUtil.timeParser(Scheduler.getInstance().getTimeLeft());
            }
        }
        // %salarymanager_salary_offline% - offline salary amount
        // %salarymanager_salary_offline_formatted% - offline salary amount - formatted
        // %salarymanager_salary_offline_reason% - offline salary reason
        if (identifier.startsWith("salary_offline")) {
            PaymentDescription desc = SalaryHandler.getInstance().getPayableToOffline(player);
            if (identifier.equals("salary_offline")) {
                return String.valueOf(desc.getAmount());
            } else if (identifier.equals("salary_offline_formatted")) {
                return fixMoney(desc.getAmount());
            } else if (identifier.equals("salary_offline_reason")) {
                SalaryReason reason = desc.getReason();
                return reason == null ? NO_SALARY_REASON : reason.name();
            }
        }

        // %salarymanager_nr_salaries_paid_today% - number of payments made to the player today
        if (identifier.equals("nr_salaries_paid_today")) {
            PaymentsLogger logger = plugin.getPaymentsLogger();
            if (logger == null) {
                return "N/A";
            }
            int nr = logger.getNumberOfPaymentsToday(player);
            if (nr < 0) {
                return "N/A";
            }
            return String.valueOf(nr);
        }

        // We return null if an invalid placeholder (f.e. %someplugin_placeholder3%)
        // was provided
        return null;
    }

}
