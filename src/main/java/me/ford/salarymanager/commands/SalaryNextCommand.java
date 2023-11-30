package me.ford.salarymanager.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import me.ford.salarymanager.Messages;
import me.ford.salarymanager.Scheduler;
import me.ford.salarymanager.TimeUtil;

public class SalaryNextCommand implements TabExecutor {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("\\{time\\}", TimeUtil.timeParser(Scheduler.getInstance().getTimeLeft()));
        sender.sendMessage(Messages.TIME_LEFT.get(map));
        return true;
    }

}
