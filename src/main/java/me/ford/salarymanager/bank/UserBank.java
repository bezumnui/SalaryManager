package me.ford.salarymanager.bank;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import net.milkbowl.vault.economy.Economy;

public class UserBank implements BankAccount {
    private final Economy econ;
    private final UUID id;

    public UserBank(Economy econ, OfflinePlayer player) {
        this(econ, player.getUniqueId());
    }

    public UserBank(Economy econ, UUID id) {
        this.econ = econ;
        this.id = id;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(id);
    }

    @Override
    public double getMoney() {
        return econ.getBalance(getPlayer());
    }

    @Override
    public void removeMoney(double amount) throws IllegalArgumentException {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot remove negative value");
        }
        econ.withdrawPlayer(getPlayer(), amount);
    }

    @Override
    public void addMoney(double amount) throws IllegalArgumentException {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative value");
        }
        econ.depositPlayer(getPlayer(), amount);
    }

    @Override
    public String asSaveableString() {
        return id.toString();
    }

}
