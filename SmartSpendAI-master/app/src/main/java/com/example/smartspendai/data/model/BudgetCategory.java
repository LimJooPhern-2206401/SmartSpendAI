package com.example.smartspendai.data.model;

public class BudgetCategory {
    private final long id;
    private final long budgetId;
    private final String name;
    private final double allocatedAmount;
    private final double spentAmount;

    public BudgetCategory(long id, long budgetId, String name, double allocatedAmount, double spentAmount) {
        this.id = id;
        this.budgetId = budgetId;
        this.name = name;
        this.allocatedAmount = allocatedAmount;
        this.spentAmount = spentAmount;
    }

    public long getId() {
        return id;
    }

    public long getBudgetId() {
        return budgetId;
    }

    public String getName() {
        return name;
    }

    public double getAllocatedAmount() {
        return allocatedAmount;
    }

    public double getSpentAmount() {
        return spentAmount;
    }

    public double getRemainingAmount() {
        return allocatedAmount - spentAmount;
    }
}
