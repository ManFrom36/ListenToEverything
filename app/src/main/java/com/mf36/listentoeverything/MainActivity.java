package com.mf36.listentoeverything;

import static com.mf36.listentoeverything.Album.MODE_EDIT;
import static com.mf36.listentoeverything.Album.STATUS_IN_PROCESS;
import static com.mf36.listentoeverything.Album.STATUS_LISTEN;
import static com.mf36.listentoeverything.Album.MODE_CREATE;
import static com.mf36.listentoeverything.Album.STATUS_NOT_LISTEN;
import static com.mf36.listentoeverything.AlbumComparator.BY_NAME;
import static com.mf36.listentoeverything.AlbumComparator.BY_RATING;
import static com.mf36.listentoeverything.AlbumComparator.BY_YEAR;
import static com.mf36.listentoeverything.Global.getResString;
import static com.mf36.listentoeverything.Global.prefs;
import static com.mf36.listentoeverything.Global.toast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.preference.PreferenceManager;

import android.provider.DocumentsContract;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.mf36.listentoeverything.databinding.ActivityMainBinding;

import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    final int CREATE_FILE_CODE = 1;
    final int OPEN_FILE_CODE = 2;

    public final int MODE_ALL = -1;
    public final int MODE_LISTEN = 1;
    public final int MODE_NOT_LISTEN = 0;

    static AlbumAdapter albumAdapter;
    Button ibAddAlbum;
    Button ibGetRandom;
    Button ibMenuSort;
    Button ibMenuMain;
    ListView listAlbums;
    ToggleButton btnAll;
    ToggleButton btnListen;
    ToggleButton btnNotListen;
    TextView tvStatistics;

    @SuppressLint("RestrictedApi")
    MenuPopupHelper sortMenu;
    @SuppressLint("RestrictedApi")
    MenuPopupHelper mainMenu;

    int viewMode = MODE_ALL;

    // sort options
    public int sortType = BY_NAME;
    public boolean sortAsc = true;

    //----------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.lte_gray)); //status bar or the time bar at the top (see example image1)
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.lte_gray)); // Navigation bar the soft bottom of some phones like nexus and some Samsung note series  (see example image2)

        Global.init(getApplicationContext());
        initViews();

        albumAdapter = new AlbumAdapter();
        albumAdapter.load();
        listAlbums.setAdapter(albumAdapter);
        initListViewListeners();
        restoreSettings();
        loadList();
    }

    //----------------------------------------------------------------------------------------------
    private void completeListen(Album currentAlbum) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ask_listen_complete)
                .setMessage(currentAlbum.getText())
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                    currentAlbum.status = STATUS_LISTEN;
                    currentAlbum.listenDate = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
                    currentAlbum.save(Global.prefs);
                    loadList();
                })
                .setNegativeButton(android.R.string.no, (dialog12, which) -> dialog12.dismiss())
                .create();
        dialog.show();
    }

    //----------------------------------------------------------------------------------------------
    private void relisten(Album currentAlbum) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ask_relisten)
                .setMessage(currentAlbum.getText())
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                    currentAlbum.status = STATUS_NOT_LISTEN;
                    currentAlbum.save(Global.prefs);
                    loadList();
                })
                .setNegativeButton(android.R.string.no, (dialog12, which) -> dialog12.dismiss())
                .create();
        dialog.show();
    }

    //----------------------------------------------------------------------------------------------
    private void relistenAll() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(android.R.string.dialog_alert_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.ask_relistenAll)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                    for (int i = 0; i < albumAdapter.getAllCount() - 1; i++) {
                        albumAdapter.getItemFromAll(i).status = STATUS_NOT_LISTEN;
                        albumAdapter.getItemFromAll(i).save(prefs);
                    }
                    loadList();
                })
                .setNegativeButton(android.R.string.no, (dialog12, which) -> dialog12.dismiss())
                .create();
        dialog.show();
    }

    //----------------------------------------------------------------------------------------------
    private void startListen(Album currentAlbum) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ask_listen)
                .setMessage(currentAlbum.getText())
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                    currentAlbum.status = STATUS_IN_PROCESS;
                    currentAlbum.save(Global.prefs);
                    loadList();
                })
                .setNegativeButton(android.R.string.no, (dialog12, which) -> dialog12.dismiss())
                .create();
        dialog.show();
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.ibAddAlbum) {
            createAlbum();
        } else if (v.getId() == R.id.ibMenuSort) {
            showSortMenu();
        } else if (v.getId() == R.id.ibMenuMain) {
            showMainMenu();
        } else if (v.getId() == R.id.ibGetRandom) {
            Album album = albumAdapter.getRandom();
            if (album == null) {
                    toast(getResString(R.string.list_empty));
                    return;
                }
            startListen(album);
        }

    }

    //----------------------------------------------------------------------------------------------
    void initViews(){
        ibAddAlbum = findViewById(R.id.ibAddAlbum);
        ibAddAlbum.setOnClickListener(this);
        ibGetRandom  = findViewById(R.id.ibGetRandom);
        ibGetRandom.setOnClickListener(this);
        ibMenuSort = findViewById(R.id.ibMenuSort);
        ibMenuSort.setOnClickListener(this);
        ibMenuMain = findViewById(R.id.ibMenuMain);
        ibMenuMain.setOnClickListener(this);

        listAlbums = findViewById(R.id.listAlbums);
        btnAll = findViewById(R.id.btnAll);
        btnListen = findViewById(R.id.btnListen);
        btnNotListen = findViewById(R.id.btnNotListen);
        btnAll.setOnClickListener(this::onModeClick);
        btnListen.setOnClickListener(this::onModeClick);
        btnNotListen.setOnClickListener(this::onModeClick);
        tvStatistics = findViewById(R.id.tvStat);
    }

    //----------------------------------------------------------------------------------------------
    public void onModeClick(View v) {

        if (v.getId() == R.id.btnAll) {
            viewMode = MODE_ALL;
        } else if (v.getId() == R.id.btnListen) {
            viewMode = MODE_LISTEN;
        } else if (v.getId() == R.id.btnNotListen) {
            viewMode = MODE_NOT_LISTEN;
        }

        btnAll.setChecked(viewMode == MODE_ALL);
        btnListen.setChecked(viewMode == MODE_LISTEN);
        btnNotListen.setChecked(viewMode == MODE_NOT_LISTEN);

        albumAdapter.getFilter().filter(String.valueOf(viewMode));
    }

    //----------------------------------------------------------------------------------------------
    final ActivityResultLauncher<Intent> albumActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadList();
                }
            });

    //----------------------------------------------------------------------------------------------
    void createAlbum(){
        Intent intent = new Intent(this, AlbumActivity.class);
        intent.putExtra("mode", MODE_CREATE);
        albumActivityResultLauncher.launch(intent);
    }

    //----------------------------------------------------------------------------------------------
    void editAlbum(Album album){
        Intent intent = new Intent(this, AlbumActivity.class);
        intent.putExtra("mode", MODE_EDIT);
        intent.putExtra("id", album.id);
        albumActivityResultLauncher.launch(intent);
    }

    //----------------------------------------------------------------------------------------------
    void loadList() {
        albumAdapter.sort(sortType, sortAsc);
        albumAdapter.getFilter().filter(String.valueOf(viewMode));

        String stat;
        int listen = albumAdapter.getCount(STATUS_LISTEN);
        if (listen == 0)
            stat = "Всего " +  albumAdapter.getAllCount();
        else
            stat = String.format(getResString(R.string.statistics),
                    listen,
                    (int) (listen / (float) albumAdapter.getAllCount() * 100),
                    albumAdapter.getAllCount()
                );

        tvStatistics.setText(stat);
    }

    //----------------------------------------------------------------------------------------------
    void initListViewListeners(){

        listAlbums.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            Album currentAlbum = (Album) listAlbums.getItemAtPosition(position);

            if (currentAlbum == null)
                return;

            if (currentAlbum.status == STATUS_NOT_LISTEN)
                startListen(currentAlbum);

            if (currentAlbum.status == STATUS_IN_PROCESS)
                completeListen(currentAlbum);

            if (currentAlbum.status == STATUS_LISTEN)
                relisten(currentAlbum);

        });

        registerForContextMenu(listAlbums);
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.listAlbums) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.album_menu, menu);
        }
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        assert info != null;
        int position = info.position;
        Album a = (Album) albumAdapter.getItem(position);
        int id = item.getItemId();

        if (id == R.id.menu_edit) {
            editAlbum(a);
            return true;
        } else if (id == R.id.menu_delete) {
            delete(a);
            return true;
        } else if (id == R.id.menu_discogs) {
            searchOnDiscogs(a);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    //----------------------------------------------------------------------------------------------
    private void searchOnDiscogs(Album a) {
        //https://www.discogs.com/search?q=sodom+1987+agent+orange&type=all
        Uri webpage = Uri.parse("https://www.discogs.com/search?q=" + a.artist + "+" + a.title.replace(" ","+"));
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    //----------------------------------------------------------------------------------------------
    void delete(Album album) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ask_delete)
                .setMessage(album.getText())
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                    albumAdapter.remove(album);
                    loadList();
                })
                .setNegativeButton(android.R.string.no, (dialog12, which) -> dialog12.dismiss())
                .create();
        dialog.show();
    }

    //----------------------------------------------------------------------------------------------
    @SuppressLint("RestrictedApi")
    protected void showSortMenu(){

        MenuBuilder menuBuilder;
        menuBuilder = new MenuBuilder(this);

        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.sort_menu, menuBuilder);
        sortMenu = new MenuPopupHelper(this, menuBuilder, ibMenuSort);
        sortMenu.setForceShowIcon(true);

        int positionOfMenuItem = sortType;

        MenuItem item = menuBuilder.getItem(positionOfMenuItem);

        SpannableString s = new SpannableString(item.getTitle());
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, s.length(), 0);
        item.setTitle(s);

        if (sortAsc)
            item.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_arrow_drop_up_24, null));
        else
            item.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_arrow_drop_down_24, null));

        menuBuilder.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {

                int itemId = item.getItemId();
                if (itemId == R.id.sortByName) {
                    if (sortType == BY_NAME)
                        sortAsc = !sortAsc;
                    sortType = BY_NAME;
                } else if (itemId == R.id.sortByDate) {
                    if (sortType == BY_YEAR)
                        sortAsc = !sortAsc;
                    sortType = BY_YEAR;
                } else if (itemId == R.id.sortByRating) {
                    if (sortType == BY_RATING)
                        sortAsc = !sortAsc;
                    sortType = BY_RATING;
                } else {
                    return false;
                }

                loadList();

                return true;
            }

            @Override
            public void onMenuModeChange(@NonNull MenuBuilder menu) {}
        });

        sortMenu.show();

    }

    //----------------------------------------------------------------------------------------------
    @SuppressLint("RestrictedApi")
    protected void showMainMenu(){

        MenuBuilder menuBuilder;
        menuBuilder = new MenuBuilder(this);

        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main_menu, menuBuilder);
        mainMenu = new MenuPopupHelper(this, menuBuilder, ibMenuMain);
        mainMenu.setForceShowIcon(true);

        menuBuilder.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {

                int itemId = item.getItemId();
                if (itemId == R.id.menu_relistenAll) {
                    relistenAll();
                    return true;
                } else if (itemId == R.id.menu_backup) {
                    backupCollection();
                    return true;
                } else if (itemId == R.id.menu_restore) {
                    askRestoreCollection();
                    return true;
                } else if (itemId == R.id.menu_about) {
                    showAbout();
                    return true;
                } else {
                    return false;
                }

            }

            @Override
            public void onMenuModeChange(@NonNull MenuBuilder menu) {}
        });

        mainMenu.show();


    }

    //----------------------------------------------------------------------------------------------
    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_about)
                .setMessage(R.string.about_text)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog12, which) -> dialog12.dismiss())
                .create()
                .show();
    }

    //----------------------------------------------------------------------------------------------
    void backupSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("sortAsc", sortAsc);
        editor.putInt("sortType", sortType);
        editor.putInt("viewMode", viewMode);
        editor.apply();
    }

    //----------------------------------------------------------------------------------------------
    void restoreSettings(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sortAsc = prefs.getBoolean("sortAsc", true);
        sortType = prefs.getInt("sortType", BY_NAME);
        viewMode = prefs.getInt("viewMode", MODE_ALL);

        btnAll.setChecked(viewMode == MODE_ALL);
        btnListen.setChecked(viewMode == MODE_LISTEN);
        btnNotListen.setChecked(viewMode == MODE_NOT_LISTEN);
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void onStop() {
        backupSettings();
        super.onStop();
    }

    //----------------------------------------------------------------------------------------------
    private void backupCollection() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/text");
        intent.putExtra(Intent.EXTRA_TITLE, "listen.txt");
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, (Uri) null);
        startActivityForResult(intent, CREATE_FILE_CODE);
    }

    //----------------------------------------------------------------------------------------------
    private void restoreCollection() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.setType("text/*");
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, (Uri) null);
        startActivityForResult(intent, OPEN_FILE_CODE);
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Uri uri;
        if (requestCode == CREATE_FILE_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                uri = resultData.getData();
                saveToFile(uri);
            }
        }
        if (requestCode == OPEN_FILE_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                uri = resultData.getData();
                restoreFromFile(uri);
                albumAdapter.load();
                loadList();
            }
        }

    }

    //----------------------------------------------------------------------------------------------
    void saveToFile(Uri uri) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(getContentResolver().openOutputStream(uri));
            oos.writeObject(prefs.getAll());
            oos.close();
        } catch (IOException e) {
            toast("Error! " + e.getMessage());
        }

    }

    void restoreFromFile(Uri uri) {
        try {
            ObjectInputStream ois = new ObjectInputStream(getContentResolver().openInputStream(uri));
            Map <String, Object> map = (Map) ois.readObject();
            if (map.isEmpty())
                return;
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (e.getValue() instanceof Boolean) {
                    editor.putBoolean(e.getKey(), (Boolean)e.getValue());
                } else if (e.getValue() instanceof String) {
                    editor.putString(e.getKey(), (String)e.getValue());
                } else if (e.getValue() instanceof Integer) {
                    editor.putInt(e.getKey(), (int)e.getValue());
                } else if (e.getValue() instanceof Float) {
                    editor.putFloat(e.getKey(), (float)e.getValue());
                } else if (e.getValue() instanceof Long) {
                    editor.putLong(e.getKey(), (Long) e.getValue());
                } else if (e.getValue() instanceof Set) {
                    editor.putStringSet(e.getKey(), (Set<String>) e.getValue());
                } else {
                    throw new IllegalArgumentException("Type " + e.getValue().getClass().getName() + " is unknown");
            }
        }
        editor.apply();

        } catch (IOException e) {
            toast("Error occured: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    //----------------------------------------------------------------------------------------------
    void askRestoreCollection() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ask_restore_title)
                .setMessage(R.string.ask_restore_msg)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog1, which) -> restoreCollection())
                .setNegativeButton(android.R.string.no, (dialog12, which) -> dialog12.dismiss())
                .create();
        dialog.show();
    }

}

