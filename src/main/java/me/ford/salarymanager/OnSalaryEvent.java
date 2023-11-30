package me.ford.salarymanager;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class OnSalaryEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private double amount;
    private boolean cancelled = false;
    private boolean shouldSendMessage = true;

    public OnSalaryEvent(@NotNull Player player, double amount) {
        super(player);
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setShouldSendMessage(boolean shouldSendMessage) {
        this.shouldSendMessage = shouldSendMessage;
    }
    public boolean getShouldSendMessage() {
        return shouldSendMessage;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
