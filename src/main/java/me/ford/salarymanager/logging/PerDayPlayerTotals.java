package me.ford.salarymanager.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.ford.salarymanager.SalaryHandler;
import me.ford.salarymanager.SalaryManager;

public class PerDayPlayerTotals implements Listener {
    private final SalaryManager plugin;
    private final PerDayLogger pdl;
    private final Map<UUID, Integer> onlinePlayers = new HashMap<>();
    private final Map<UUID, Integer> offlinePlayers = new HashMap<>();

    public PerDayPlayerTotals(SalaryManager plugin, PerDayLogger pdl) {
        this.plugin = plugin;
        this.pdl = pdl;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            checkAndReaddOnlinePlayers();
            checkAndReaddOfflinePlayers();
        });
    }

    private void checkAndReaddOnlinePlayers() {
        onlinePlayers.clear();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            setOnlinePlayer(player, true);
        }
    }

    private void setOnlinePlayer(Player player, boolean forceReset) {
        UUID id = player.getUniqueId();
        // if already listed as offline salary, don't go looking in config
        Integer val = offlinePlayers.get(id);
        if (val == null || forceReset) { // force reset when day resets
            // look in config if not listed as offline salary
            val = pdl.getNumberOfPaymentsToday(player);
        }
        onlinePlayers.put(id, val);
    }

    private void checkAndReaddOfflinePlayers() {
        offlinePlayers.clear();
        SalaryHandler sal = SalaryHandler.getInstance();
        for (UUID uuid : sal.getOfflineSalaries().keySet()) {
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(uuid);
            if (player == null || (!player.isOnline() && !player.hasPlayedBefore())) {
                plugin.getLogger().warning("Unknown player getting offline salaires:" + uuid);
                continue;
            }
            int count = pdl.getNumberOfPaymentsToday(player);
            offlinePlayers.put(uuid, count);
        }
    }

    // TODO - CALL somewhere
    public void resetDay() {
        checkAndReaddOnlinePlayers();
        checkAndReaddOfflinePlayers();
    }

    public int getNumberOfPaymentsToday(OfflinePlayer player) {
        if (player.isOnline()) {
            return onlinePlayers.get(player.getUniqueId());
        } else {
            Integer val = offlinePlayers.get(player.getUniqueId());
            return val == null ? 0 : val;
        }
    }

    public void increment(OfflinePlayer player) {
        increment(player.getUniqueId());
    }

    public void increment(UUID id) {
        if (onlinePlayers.containsKey(id)) {
            int prev = onlinePlayers.get(id);
            onlinePlayers.put(id, prev + 1);
        }
        if (offlinePlayers.containsKey(id)) {
            int prev = offlinePlayers.get(id);
            offlinePlayers.put(id, prev + 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setOnlinePlayer(player, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        onlinePlayers.remove(player.getUniqueId());
    }

}
