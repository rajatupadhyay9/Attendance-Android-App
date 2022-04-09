package com.example.attendance.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.attendance.util.Util;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;

public class DatabaseHandler extends SQLiteOpenHelper {
    Context context;

    public DatabaseHandler(Context context) {
        super(context, Util.DATABASE_NAME, null, Util.DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insert(String date, ArrayList<String> devices, String TABLE_NAME) {
        SQLiteDatabase db = this.getWritableDatabase();
        String create_table_query = "create table if not exists "
                + TABLE_NAME + "(ROLL_NO text)";
        db.execSQL(create_table_query);
        String alter_table_query = "alter table " + TABLE_NAME + " add " +
                date + " char default 'A'";
        try {
            db.execSQL(alter_table_query);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String device : devices) {
            Cursor cursor = db.rawQuery("select ROLL_NO from " + TABLE_NAME +
                    " where ROLL_NO == \"" + device + "\"", null);
            if (cursor == null || cursor.getCount() == 0) {
                db.execSQL("insert into " + TABLE_NAME + "(ROLL_NO," + date + ")"
                        + "values (\"" + device + "\", 'P')");
            } else {
                db.execSQL("update " + TABLE_NAME + " set " + date +
                        " = 'P' where ROLL_NO == \"" + device + "\"");
            }
            if (cursor != null) cursor.close();
        }
        Toast.makeText(context, devices.size() + " records inserted successfully",
                Toast.LENGTH_LONG).show();
        db.close();
    }

    public void exportDB(String TABLE_NAME) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TABLE_NAME, null);

        File exportDir = new File(Environment.getExternalStorageDirectory(), "attendance");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, TABLE_NAME + ".csv");
        try {
            file.createNewFile();
            CSVWriter csvWriter = new CSVWriter(new FileWriter(file));
            csvWriter.writeNext(cursor.getColumnNames());
            while (cursor.moveToNext()) {
                Log.d("TAG", "exportDB: MOVINF NEXT");
                int columns = cursor.getColumnCount();
                String[] columnArr = new String[columns];
                for (int i = 0; i < columns; i++) {
                    columnArr[i] = cursor.getString(i);
                }
                for (String i : columnArr) Log.d("TAG", "LOOPDB: " + i);
                csvWriter.writeNext(columnArr);
            }
            csvWriter.close();
            cursor.close();
            Toast.makeText(context, "Attendance successfully exported at : " + file.getAbsolutePath()
                    , Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "There was some error", Toast.LENGTH_LONG).show();
        }
        db.close();
    }

    public List<List<String>> populate(List<List<String>> datacache, String TABLE_NAME) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from " + TABLE_NAME + " order by ROLL_NO asc", null);
            if (cursor == null) {
                datacache.clear();
                return datacache;
            }
            int n = cursor.getCount();
            datacache.clear();
            List<String> empty = new ArrayList<>();
            empty.add(" ");empty.add(" ");empty.add(" ");empty.add(" ");
            List<String> temp = new ArrayList<String>(empty);
            cursor.moveToNext();
            temp.set(0, "ROLL_NO");
            datacache.add(temp);
            for (int i = 0; i < n; i++) {
                temp = new ArrayList<>(empty);
                temp.set(0, cursor.getString(0));
                datacache.add(temp);
                cursor.moveToNext();
            }

            int x = 0;
            String[] cols = cursor.getColumnNames();
            for(String c : cols) Log.d("TAG", "namaywa: " + c);
            Log.d("TAG", "populate: " + cols.length + ":" + cursor.getColumnCount());
            for (int i = cursor.getColumnCount() - 1; i > cursor.getColumnCount() - 4 && i > 0; i--) {
                Log.d("TAG", "populate: for i = " + i);
                cursor.moveToFirst();
                temp = datacache.get(0);
                Log.d("TAG", "populate: for 3-x = " + (3-x));
                temp.set(3 - x, cols[i]);
                datacache.set(0, temp);
                Log.d("TAG", "update: " + datacache.get(0).toString());
                for (int j = 1; j <= n; j++) {
                    temp = datacache.get(j);
                    temp.set(3 - x, cursor.getString(i));
                    datacache.set(j, temp);
                    cursor.moveToNext();
                }
                x++;
            }
            cursor.close();
            db.close();
            return datacache;
        } catch (Exception e) {
            e.printStackTrace();
            db.close();
//            datacache.clear();
            return datacache;
        }
    }

    public void delete(String TABLE_NAME) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("drop table if exists " + TABLE_NAME);
        db.close();
    }
}
