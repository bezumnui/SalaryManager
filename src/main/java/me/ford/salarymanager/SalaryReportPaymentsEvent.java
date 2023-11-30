package me.ford.salarymanager;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SalaryReportPaymentsEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    public SalaryReportPaymentsEvent() {
        super();
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
