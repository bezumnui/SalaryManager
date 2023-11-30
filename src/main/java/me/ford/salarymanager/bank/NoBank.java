package me.ford.salarymanager.bank;

public final class NoBank implements BankAccount {
    private static final NoBank INSTANCE = new NoBank();

    public static NoBank getInstance() {
        return INSTANCE;
    }

    private NoBank() {
        // private constructor
    }

    @Override
    public double getMoney() {
        return 0;
    }

    @Override
    public void removeMoney(double amount) throws IllegalArgumentException {
        // NOTHING
    }

    @Override
    public void addMoney(double amount) throws IllegalArgumentException {
        // NOTHING
    }

    @Override
    public String asSaveableString() {
        return null;
    }

}
