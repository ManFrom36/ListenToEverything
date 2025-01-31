package com.mf36.listentoeverything;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

class Global {
    static SharedPreferences prefs;
    static Context context;

    static void init(Context ctx) {
        prefs = ctx.getSharedPreferences("database", Context.MODE_PRIVATE);
        context = ctx;
    }

    static void toast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    static String getResString(int resourceId) {
        return context.getResources().getString(resourceId);
    }

}
