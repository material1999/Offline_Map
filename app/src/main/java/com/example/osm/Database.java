package com.example.osm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Database extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "places.db";
    public static final String TABLE_NAME = "places_table";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "NAME";
    public static final String COL_3 = "COORDINATES_X";
    public static final String COL_4 = "COORDINATES_Y";
    public static final String COL_5 = "TYPE";
    public static final String COL_6 = "DESCRIPTION";


    public Database(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "(ID INTEGER PRIMARY KEY AUTOINCREMENT,NAME TEXT,COORDINATES_X REAL," +
                "COORDINATES_Y TEXT,TYPE TEXT,DESCRIPTION TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public SQLiteDatabase addData(InputStreamReader is, Context c) {
        c.deleteDatabase(DATABASE_NAME);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        String row;
        try {
            BufferedReader reader = new BufferedReader(is);
            while ((row = reader.readLine()) != null) {
                String[] data = row.split(";");
                contentValues.put(COL_2, data[0]);
                contentValues.put(COL_3, data[1]);
                contentValues.put(COL_4, data[2]);
                contentValues.put(COL_5, data[3]);
                contentValues.put(COL_6, data[4]);
                db.insert(TABLE_NAME,null, contentValues);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return db;
    }

    public Cursor getCursor() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME, null);
        return res;
    }
}
