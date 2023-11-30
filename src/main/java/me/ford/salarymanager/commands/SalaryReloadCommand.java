package me.ford.salarymanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import me.ford.salarymanager.SalaryManager;
import me.ford.salarymanager.Scheduler;
import net.md_5.bungee.api.ChatColor;

public class SalaryReloadCommand implements TabExecutor {
    private final SalaryManager plugin;

    public SalaryReloadCommand(SalaryManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        long prevPeriod = plugin.getSettings().getPeriod();
        plugin.reload();
        if (prevPeriod != plugin.getSettings().getPeriod() || plugin.getSettings().rescheduleOnReload()) {
            Scheduler.getInstance().schedule(plugin.getSettings().getPeriod()); // schedule again (might have a new period)
        }
        sender.sendMessage(ChatColor.RED + "SalaryManager config reloaded!");
        return true;
    }

}
