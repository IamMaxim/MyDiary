package ru.iammaxim.mydiary.Activities;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import ru.iammaxim.mydiary.App;
import ru.iammaxim.mydiary.DB.Diary;
import ru.iammaxim.mydiary.R;

public class NoteEditor extends AppCompatActivity {
    private long id;
    private String title, initialTitle;
    private String initialText = "ERROR LOADING TEXT";
    private String path;

    private EditText text_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);
        text_tv = findViewById(R.id.text);

        if (!getIntent().hasExtra("id")) {
            Toast.makeText(getApplicationContext(), "Error! No note id passed", Toast.LENGTH_SHORT).show();
            finish();
        }

        id = getIntent().getLongExtra("id", -1);
        loadNote();
    }

    private void loadNote() {
        SQLiteDatabase db = App.mDBHelper.getReadableDatabase();

        String[] projection = {
                BaseColumns._ID,
                Diary.Entry.COLUMN_NAME_TITLE,
                Diary.Entry.COLUMN_NAME_TEXT,
                Diary.Entry.COLUMN_NAME_DATE,
                Diary.Entry.COLUMN_NAME_PATH,
        };

        String sortOrder = Diary.Entry.COLUMN_NAME_DATE + " DESC";

        String selection = Diary.Entry._ID + " LIKE ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = db.query(
                Diary.Entry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );

        while (cursor.moveToNext()) {
//            id = cursor.getLong(cursor.getColumnIndexOrThrow(Diary.Entry._ID));
            title = cursor.getString(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_TITLE));
            initialTitle = title;
            initialText = cursor.getString(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_TEXT));
            path = cursor.getString(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_PATH));
        }
        cursor.close();

        text_tv.setText(initialText);
        setTitle(title);
    }

    private void saveNote() {
        String text = text_tv.getEditableText().toString();

        // no need to save note if it wasn't changed
        if (text.equals(initialText) & title.equals(initialTitle))
            return;

        long date = System.currentTimeMillis();

        SQLiteDatabase db = App.mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Diary.Entry.COLUMN_NAME_TITLE, title);
        values.put(Diary.Entry.COLUMN_NAME_TEXT, text);
        values.put(Diary.Entry.COLUMN_NAME_DATE, date);
        values.put(Diary.Entry.COLUMN_NAME_PATH, path);

        String selection = Diary.Entry._ID + " LIKE ?";
        String[] selectionArgs = {String.valueOf(id)};

        db.update(Diary.Entry.TABLE_NAME, values, selection, selectionArgs);
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
    }

    private void deleteNote() {
        SQLiteDatabase db = App.mDBHelper.getWritableDatabase();

        String selection = Diary.Entry._ID + " LIKE ?";
        String[] selectionArgs = {String.valueOf(id)};

        db.delete(Diary.Entry.TABLE_NAME, selection, selectionArgs);
        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNote();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_editor_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.delete: {
                new AlertDialog.Builder(this).setMessage("Are you sure you want to delete this note?").setPositiveButton("Yes", (dialog, which) -> {
                    deleteNote();
                    finish();
                }).setCancelable(true).setNeutralButton("No", null).show();
                break;
            }
            case R.id.rename: {
                EditText input = new EditText(this);
                input.setText(title);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                new AlertDialog.Builder(this).setTitle("Change note title").setView(input).setPositiveButton("Save", (dialog, which) -> {
                    title = input.getEditableText().toString();
                    setTitle(title);
                }).setNegativeButton("Cancel", null).show();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
