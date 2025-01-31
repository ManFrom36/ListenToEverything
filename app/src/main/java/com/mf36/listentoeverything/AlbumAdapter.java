package com.mf36.listentoeverything;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mf36.listentoeverything.Album.STATUS_IN_PROCESS;
import static com.mf36.listentoeverything.Album.STATUS_LISTEN;
import static com.mf36.listentoeverything.Album.STATUS_NOT_LISTEN;
import static com.mf36.listentoeverything.Global.context;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

class AlbumHolder {
    public TextView tvArtist;
    public TextView tvTitle;
    public TextView tvListenDate;
    public ImageView imPhones;
    public RatingBar rbAlbum;
}

class AlbumAdapter extends BaseAdapter implements Filterable {
    // album lists
    private final List<Album> allAlbums = new ArrayList<>();
    private List<Album> filteredAlbums = new ArrayList<>();

    public void load(){
        allAlbums.clear();
        Map<String, ?> allEntries = Global.prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Album album = new Album();
            album.load(entry.getKey(), entry.getValue().toString());
            add(album);
        }
    }

    //----------------------------------------------------------------------------------------------
    public void add(Album album) {
        allAlbums.add(album);
    }

    //----------------------------------------------------------------------------------------------
    public void addNew(Album album) {
        Album newAlbum = new Album();
        newAlbum.id = album.id;
        allAlbums.add(newAlbum);
        update(album.id, album);
    }

    //----------------------------------------------------------------------------------------------
    public void update(String id, Album source) {
        Album album = findById(id);
        if (album == null)
            return;
        album.artist = source.artist;
        album.title = source.title;
        album.year = source.year;
        album.rating = source.rating;
    }

    //----------------------------------------------------------------------------------------------
    public void remove(Album album) {
        album.delete(Global.prefs);
        allAlbums.remove(album);
        filteredAlbums.remove(album);
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public int getCount() {
        return filteredAlbums.size();
    }

    //----------------------------------------------------------------------------------------------
    public int getAllCount() {
        return allAlbums.size();
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public Object getItem(int position) {
        return filteredAlbums.get(position);
    }

    //----------------------------------------------------------------------------------------------
    public Album getItemFromAll(int position) {
        return allAlbums.get(position);
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public long getItemId(int position) {
        return position;
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AlbumHolder holder = new AlbumHolder();
        final Album album  = filteredAlbums.get(position);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        try {
            assert inflater != null;
            convertView = inflater.inflate(R.layout.album, null);
        } catch (NullPointerException e) {
            Log.w(e.getMessage(), e);
            return null;
        }

        holder.tvArtist = convertView.findViewById(R.id.tvArtist);
        holder.tvTitle = convertView.findViewById(R.id.tvCaption);
        holder.imPhones = convertView.findViewById(R.id.imPhones);
        holder.tvListenDate = convertView.findViewById(R.id.tvListenDate);
        holder.rbAlbum = convertView.findViewById(R.id.rbAlbum);

        holder.rbAlbum.setRating(album.rating);

        holder.tvArtist.setText(String.format("%s  [%s]", album.artist, album.year));
        holder.tvTitle.setText(album.title);

        holder.imPhones.setVisibility(album.status == STATUS_IN_PROCESS ? VISIBLE : GONE);

        holder.tvListenDate.setText(album.listenDate);
        holder.tvListenDate.setVisibility(album.status == STATUS_LISTEN ? VISIBLE : GONE);

        return convertView;
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredAlbums = (List<Album>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                filteredAlbums.clear();

                for (int i = 0; i < allAlbums.size(); i++) {
                    Album alb = allAlbums.get(i);
                    if ((alb.status == Integer.parseInt (constraint.toString())) || constraint.equals("-1"))
                        filteredAlbums.add(alb);
                    else if ((alb.status == STATUS_IN_PROCESS) && constraint.toString().equals("0"))
                        filteredAlbums.add(alb);
                }

                results.count = filteredAlbums.size();
                results.values = filteredAlbums;

                return results;
            }
        };

        return filter;
    }

    //----------------------------------------------------------------------------------------------
    private String createDuplicateMask(Album source) {
        return (source.artist + source.title)
                .replace(" ", "")
                .replace(".", "")
                .replace(",", "")
                .replace("\"", "")
                .replace("'", "")
                .replace("-", "")
                .toLowerCase();
    }

    //----------------------------------------------------------------------------------------------
    public Album findDuplicate(Album source) {
        String sourceMask = createDuplicateMask(source);

        for (Album current : allAlbums)
            if (sourceMask.equals(createDuplicateMask(current)) && !source.id.equals(current.id))
                    return current;

        return null;
    }

    //----------------------------------------------------------------------------------------------
    public Album findById(String id) {

        for (Album current : allAlbums)
            if (current.id.equals(id))
                return current;

        return null;
    }

    //----------------------------------------------------------------------------------------------
    public Album getRandom(){
        ArrayList<String> list = new ArrayList<>();
        for (Album album : allAlbums) {
            if (album.status == STATUS_NOT_LISTEN)
                list.add(album.id);
        }

        int size = list.size();
        if (size > 0)
            return findById(list.get(new Random().nextInt((size))));
        else
            return null;

    }

    //----------------------------------------------------------------------------------------------
    public void sort(int sortType, boolean sortAsc) {
        allAlbums.sort(new AlbumComparator(sortType, sortAsc));
    }

    //----------------------------------------------------------------------------------------------
    public int getCount(int status) {
        int result = 0;
        for (Album a : allAlbums)
            if ((a.status == -1) || (a.status == status))
                result ++;
        return result;
    }

}
