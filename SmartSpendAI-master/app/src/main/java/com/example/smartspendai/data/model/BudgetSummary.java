package com.example.smartspendai.data.model;

import java.util.List;

public class BudgetSummary {
    private final long id;
    private final long userId;
    private final int month;
    private final int year;
    private final double income;
    private final List<BudgetCategory> categories;

    public BudgetSummary(long id, long userId, int month, int year, double income, List<BudgetCategory> categories) {
        this.id = id;
        this.userId = userId;
        this.month = month;
        this.year = year;
        this.income = income;
        this.categories = categories;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public double getIncome() {
        return income;
    }

    public List<BudgetCategory> getCategories() {
        return categories;
    }

    public double getTotalAllocated() {
        double total = 0;
        for (BudgetCategory category : categories) {
            total += category.getAllocatedAmount();
        }
        return total;
    }

    public double getTotalSpent() {
        double total = 0;
        for (BudgetCategory category : categories) {
            total += category.getSpentAmount();
        }
        return total;
    }

    public double getUnallocatedAmount() {
        return income - getTotalAllocated();
    }
}
