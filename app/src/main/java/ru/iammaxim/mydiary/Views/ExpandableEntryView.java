package ru.iammaxim.mydiary.Views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import ru.iammaxim.mydiary.R;

/**
 * Created by maxim on 3/19/18.
 */

public class ExpandableEntryView extends LinearLayout {
    public ExpandableEntryView(Context context) {
        this(context, null);
    }

    public ExpandableEntryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        addView(LayoutInflater.from(context).inflate(R.layout.expandable_entry_header, this, false));
        addView(LayoutInflater.from(context).inflate(R.layout.expandable_entry_content, this, false));
    }
}
