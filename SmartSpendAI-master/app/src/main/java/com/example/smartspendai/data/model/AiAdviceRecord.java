package com.example.smartspendai.data.model;

public class AiAdviceRecord {
    private final long id;
    private final long userId;
    private final long budgetId;
    private final String adviceText;
    private final int statusCode;
    private final long createdAt;

    public AiAdviceRecord(long id, long userId, long budgetId, String adviceText,
                          int statusCode, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.budgetId = budgetId;
        this.adviceText = adviceText;
        this.statusCode = statusCode;
        this.createdAt = createdAt;
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

    public String getAdviceText() {
        return adviceText;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
