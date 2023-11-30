package me.ford.salarymanager.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import me.ford.salarymanager.Messages;
import me.ford.salarymanager.SalaryManager;
import me.ford.salarymanager.Scheduler;
import me.ford.salarymanager.TimeUtil;

public class SalarySetPeriodCommand implements TabExecutor {
    private final SalaryManager plugin;

    public SalarySetPeriodCommand(SalaryManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        long period = plugin.getSettings().getPeriod();
        try {
            period = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Messages.PROVIDE_PERIOD_NUMBER.get());
            return true;
        }
        long oldPeriod = plugin.getSettings().getPeriod();
        long newPeriod = period;
        plugin.getSettings().setNewPeriod(newPeriod);
        Scheduler.getInstance().schedule(Scheduler.getInstance().getTimeLeft());
        Map<String, String> map = new HashMap<String, String>();
        map.put("\\{oldperiod\\}", TimeUtil.timeParser(oldPeriod));
        map.put("\\{newperiod\\}", TimeUtil.timeParser(newPeriod));
        sender.sendMessage(Messages.SET_NEW_PERIOD.get(map));
        return true;
    }

}
