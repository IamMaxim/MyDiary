package ru.iammaxim.mydiary;

import android.app.Application;

import ru.iammaxim.mydiary.DB.DBHelper;

/**
 * Created by maxim on 3/19/18.
 */

public class App extends Application {
    public static DBHelper mDBHelper;

    @Override
    public void onTerminate() {
        super.onTerminate();
        mDBHelper.close();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDBHelper = new DBHelper(this);
    }
}
