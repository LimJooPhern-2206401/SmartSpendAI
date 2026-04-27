package com.example.smartspendai.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smartspendai.data.local.SmartSpendDbHelper;
import com.example.smartspendai.data.model.AiAdviceRecord;

import java.util.ArrayList;
import java.util.List;

public class AiAdviceRepository {
    private final SmartSpendDbHelper dbHelper;

    public AiAdviceRepository(Context context) {
        dbHelper = new SmartSpendDbHelper(context.getApplicationContext());
    }

    public long saveAdvice(long userId, long budgetId, String adviceText, int statusCode) {
        ContentValues values = new ContentValues();
        values.put(SmartSpendDbHelper.COLUMN_USER_ID, userId);
        values.put(SmartSpendDbHelper.COLUMN_BUDGET_ID, budgetId);
        values.put(SmartSpendDbHelper.COLUMN_ADVICE_TEXT, adviceText);
        values.put(SmartSpendDbHelper.COLUMN_STATUS_CODE, statusCode);
        values.put(SmartSpendDbHelper.COLUMN_CREATED_AT, System.currentTimeMillis());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insertOrThrow(SmartSpendDbHelper.TABLE_AI_ADVICE, null, values);
    }

    public List<AiAdviceRecord> getAdviceHistory(long budgetId) {
        List<AiAdviceRecord> records = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                SmartSpendDbHelper.TABLE_AI_ADVICE,
                null,
                SmartSpendDbHelper.COLUMN_BUDGET_ID + " = ?",
                new String[]{String.valueOf(budgetId)},
                null,
                null,
                SmartSpendDbHelper.COLUMN_CREATED_AT + " DESC"
        )) {
            while (cursor.moveToNext()) {
                records.add(new AiAdviceRecord(
                        cursor.getLong(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_USER_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_BUDGET_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_ADVICE_TEXT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_STATUS_CODE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_CREATED_AT))
                ));
            }
        }
        return records;
    }
}
