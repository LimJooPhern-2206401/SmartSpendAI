package com.example.smartspendai.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smartspendai.data.local.SmartSpendDbHelper;
import com.example.smartspendai.data.model.Expense;

import java.util.ArrayList;
import java.util.List;

public class ExpenseRepository {
    private final SmartSpendDbHelper dbHelper;

    public ExpenseRepository(Context context) {
        dbHelper = new SmartSpendDbHelper(context.getApplicationContext());
    }

    public long addExpense(long userId, long budgetId, long categoryId, String title,
                           double amount, String note, long expenseDate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(SmartSpendDbHelper.COLUMN_USER_ID, userId);
            values.put(SmartSpendDbHelper.COLUMN_BUDGET_ID, budgetId);
            values.put(SmartSpendDbHelper.COLUMN_CATEGORY_ID, categoryId);
            values.put(SmartSpendDbHelper.COLUMN_TITLE, title.trim());
            values.put(SmartSpendDbHelper.COLUMN_AMOUNT, amount);
            values.put(SmartSpendDbHelper.COLUMN_NOTE, note.trim());
            values.put(SmartSpendDbHelper.COLUMN_EXPENSE_DATE, expenseDate);
            values.put(SmartSpendDbHelper.COLUMN_CREATED_AT, System.currentTimeMillis());

            long expenseId = db.insertOrThrow(SmartSpendDbHelper.TABLE_EXPENSES, null, values);
            db.execSQL(
                    "UPDATE " + SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES
                            + " SET " + SmartSpendDbHelper.COLUMN_SPENT_AMOUNT
                            + " = " + SmartSpendDbHelper.COLUMN_SPENT_AMOUNT + " + ?"
                            + " WHERE " + SmartSpendDbHelper.COLUMN_ID + " = ?",
                    new Object[]{amount, categoryId}
            );
            db.setTransactionSuccessful();
            return expenseId;
        } finally {
            db.endTransaction();
        }
    }

    public List<Expense> getRecentExpenses(long budgetId, int limit) {
        return getExpenses(budgetId, limit);
    }

    public List<Expense> getAllExpenses(long budgetId) {
        return getExpenses(budgetId, -1);
    }

    public List<Expense> getExpensesForUserMonth(long userId, int month, int year) {
        String query = "SELECT e." + SmartSpendDbHelper.COLUMN_ID
                + ", e." + SmartSpendDbHelper.COLUMN_USER_ID
                + ", e." + SmartSpendDbHelper.COLUMN_BUDGET_ID
                + ", e." + SmartSpendDbHelper.COLUMN_CATEGORY_ID
                + ", c." + SmartSpendDbHelper.COLUMN_CATEGORY_NAME
                + ", e." + SmartSpendDbHelper.COLUMN_TITLE
                + ", e." + SmartSpendDbHelper.COLUMN_AMOUNT
                + ", e." + SmartSpendDbHelper.COLUMN_NOTE
                + ", e." + SmartSpendDbHelper.COLUMN_EXPENSE_DATE
                + " FROM " + SmartSpendDbHelper.TABLE_EXPENSES + " e"
                + " INNER JOIN " + SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES + " c"
                + " ON e." + SmartSpendDbHelper.COLUMN_CATEGORY_ID
                + " = c." + SmartSpendDbHelper.COLUMN_ID
                + " INNER JOIN " + SmartSpendDbHelper.TABLE_BUDGETS + " b"
                + " ON e." + SmartSpendDbHelper.COLUMN_BUDGET_ID
                + " = b." + SmartSpendDbHelper.COLUMN_ID
                + " WHERE e." + SmartSpendDbHelper.COLUMN_USER_ID + " = ?"
                + " AND b." + SmartSpendDbHelper.COLUMN_MONTH + " = ?"
                + " AND b." + SmartSpendDbHelper.COLUMN_YEAR + " = ?"
                + " ORDER BY e." + SmartSpendDbHelper.COLUMN_EXPENSE_DATE + " DESC, e."
                + SmartSpendDbHelper.COLUMN_ID + " DESC";

        return getExpensesFromQuery(query, new String[]{
                String.valueOf(userId),
                String.valueOf(month),
                String.valueOf(year)
        });
    }

    public List<Expense> getExpensesForUserDate(long userId, long startMillis, long endMillis) {
        String query = "SELECT e." + SmartSpendDbHelper.COLUMN_ID
                + ", e." + SmartSpendDbHelper.COLUMN_USER_ID
                + ", e." + SmartSpendDbHelper.COLUMN_BUDGET_ID
                + ", e." + SmartSpendDbHelper.COLUMN_CATEGORY_ID
                + ", c." + SmartSpendDbHelper.COLUMN_CATEGORY_NAME
                + ", e." + SmartSpendDbHelper.COLUMN_TITLE
                + ", e." + SmartSpendDbHelper.COLUMN_AMOUNT
                + ", e." + SmartSpendDbHelper.COLUMN_NOTE
                + ", e." + SmartSpendDbHelper.COLUMN_EXPENSE_DATE
                + " FROM " + SmartSpendDbHelper.TABLE_EXPENSES + " e"
                + " INNER JOIN " + SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES + " c"
                + " ON e." + SmartSpendDbHelper.COLUMN_CATEGORY_ID
                + " = c." + SmartSpendDbHelper.COLUMN_ID
                + " WHERE e." + SmartSpendDbHelper.COLUMN_USER_ID + " = ?"
                + " AND e." + SmartSpendDbHelper.COLUMN_EXPENSE_DATE + " >= ?"
                + " AND e." + SmartSpendDbHelper.COLUMN_EXPENSE_DATE + " < ?"
                + " ORDER BY e." + SmartSpendDbHelper.COLUMN_EXPENSE_DATE + " DESC, e."
                + SmartSpendDbHelper.COLUMN_ID + " DESC";

        return getExpensesFromQuery(query, new String[]{
                String.valueOf(userId),
                String.valueOf(startMillis),
                String.valueOf(endMillis)
        });
    }

    private List<Expense> getExpenses(long budgetId, int limit) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT e." + SmartSpendDbHelper.COLUMN_ID
                + ", e." + SmartSpendDbHelper.COLUMN_USER_ID
                + ", e." + SmartSpendDbHelper.COLUMN_BUDGET_ID
                + ", e." + SmartSpendDbHelper.COLUMN_CATEGORY_ID
                + ", c." + SmartSpendDbHelper.COLUMN_CATEGORY_NAME
                + ", e." + SmartSpendDbHelper.COLUMN_TITLE
                + ", e." + SmartSpendDbHelper.COLUMN_AMOUNT
                + ", e." + SmartSpendDbHelper.COLUMN_NOTE
                + ", e." + SmartSpendDbHelper.COLUMN_EXPENSE_DATE
                + " FROM " + SmartSpendDbHelper.TABLE_EXPENSES + " e"
                + " INNER JOIN " + SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES + " c"
                + " ON e." + SmartSpendDbHelper.COLUMN_CATEGORY_ID
                + " = c." + SmartSpendDbHelper.COLUMN_ID
                + " WHERE e." + SmartSpendDbHelper.COLUMN_BUDGET_ID + " = ?"
                + " ORDER BY e." + SmartSpendDbHelper.COLUMN_EXPENSE_DATE + " DESC, e."
                + SmartSpendDbHelper.COLUMN_ID + " DESC";

        String[] args;
        if (limit > 0) {
            query = query + " LIMIT ?";
            args = new String[]{String.valueOf(budgetId), String.valueOf(limit)};
        } else {
            args = new String[]{String.valueOf(budgetId)};
        }

        return getExpensesFromQuery(query, args);
    }

    private List<Expense> getExpensesFromQuery(String query, String[] args) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(query, args)) {
            while (cursor.moveToNext()) {
                expenses.add(new Expense(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getLong(2),
                        cursor.getLong(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getDouble(6),
                        cursor.getString(7),
                        cursor.getLong(8)
                ));
            }
        }
        return expenses;
    }

    public boolean deleteExpense(long expenseId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Expense expense = findExpenseById(db, expenseId);
            if (expense == null) {
                return false;
            }

            int deletedRows = db.delete(
                    SmartSpendDbHelper.TABLE_EXPENSES,
                    SmartSpendDbHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(expenseId)}
            );

            if (deletedRows > 0) {
                db.execSQL(
                        "UPDATE " + SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES
                                + " SET " + SmartSpendDbHelper.COLUMN_SPENT_AMOUNT
                                + " = " + SmartSpendDbHelper.COLUMN_SPENT_AMOUNT + " - ?"
                                + " WHERE " + SmartSpendDbHelper.COLUMN_ID + " = ?",
                        new Object[]{expense.getAmount(), expense.getCategoryId()}
                );
                db.setTransactionSuccessful();
                return true;
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }

    private Expense findExpenseById(SQLiteDatabase db, long expenseId) {
        String query = "SELECT e." + SmartSpendDbHelper.COLUMN_ID
                + ", e." + SmartSpendDbHelper.COLUMN_USER_ID
                + ", e." + SmartSpendDbHelper.COLUMN_BUDGET_ID
                + ", e." + SmartSpendDbHelper.COLUMN_CATEGORY_ID
                + ", c." + SmartSpendDbHelper.COLUMN_CATEGORY_NAME
                + ", e." + SmartSpendDbHelper.COLUMN_TITLE
                + ", e." + SmartSpendDbHelper.COLUMN_AMOUNT
                + ", e." + SmartSpendDbHelper.COLUMN_NOTE
                + ", e." + SmartSpendDbHelper.COLUMN_EXPENSE_DATE
                + " FROM " + SmartSpendDbHelper.TABLE_EXPENSES + " e"
                + " INNER JOIN " + SmartSpendDbHelper.TABLE_BUDGET_CATEGORIES + " c"
                + " ON e." + SmartSpendDbHelper.COLUMN_CATEGORY_ID
                + " = c." + SmartSpendDbHelper.COLUMN_ID
                + " WHERE e." + SmartSpendDbHelper.COLUMN_ID + " = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(expenseId)})) {
            if (!cursor.moveToFirst()) {
                return null;
            }

            return new Expense(
                    cursor.getLong(0),
                    cursor.getLong(1),
                    cursor.getLong(2),
                    cursor.getLong(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getDouble(6),
                    cursor.getString(7),
                    cursor.getLong(8)
            );
        }
    }
}
