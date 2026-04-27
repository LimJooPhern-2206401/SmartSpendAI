package com.example.smartspendai.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SmartSpendDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "smart_spend.db";
    public static final int DATABASE_VERSION = 4;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_BUDGETS = "budgets";
    public static final String TABLE_BUDGET_CATEGORIES = "budget_categories";
    public static final String TABLE_EXPENSES = "expenses";
    public static final String TABLE_AI_ADVICE = "ai_advice";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD_HASH = "password_hash";
    public static final String COLUMN_PASSWORD_SALT = "password_salt";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_MONTH = "month";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_INCOME = "income";
    public static final String COLUMN_BUDGET_ID = "budget_id";
    public static final String COLUMN_CATEGORY_NAME = "category_name";
    public static final String COLUMN_ALLOCATED_AMOUNT = "allocated_amount";
    public static final String COLUMN_SPENT_AMOUNT = "spent_amount";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_NOTE = "note";
    public static final String COLUMN_EXPENSE_DATE = "expense_date";
    public static final String COLUMN_ADVICE_TEXT = "advice_text";
    public static final String COLUMN_STATUS_CODE = "status_code";

    public SmartSpendDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createUserTable(db);
        createBudgetTables(db);
        createExpenseTable(db);
        createAiAdviceTable(db);
    }

    private void createUserTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_EMAIL + " TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                + COLUMN_PASSWORD_HASH + " TEXT NOT NULL, "
                + COLUMN_PASSWORD_SALT + " TEXT NOT NULL, "
                + COLUMN_CREATED_AT + " INTEGER NOT NULL"
                + ")");
    }

    private void createBudgetTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BUDGETS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USER_ID + " INTEGER NOT NULL, "
                + COLUMN_MONTH + " INTEGER NOT NULL, "
                + COLUMN_YEAR + " INTEGER NOT NULL, "
                + COLUMN_INCOME + " REAL NOT NULL, "
                + COLUMN_CREATED_AT + " INTEGER NOT NULL, "
                + "UNIQUE(" + COLUMN_USER_ID + ", " + COLUMN_MONTH + ", " + COLUMN_YEAR + ")"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BUDGET_CATEGORIES + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_BUDGET_ID + " INTEGER NOT NULL, "
                + COLUMN_CATEGORY_NAME + " TEXT NOT NULL, "
                + COLUMN_ALLOCATED_AMOUNT + " REAL NOT NULL, "
                + COLUMN_SPENT_AMOUNT + " REAL NOT NULL DEFAULT 0"
                + ")");
    }

    private void createExpenseTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXPENSES + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USER_ID + " INTEGER NOT NULL, "
                + COLUMN_BUDGET_ID + " INTEGER NOT NULL, "
                + COLUMN_CATEGORY_ID + " INTEGER NOT NULL, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_AMOUNT + " REAL NOT NULL, "
                + COLUMN_NOTE + " TEXT, "
                + COLUMN_EXPENSE_DATE + " INTEGER NOT NULL, "
                + COLUMN_CREATED_AT + " INTEGER NOT NULL"
                + ")");
    }

    private void createAiAdviceTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AI_ADVICE + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USER_ID + " INTEGER NOT NULL, "
                + COLUMN_BUDGET_ID + " INTEGER NOT NULL, "
                + COLUMN_ADVICE_TEXT + " TEXT NOT NULL, "
                + COLUMN_STATUS_CODE + " INTEGER NOT NULL, "
                + COLUMN_CREATED_AT + " INTEGER NOT NULL"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createBudgetTables(db);
        }
        if (oldVersion < 3) {
            createExpenseTable(db);
        }
        if (oldVersion < 4) {
            createAiAdviceTable(db);
        }
    }
}
