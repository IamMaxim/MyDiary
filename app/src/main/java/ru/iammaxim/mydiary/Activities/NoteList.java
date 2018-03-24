package ru.iammaxim.mydiary.Activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;

import ru.iammaxim.mydiary.App;
import ru.iammaxim.mydiary.DB.DBHelper;
import ru.iammaxim.mydiary.DB.Diary;
import ru.iammaxim.mydiary.R;

public class NoteList extends AppCompatActivity {
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy HH:mm:ss");
    NoteListAdapter adapter;
    private String passedPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);
        passedPath = getIntent().getStringExtra("path");
        if (passedPath == null)
            passedPath = "";
        if (!passedPath.isEmpty()) {
            setTitle(passedPath);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // init DB
        App.mDBHelper = new DBHelper(this);

        findViewById(R.id.fab).setOnClickListener(b -> createNewNote("New note", "", passedPath));

        adapter = new NoteListAdapter();
        RecyclerView rv = findViewById(R.id.rv);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDB();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createNewNote(String title, String text, String path) {
        long date = System.currentTimeMillis();

        SQLiteDatabase db = App.mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Diary.Entry.COLUMN_NAME_TITLE, title);
        values.put(Diary.Entry.COLUMN_NAME_TEXT, text);
        values.put(Diary.Entry.COLUMN_NAME_DATE, date);
        values.put(Diary.Entry.COLUMN_NAME_PATH, path);

        long newRowId = db.insert(Diary.Entry.TABLE_NAME, null, values);
        openNote(newRowId);
    }

    @Override
    protected void onDestroy() {
        App.mDBHelper.close();
        super.onDestroy();
    }

    private void loadDB() {
        SQLiteDatabase db = App.mDBHelper.getReadableDatabase();

        String[] projection = {
                BaseColumns._ID,
                Diary.Entry.COLUMN_NAME_TITLE,
                Diary.Entry.COLUMN_NAME_TEXT,
                Diary.Entry.COLUMN_NAME_DATE,
                Diary.Entry.COLUMN_NAME_PATH,
        };

        String sortOrder = Diary.Entry.COLUMN_NAME_DATE + " DESC";
        String selection = Diary.Entry.COLUMN_NAME_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{passedPath + "%"};

        Cursor cursor = db.query(
                Diary.Entry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );

        // clear adapter before loading it again
        adapter.entries.clear();
        adapter.expandableEntries.clear();

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(Diary.Entry._ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_TITLE));
            String text = cursor.getString(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_TEXT));
            long date = cursor.getLong(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_DATE));
            String path = cursor.getString(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_PATH));

            if (!path.isEmpty() && path.length() > passedPath.length()) {
                String[] tokens = path.substring(Math.min(path.length(), passedPath.isEmpty() ? 0 : (passedPath.length() + 1))).split("/");
                String newPath = (passedPath.isEmpty() ? "" : (passedPath + "/")) + tokens[0];
                // if path is empty, do not add it
                if (tokens.length == 1 && tokens[0].equals("")) {
                    continue;
                }
                // skip adding path if note with such path already added
                if (adapter.expandableEntries.contains(newPath))
                    continue;
                adapter.entries.add(new ExpandableEntry(tokens[0], newPath));
                adapter.expandableEntries.add(newPath);
            } else {
                Entry entry = new NoteEntry(id, title, text, date, path);
                adapter.entries.add(entry);
            }
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    public class Entry {
    }

    public class ExpandableEntry extends Entry {
        public String name, path;

        public ExpandableEntry(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    public class NoteEntry extends Entry {
        public NoteEntry(long id, String title, String text, long date, String path) {
            this.id = id;
            this.title = title;
            this.text = text;
            this.date = date;
            this.path = path;
        }

        public long id;

        public String title;
        public String text;
        public String path;
        public long date;
    }

    public void openNote(long id) {
        Intent intent = new Intent(NoteList.this, NoteEditor.class);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    public class NoteListAdapter extends RecyclerView.Adapter implements ItemClickListener {
        public ArrayList<Entry> entries = new ArrayList<>();
        public HashSet<String> expandableEntries = new HashSet<>();

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case EntryType.NOTE: {
                    View view = LayoutInflater.from(NoteList.this).inflate(R.layout.note_list_entry, parent, false);
                    return new NoteViewHolder(view);
                }
                case EntryType.EXPANDABLE: {
//                    View view = new ExpandableEntryView(NoteList.this);
                    View view = LayoutInflater.from(NoteList.this).inflate(R.layout.expandable_list_entry, parent, false);
                    return new ExpandableViewHolder(view);
                }
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Entry entry = entries.get(position);
            int viewType = getItemViewType(position);

            if (viewType == EntryType.NOTE) {
                NoteEntry ne = (NoteEntry) entry;
                NoteViewHolder nvh = (NoteViewHolder) holder;
                nvh.title.setId(position);
                nvh.title.setText(ne.title);
                nvh.text.setText(ne.text);
                nvh.date.setText(sdf.format(ne.date));
            } else if (viewType == EntryType.EXPANDABLE) {
                ExpandableEntry ee = (ExpandableEntry) entry;
                ExpandableViewHolder evh = (ExpandableViewHolder) holder;
                evh.name.setText(ee.name);
            }
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        @Override
        public int getItemViewType(int position) {
            Entry e = entries.get(position);
            if (e instanceof NoteEntry)
                return EntryType.NOTE;
            else if (e instanceof ExpandableEntry)
                return EntryType.EXPANDABLE;
            return -1;
        }

        @Override
        public void onClick(View v, int position) {
            Entry entry = adapter.entries.get(position);
            int viewType = getItemViewType(position);

            if (viewType == EntryType.NOTE)
                openNote(((NoteEntry) entry).id);
            else if (viewType == EntryType.EXPANDABLE) {
                ExpandableEntry ee = (ExpandableEntry) entry;
                Intent intent = new Intent(NoteList.this, NoteList.class);
                intent.putExtra("path", ee.path);
                startActivity(intent);
            }
        }

        class NoteViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView title, date, text;

            NoteViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                title = itemView.findViewById(R.id.title);
                date = itemView.findViewById(R.id.date);
                text = itemView.findViewById(R.id.text);
            }

            @Override
            public void onClick(View v) {
                NoteListAdapter.this.onClick(v, getAdapterPosition());
            }
        }

        class ExpandableViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public TextView name;

            public ExpandableViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                name = itemView.findViewById(R.id.name);
            }

            @Override
            public void onClick(View v) {
                NoteListAdapter.this.onClick(v, getAdapterPosition());
            }
        }
    }

    public interface ItemClickListener {
        void onClick(View v, int position);
    }

    public static final class EntryType {
        public static final int NOTE = 0,
                EXPANDABLE = 1;
    }
}
