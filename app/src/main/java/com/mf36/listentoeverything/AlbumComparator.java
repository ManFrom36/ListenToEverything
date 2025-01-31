package com.mf36.listentoeverything;

import android.annotation.SuppressLint;

import java.util.Comparator;

public class AlbumComparator implements Comparator<Album> {

    // вариант сортировки
    public static final int BY_NAME = 0;
    public static final int BY_YEAR = 1;
    public static final int BY_RATING = 2;

    public AlbumComparator(int sortType, boolean sortAsc) {
        this.sortType = sortType;
        this.sortAsc = sortAsc;
    }

    public final int sortType;
    public final boolean sortAsc;

    //----------------------------------------------------------------------------------------------
    @SuppressLint("DefaultLocale")
    public int compare(Album left, Album right) {
        int compareResult = 0;

        String lPart = left.getText().toUpperCase().replace(" ","");
        String rPart = right.getText().toUpperCase().replace(" ","");

        // by artist, year and album
        if (sortType == BY_NAME)
            compareResult = lPart.compareTo(rPart);

        // by year
        if (sortType == BY_YEAR)
            compareResult = (left.year + lPart).compareTo(right.year + rPart);

        if (sortType == BY_RATING)
            compareResult = (left.rating + lPart).compareTo(right.rating + rPart);

        return sortAsc ? compareResult : -compareResult;

    }

}
