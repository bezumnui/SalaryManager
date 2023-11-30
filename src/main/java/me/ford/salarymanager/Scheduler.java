package me.ford.salarymanager;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class Scheduler {

    private static Scheduler instance = null;
    private final SalaryManager SM;

    private Scheduler(SalaryManager plugin) {
        SM = plugin;
        schedule(SM.getSettings().getPeriod());
    }

    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler(JavaPlugin.getPlugin(SalaryManager.class));
        }
        return instance;
    }

    private BukkitTask task = null;
    private long starttime;
    private long period;
    private final Runnable runnable = new Runnable() {

        @Override
        public void run() {
            SalaryPaymentManager.getInstance().pay(true);
        }

    };

    private boolean isRunning() {
        if (task == null) {
            return false;
        }
        int id = task.getTaskId();
        return SM.getServer().getScheduler().isCurrentlyRunning(id) || SM.getServer().getScheduler().isQueued(id);
    }

    public void schedule(long delay) {
        if (isRunning()) {
            task.cancel();
        }
        starttime = System.currentTimeMillis();
        period = SM.getSettings().getPeriod(); // in seconds
        task = SM.getServer().getScheduler().runTaskTimer(SM, runnable, delay * 20L, period * 20L);
    }

    public long getTimeLeft() {
        return period - ((System.currentTimeMillis() - starttime) / 1000L) % period;
    }

}
