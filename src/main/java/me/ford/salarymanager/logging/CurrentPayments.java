package me.ford.salarymanager.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import me.ford.salarymanager.SalaryHandler.SalaryReason;

public class CurrentPayments {
    private final ConfigurationSection out;
    private List<CurrentPayments> otherLoggers = new ArrayList<>();

    public CurrentPayments(ConfigurationSection section) {
        out = section;
    }

    public void log(OfflinePlayer player, double amount, SalaryReason reason) {
        // players will be unique - only one call per player per cycle
        String idStr = player.getUniqueId().toString();
        ConfigurationSection playerSection;
        if (out.contains(idStr)) { // in case offline AND online salaries
            playerSection = out.createSection(idStr + "_2"); // TODO - configurable?
        } else {
            playerSection = out.createSection(idStr);
        }
        playerSection.set("player-name", player.getName());
        playerSection.set("payment-amount", amount);
        playerSection.set("payment-reason", reason.name());
        if (reason == SalaryReason.ALL) {
            for (Entry<String, Double> entry : reason.getGroups().entrySet()) {
                playerSection.set("reasons." + entry.getKey(), entry.getValue());
            }
        } else if (reason == SalaryReason.GROUP) {
            playerSection.set("group", reason.getGroup());
        }
        for (CurrentPayments other : otherLoggers) {
            other.log(player, amount, reason);
        }
    }

    void appendLogger(CurrentPayments logger) {
        otherLoggers.add(logger);
    }

}