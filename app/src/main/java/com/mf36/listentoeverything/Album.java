package com.mf36.listentoeverything;


import static com.mf36.listentoeverything.Global.toast;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.UUID;

//"test, \"sod,sod\"".split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)")

public class Album {
    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT = 1;

    public static final int STATUS_NOT_LISTEN = 0;
    public static final int STATUS_LISTEN = 1;
    public static final int STATUS_IN_PROCESS = 2;

    public String id = "";
    public String artist;
    public String year;
    public String title;
    public int status = STATUS_NOT_LISTEN;
    public String listenDate;
    public float rating = 0;

    @NonNull
    public String toString() {
        return String.format("%s\001%s\001%s\001%s\001%s\001%s", artist, year, title, status, listenDate, rating);
    }

    public Album () {
        clear();
    }

    //----------------------------------------------------------------------------------------------
    // clear album and set it's defaults
    public void clear(){
        id = UUID.randomUUID().toString();
        artist = "";
        title = "";
        year = "";
        listenDate = "";
        status = STATUS_NOT_LISTEN;
        rating = 0;
    }

    //----------------------------------------------------------------------------------------------
    // load album from shared preferences file
    public void load(String id, SharedPreferences prefs){
        String value = prefs.getString(id,"");
        load(id, value);
    }

    //----------------------------------------------------------------------------------------------
    // delete album from shared preferences file
    public void delete(SharedPreferences prefs){
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(id);
        editor.apply();
    }

    //----------------------------------------------------------------------------------------------
    // load album from string
    public void load(String id, String values){
        this.id = id;
        String [] vals = values.split("\001");
        if (vals.length > 0)
            artist = vals[0];
        if (vals.length > 1)
            year = vals[1];
        if (vals.length > 2)
            title = vals[2];
        if (vals.length > 3)
            status = Integer.parseInt(vals[3]);
        if (vals.length > 4)
            listenDate = vals[4];
        if (vals.length > 5)
            rating = Float.parseFloat(vals[5]);

    }

    //----------------------------------------------------------------------------------------------
    int check(){

        if (artist.trim().isEmpty()) {
            toast("Заполните исполнителя");
            return 1;
        }

        if (year.trim().isEmpty()) {
            toast("Заполните год");
            return 2;
        }

        if (title.trim().isEmpty()) {
            toast("Заполните альбом");
            return 3;
        }

        return 0;
    }

    //----------------------------------------------------------------------------------------------
    public void save(SharedPreferences prefs){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(id, this.toString());
        editor.apply();
    }

    //----------------------------------------------------------------------------------------------
    public String getText(){
        return String.format("%s %s %s", artist, year, title);
    }

}
