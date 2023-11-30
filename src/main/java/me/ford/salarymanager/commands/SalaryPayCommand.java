package me.ford.salarymanager.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import me.ford.salarymanager.SalaryPaymentManager;

public class SalaryPayCommand implements TabExecutor {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int nr = 1;
        if (args.length > 0) {
            try {
                nr = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // defaults to 1
            }
        }
        for (int i = 0; i < nr; i++) {
            SalaryPaymentManager.getInstance().pay(i == nr - 1);
        }
        return true;
    }

}