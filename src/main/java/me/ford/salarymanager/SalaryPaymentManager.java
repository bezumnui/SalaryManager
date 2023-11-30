package me.ford.salarymanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.ford.salarymanager.SalaryHandler.PaymentDescription;
import me.ford.salarymanager.SalaryHandler.SalaryReason;
import me.ford.salarymanager.SalaryHandler.SalaryType;
import me.ford.salarymanager.bank.BankAccount;
import me.ford.salarymanager.bank.UserBank;
import me.ford.salarymanager.logging.CurrentPayments;
import me.ford.salarymanager.logging.PaymentsLogger;

public final class SalaryPaymentManager {
    private static final int OFFLINE_LIMIT = 100;
    private static final SalaryPaymentManager INSTANCE = new SalaryPaymentManager(
            JavaPlugin.getPlugin(SalaryManager.class));

    public static SalaryPaymentManager getInstance() {
        return INSTANCE;
    }

    private final SalaryManager plugin;

    private SalaryPaymentManager(SalaryManager plugin) {
        this.plugin = plugin;
    }

    private CurTotals payOnline(SalaryType type, Map<String, String> map, CurrentPayments payments) {
        SalaryHandler salaries = SalaryHandler.getInstance();
        double totalSum = 0.;
        int count = 0;
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            PaymentDescription pmnt = salaries.getPayableTo(onlinePlayer, type);
            double amount = pmnt.getAmount();
            SalaryReason reason = pmnt.getReason();
            boolean allowNeg = plugin.getSettings().allowNegativeSalaries();
            if (allowNeg || amount > 0) {
                OnSalaryEvent event = new OnSalaryEvent(onlinePlayer, amount);
                Bukkit.getPluginManager().callEvent(event);
                System.out.println("Salary");
                if (!event.isCancelled()) {
                    payPlayer(onlinePlayer, event.getAmount());
                    if (plugin.getSettings().sendMessage()) {
                        map.put("\\{amount\\}", Messages.DoubleFormat.format(event.getAmount()));
                        if (event.getShouldSendMessage()) {
                            onlinePlayer.sendMessage(Messages.GOT_SALARY.get(map));
                        }
                    }
                    if (plugin.getSettings().logPayments()) {
                        payments.log(onlinePlayer, event.getAmount(), reason);
                        if (reason == SalaryReason.ALL) {
                            reason.getGroups().clear();
                        }
                    }
                    count++;
                    totalSum += event.getAmount();
                }
            }
        }
        return new CurTotals(totalSum, count);
    }

    private void payPlayer(OfflinePlayer player, double amount) {
        if (amount > 0) {
            plugin.getEcon().depositPlayer(player, amount);
        } else {
            if (plugin.getSettings().allowNegativeSalaries()) {
                plugin.getEcon().withdrawPlayer(player, -amount);
            } else {
                throw new IllegalStateException("Negative salaries are not allowed right now");
            }
        }
        incrementPayments(player);
    }

    private void incrementPayments(OfflinePlayer player) {
        PaymentsLogger logger = plugin.getPaymentsLogger();
        if (logger == null) {
            return;
        }
        logger.incrementPayments(player);
    }

    public boolean canGetPaidAgain(OfflinePlayer player) {
        PaymentsLogger logger = plugin.getPaymentsLogger();
        return logger == null ? true : logger.canGetPaidAgain(player);
    }

    private CompletableFuture<CurTotals> payOfflineSalaries(CurrentPayments payments, SalaryHandler salaries) {
        return payOfflineSalaries(payments, new ArrayList<>(salaries.getOfflineSalaries().entrySet()), 0,
                OFFLINE_LIMIT);
    }

    private CompletableFuture<CurTotals> payOfflineSalaries(CurrentPayments payments,
            List<Map.Entry<UUID, Double>> entries, int counter, int limit) {
        SalaryHandler salaries = SalaryHandler.getInstance();
        double totalSum = 0.;
        int count = 0;
        int size = entries.size();
        CompletableFuture<CurTotals> future = new CompletableFuture<>();
        for (int i = counter; i < counter + limit && i < size; i++) {
            Map.Entry<UUID, Double> entry = entries.get(i);
            UUID id = entry.getKey();
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(id);
            PaymentDescription pmnt = salaries.getPayableToOffline(player, entry);
            if (pmnt == PaymentDescription.NULL_PAYMENT || pmnt.getAmount() <= 0) {
                continue;
            }
            double amount = pmnt.getAmount();
            totalSum += amount;
            count++;
            if (plugin.getSettings().logPayments()) {
                payments.log(player, amount, SalaryReason.OFFLINE);
            }
            if (count == limit && i < counter + limit) {// schedule next
                double sum = totalSum;
                long lastCount = count;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    payOfflineSalaries(payments, entries, counter + limit, limit).whenComplete((cur, e) -> {
                        cur.total += sum;
                        cur.count += lastCount;
                        future.complete(cur);
                    });
                });
                return future;
            }
        }
        double sum = totalSum;
        long lastCount = count;
        plugin.getServer().getScheduler().runTask(plugin, () -> future.complete(new CurTotals(sum, lastCount)));
        return future;
    }

    public void pay(boolean saveLoggers) {
        Map<String, String> map = new HashMap<String, String>();
        CurrentPayments payments;
        SalaryType type = SalaryType.getType(plugin);
        if (plugin.getSettings().logPayments()) {
            payments = plugin.getPaymentsLogger().startLog();
        } else {
            payments = null;
        }
        CurTotals totals = payOnline(type, map, payments);
        double totalSum = totals.total;
        long count = totals.count;
        SalaryReportPaymentsEvent event = new SalaryReportPaymentsEvent();
        Bukkit.getPluginManager().callEvent(event);

        payOfflineSalaries(payments, SalaryHandler.getInstance()).whenComplete((tots, err) -> {
            double ftotalSum = totalSum + tots.total;
            long fcount = count + tots.count;
            reportPayments(payments, map, ftotalSum, fcount, saveLoggers);
        });
    }

    private void reportPayments(CurrentPayments payments, Map<String, String> map, double totalSum, long count,
            boolean saveLoggers) {
        if (plugin.getSettings().logPayments()) {
            plugin.getPaymentsLogger().doneLog(payments, saveLoggers);
        }
        map.put("\\{total\\}", Messages.DoubleFormat.format(totalSum));
        map.put("\\{nr\\}", String.valueOf(count));
        if (plugin.getSettings().notifyStaff()) {
            plugin.getServer().broadcast(Messages.TOTAL_SALARIES.get(map), "salarymanager.notify");
        } else if (plugin.getSettings().logPaymentsToConsole()) {
            plugin.getLogger().info(Messages.TOTAL_SALARIES.get(map));
        }
        SalaryHandler salaries = SalaryHandler.getInstance();
        BankAccount bank = salaries.getBank();
        bank.removeMoney(totalSum);

        if (bank instanceof UserBank) {
            OfflinePlayer player = ((UserBank) bank).getPlayer();
            if (player.isOnline()) {

                player.getPlayer().sendMessage(Messages.TAKEN_FROM.get(map));
            }
        }
    }

    private class CurTotals {
        private double total;
        private long count;

        private CurTotals(double total, long count) {
            this.total = total;
            this.count = count;
        }

    }

}
