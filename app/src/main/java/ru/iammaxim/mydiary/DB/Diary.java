package ru.iammaxim.mydiary.DB;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import ru.iammaxim.mydiary.App;

/**
 * Created by maxim on 3/19/18.
 */

public class Diary {
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Entry.TABLE_NAME + " (" +
                    Entry._ID + " INTEGER PRIMARY KEY," +
                    Entry.COLUMN_NAME_TITLE + " TEXT," +
                    Entry.COLUMN_NAME_TEXT + " TEXT," +
                    Entry.COLUMN_NAME_DATE + " INTEGER," +
                    Entry.COLUMN_NAME_PATH + " TEXT)";

    public static final String SQL_DROP_ENTRIES =
            "DROP TABLE IF EXISTS " + Entry.TABLE_NAME;

    private Diary() {
    }

    public static class Entry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_PATH = "path";

        public static long create(String title, String text, long date, String path) {
            SQLiteDatabase db = App.mDBHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_TITLE, title);
            values.put(COLUMN_NAME_TEXT, text);
            values.put(COLUMN_NAME_DATE, date);
            values.put(COLUMN_NAME_PATH, path);

            return db.insert(TABLE_NAME, null, values);
        }
    }
}
