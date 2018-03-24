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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import ru.iammaxim.mydiary.App;
import ru.iammaxim.mydiary.DB.DBHelper;
import ru.iammaxim.mydiary.DB.Diary;
import ru.iammaxim.mydiary.R;
import ru.iammaxim.mydiary.Views.ExpandableEntryView;

public class NoteList extends AppCompatActivity {
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy HH:mm:ss");
    NoteListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        // init DB
        App.mDBHelper = new DBHelper(this);

        findViewById(R.id.fab).setOnClickListener(b -> createNewNote("New note", "", ""));

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
        System.out.println("loading db");
        SQLiteDatabase db = App.mDBHelper.getReadableDatabase();

        String[] projection = {
                BaseColumns._ID,
                Diary.Entry.COLUMN_NAME_TITLE,
                Diary.Entry.COLUMN_NAME_TEXT,
                Diary.Entry.COLUMN_NAME_DATE,
                Diary.Entry.COLUMN_NAME_PATH,
        };

        String sortOrder = Diary.Entry.COLUMN_NAME_DATE + " DESC";

        Cursor cursor = db.query(
                Diary.Entry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        adapter.entries.clear();
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(Diary.Entry._ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_TITLE));
            String text = cursor.getString(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_TEXT));
            long date = cursor.getLong(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_DATE));
            String path = cursor.getString(cursor.getColumnIndexOrThrow(Diary.Entry.COLUMN_NAME_PATH));

            if (!path.isEmpty()) {
                String[] tokens = path.split("/");
                ExpandableEntry e = adapter.expandableEntries.get(tokens[0]);
                if (e == null) {
                    e = new ExpandableEntry(tokens[0]);
                    adapter.expandableEntries.put(tokens[0], e);
                    adapter.entries.add(e);
                }
                for (int i = 1; i < tokens.length; i++) {
                    ExpandableEntry e1 = e.expandableEntries.get(tokens[i]);
                    if (e1 == null) {
                        e1 = new ExpandableEntry(tokens[i]);
                        e.expandableEntries.put(tokens[i], e1);
                        e.entries.add(e1);
                    }
                    e = e1;
                }
            }

            Entry entry = new NoteEntry(id, title, text, date, path);
            adapter.entries.add(entry);
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    public class Entry {
    }

    public class ExpandableEntry extends Entry {
        public String name;
        public ArrayList<Entry> entries = new ArrayList<>();
        public HashMap<String, ExpandableEntry> expandableEntries = new HashMap<>();

        public ExpandableEntry(String name) {
            this.name = name;
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
        public HashMap<String, ExpandableEntry> expandableEntries = new HashMap<>();

        @Override
        public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case EntryType.NOTE:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_list_entry, parent, false);
                    break;
                case EntryType.EXPANDABLE:
                    view = new ExpandableEntryView(NoteList.this);
                    break;
            }
            return new NoteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Entry entry = entries.get(position);
            int viewType = getItemViewType(position);

            if (viewType == EntryType.NOTE) {
                NoteViewHolder nvh = (NoteViewHolder) holder;
                NoteEntry ne = (NoteEntry) entry;
                nvh.title.setId(position);
                nvh.title.setText(ne.title);
                nvh.text.setText(ne.text);
                nvh.date.setText(sdf.format(ne.date));
            } else if (viewType == EntryType.EXPANDABLE) {
                ExpandableViewHolder evh = (ExpandableViewHolder) holder;
                ExpandableEntry ee = (ExpandableEntry) entry;
                evh.name.setText(ee.name);
//                ((NoteListAdapter) evh.rv.getAdapter()).;
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
            public RecyclerView rv;

            public ExpandableViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                name = itemView.findViewById(R.id.name);
                rv = itemView.findViewById(R.id.rv);
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
