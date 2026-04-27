package com.example.smartspendai.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.example.smartspendai.data.local.SmartSpendDbHelper;
import com.example.smartspendai.data.model.User;
import com.example.smartspendai.util.PasswordHasher;

public class UserRepository {
    private final SmartSpendDbHelper dbHelper;

    public UserRepository(Context context) {
        dbHelper = new SmartSpendDbHelper(context.getApplicationContext());
    }

    public RegisterResult register(String name, String email, String password) {
        String salt = PasswordHasher.createSalt();
        String passwordHash = PasswordHasher.hash(password, salt);

        ContentValues values = new ContentValues();
        values.put(SmartSpendDbHelper.COLUMN_NAME, name.trim());
        values.put(SmartSpendDbHelper.COLUMN_EMAIL, email.trim().toLowerCase());
        values.put(SmartSpendDbHelper.COLUMN_PASSWORD_HASH, passwordHash);
        values.put(SmartSpendDbHelper.COLUMN_PASSWORD_SALT, salt);
        values.put(SmartSpendDbHelper.COLUMN_CREATED_AT, System.currentTimeMillis());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            long userId = db.insertOrThrow(SmartSpendDbHelper.TABLE_USERS, null, values);
            return new RegisterResult(true, null, new User(userId, name.trim(), email.trim().toLowerCase()));
        } catch (SQLiteConstraintException exception) {
            return new RegisterResult(false, "This email is already registered.", null);
        }
    }

    public User login(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] columns = {
                SmartSpendDbHelper.COLUMN_ID,
                SmartSpendDbHelper.COLUMN_NAME,
                SmartSpendDbHelper.COLUMN_EMAIL,
                SmartSpendDbHelper.COLUMN_PASSWORD_HASH,
                SmartSpendDbHelper.COLUMN_PASSWORD_SALT
        };

        try (Cursor cursor = db.query(
                SmartSpendDbHelper.TABLE_USERS,
                columns,
                SmartSpendDbHelper.COLUMN_EMAIL + " = ?",
                new String[]{email.trim().toLowerCase()},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }

            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_PASSWORD_HASH));
            String salt = cursor.getString(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_PASSWORD_SALT));
            if (!PasswordHasher.matches(password, salt, storedHash)) {
                return null;
            }

            long id = cursor.getLong(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_NAME));
            String storedEmail = cursor.getString(cursor.getColumnIndexOrThrow(SmartSpendDbHelper.COLUMN_EMAIL));
            return new User(id, name, storedEmail);
        }
    }

    public static class RegisterResult {
        private final boolean success;
        private final String errorMessage;
        private final User user;

        public RegisterResult(boolean success, String errorMessage, User user) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.user = user;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public User getUser() {
            return user;
        }
    }
}
