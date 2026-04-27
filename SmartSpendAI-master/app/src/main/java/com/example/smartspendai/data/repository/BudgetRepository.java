package com.example.smartspendai.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smartspendai.data.local.SmartSpendDbHelper;
import com.example.smartspendai.data.model.BudgetCategory;
import com.example.smartspendai.data.model.BudgetSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BudgetRepository {
    public static final String[] DEFAULT_CATEGORIES = {
            "Food",
            "Transport",
            "Bills",
            "Entertainment",
            "Education",
            "Savings",
            "Others"
    };

    private final SmartSpendDbHelper dbHelper;

    public BudgetRepository(Context context) {
        dbHelper = new SmartSpendDbHelper(context.getApplicationContext());
    }

    public long saveMonthlyBudget(long userId, int month, int year, double income, Map<String, Double> allocations) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            long budgetId = findBudgetId(db, userId, month, year);
            ContentValues budgetValues = new ContentValues();
            budgetValues.put(SmartSpendDbHelper.COLUMN_USER_ID, userId);
            budgetValues.put(SmartSpendDbHelper.COLUMN_MONTH, month);
            budgetValues.put(SmartSpendDbHelper.COLUMN_YEAR, year);
            budgetValues.put(SmartSpendDbHelper.COLUMN_INCOME, income);
            budgetValues.put(SmartSpendDbHelper.COLUMN_CREATED_AT, System.currentTimeMillis());

            if (budgetId == -1) {
                budgetId = db.insertOrThrow(SmartSpendDbHelper.TABLE_BUDGETS, null, budgetValues);
            } else {
                db.update(
                        SmartSpendDbHelper.TABLE_BUDGETS,
                        budgetValues,
                        SmartSpendDbHelper.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(budgetId)}
                );
                upsertBudgetCategories(db, budgetId, allocations);
                db.setTransactionSuccessful();
                return budgetId;
            }

            insertBudgetCategories(db, budgetId, allocations);
            db.setTransactionSuccessful();
            return budgetId;
        } finally {
            db.endTransaction();
        }
    }

    public BudgetSummary getMonthlyBudget(long userId, int month, int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] columns = {
                SmartSpendDbHelper.COLUMN_ID,
                SmartSpendDbHelper.COLUMN_INCOME
        };

        try (Cursor cursor = db.query(
                SmartSpendDbHelper.TABLE_BUDGETS,
                columns,
                SmartSpendDbHelper.COLUMN_USER_ID + " = ? AND "
                        + SmartSpendDbHelper.COLUMN_MONTH + " = ? AND "
                        + SmartSpendDbHelper.COLUMN_YEAR + " = ?",
                new String[]{String.valueOf(userId), String.valueOf(month), String.valueOf(year)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }

            long budgetId = cursor.getLong(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_ID));
            double income = cursor.getDouble(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_INCOME));
            return new BudgetSummary(budgetId, userId, month, year, income, getCategories(db, budgetId));
        }
    }

    private long findBudgetId(SQLiteDatabase db, long userId, int month, int year) {
        try (Cursor cursor = db.query(
                SmartSpendDbHelper.TABLE_BUDGETS,
                new String[]{SmartSpendDbHelper.COLUMN_ID},
                SmartSpendDbHelper.COLUMN_USER_ID + " = ? AND "
                        + SmartSpendDbHelper.COLUMN_MONTH + " = ? AND "
                        + SmartSpendDbHelper.COLUMN_YEAR + " = ?",
                new String[]{String.valueOf(userId), String.valueOf(month), String.valueOf(year)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return -1;
            }
            return cursor.getLong(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_ID));
        }
    }

    private List<BudgetCategory> getCategories(SQLiteDatabase db, long budgetId) {
        List<BudgetCategory> categories = new ArrayList<>();
        try (Cursor cursor = db.query(
                SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES,
                null,
                SmartSpendDbHelper.COLUMN_BUDGET_ID + " = ?",
                new String[]{String.valueOf(budgetId)},
                null,
                null,
                SmartSpendDbHelper.COLUMN_ID + " ASC"
        )) {
            while (cursor.moveToNext()) {
                categories.add(new BudgetCategory(
                        cursor.getLong(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_ID)),
                        budgetId,
                        cursor.getString(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_CATEGORY_NAME)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_ALLOCATED_AMOUNT)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_SPENT_AMOUNT))
                ));
            }
        }
        return categories;
    }

    private void insertBudgetCategories(SQLiteDatabase db, long budgetId, Map<String, Double> allocations) {
        for (String categoryName : DEFAULT_CATEGORIES) {
            double allocatedAmount = allocations.containsKey(categoryName)
                    ? allocations.get(categoryName)
                    : 0;

            ContentValues categoryValues = new ContentValues();
            categoryValues.put(SmartSpendDbHelper.COLUMN_BUDGET_ID, budgetId);
            categoryValues.put(SmartSpendDbHelper.COLUMN_CATEGORY_NAME, categoryName);
            categoryValues.put(SmartSpendDbHelper.COLUMN_ALLOCATED_AMOUNT, allocatedAmount);
            categoryValues.put(SmartSpendDbHelper.COLUMN_SPENT_AMOUNT, 0);
            db.insertOrThrow(SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES, null, categoryValues);
        }
    }

    private void upsertBudgetCategories(SQLiteDatabase db, long budgetId, Map<String, Double> allocations) {
        for (String categoryName : DEFAULT_CATEGORIES) {
            double allocatedAmount = allocations.containsKey(categoryName)
                    ? allocations.get(categoryName)
                    : 0;

            ContentValues categoryValues = new ContentValues();
            categoryValues.put(SmartSpendDbHelper.COLUMN_ALLOCATED_AMOUNT, allocatedAmount);

            int updatedRows = db.update(
                    SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES,
                    categoryValues,
                    SmartSpendDbHelper.COLUMN_BUDGET_ID + " = ? AND "
                            + SmartSpendDbHelper.COLUMN_CATEGORY_NAME + " = ?",
                    new String[]{String.valueOf(budgetId), categoryName}
            );

            if (updatedRows == 0) {
                categoryValues.put(SmartSpendDbHelper.COLUMN_BUDGET_ID, budgetId);
                categoryValues.put(SmartSpendDbHelper.COLUMN_CATEGORY_NAME, categoryName);
                categoryValues.put(SmartSpendDbHelper.COLUMN_SPENT_AMOUNT, 0);
                db.insertOrThrow(SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES, null, categoryValues);
            }
        }
    }
}
