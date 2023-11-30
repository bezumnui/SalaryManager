package me.ford.salarymanager.bank;

public interface BankAccount {

    /**
     * Gets the amount of money currently in bank.
     */
    double getMoney();

    /**
     * Removes a set amount of money from player.
     *
     * Throws illegal argument exception if amount is negative.
     *
     * @param amount - amount to remove
     * @throws IllegalArgumentException is thrown when amount is negative
     */
    void removeMoney(double amount) throws IllegalArgumentException;

    /**
     * Adds a set amount of money from player.
     *
     * Throws illegal argument exception if amount is negative.
     *
     * @param amount - amount to remove
     * @throws IllegalArgumentException is thrown when amount is negative
     */
    void addMoney(double amount) throws IllegalArgumentException;

    /**
     * Returns a way to save this account.
     *
     * @return a saveable string if this can be saved, null otherwise.
     */
    String asSaveableString();

}
