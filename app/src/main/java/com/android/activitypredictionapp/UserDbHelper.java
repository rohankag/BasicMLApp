package com.android.activitypredictionapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

/**
 * Created by aditya on 10/2/16.
 */
public class UserDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    private static final String TAG = UserDbHelper.class.getCanonicalName();
    private static String externalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String DATABASE_NAME = externalStorageDirectory + "/databaseFolder/userActivity.db";
    public static final String TRAINING_FILE_NAME = externalStorageDirectory + "/databaseFolder/trainingData";
    public static final String TABLE_NAME = "user_data";
    public static final String PATIENT_COLUMN_RECORD_ID = "ID";
    public static final String PATIENT_COLUMN_X_VALUE = "AccelX";
    public static final String PATIENT_COLUMN_Y_VALUE = "AccelY";
    public static final String PATIENT_COLUMN_Z_VALUE = "AccelZ";
    public static final String PATIENT_ACTIVITY_LABEL = "ActivityLabel";

    public UserDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v(TAG, "onCreate Db at : " + DATABASE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.v(TAG, "onUpgrade");
    }

    public void createPatientTable(String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String createTableQuery = "create table if not exists " + tableName + " ("
                + PATIENT_COLUMN_RECORD_ID + " integer PRIMARY KEY autoincrement, ";

        for (int i = 1; i <= 50; i++) {
            createTableQuery = createTableQuery + PATIENT_COLUMN_X_VALUE + i + " float,";
            createTableQuery = createTableQuery + PATIENT_COLUMN_Y_VALUE + i + " float,";
            createTableQuery = createTableQuery + PATIENT_COLUMN_Z_VALUE + i + " float,";
        }

        createTableQuery = createTableQuery + PATIENT_ACTIVITY_LABEL + " text );";

        db.execSQL(createTableQuery);
    }

    public boolean insertUserActivityData(float[][] values, String activityLabel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        for (int i = 1; i <= 50; i++) {
            contentValues.put(PATIENT_COLUMN_X_VALUE + i, values[i - 1][0]);
            contentValues.put(PATIENT_COLUMN_Y_VALUE + i, values[i - 1][1]);
            contentValues.put(PATIENT_COLUMN_Z_VALUE + i, values[i - 1][2]);
        }
        contentValues.put(PATIENT_ACTIVITY_LABEL, activityLabel);
        db.insert(TABLE_NAME, null, contentValues);
        return true;
    }

    public Cursor getTrainingData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
        return cursor;
    }

    public int numberOfRows(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, tableName);
        return numRows;
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}