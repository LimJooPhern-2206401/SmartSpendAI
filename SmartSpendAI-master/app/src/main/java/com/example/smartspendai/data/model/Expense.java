package com.example.smartspendai.data.model;

public class Expense {
    private final long id;
    private final long userId;
    private final long budgetId;
    private final long categoryId;
    private final String categoryName;
    private final String title;
    private final double amount;
    private final String note;
    private final long expenseDate;

    public Expense(long id, long userId, long budgetId, long categoryId, String categoryName,
                   String title, double amount, String note, long expenseDate) {
        this.id = id;
        this.userId = userId;
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.title = title;
        this.amount = amount;
        this.note = note;
        this.expenseDate = expenseDate;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public long getBudgetId() {
        return budgetId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getTitle() {
        return title;
    }

    public double getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public long getExpenseDate() {
        return expenseDate;
    }
}
