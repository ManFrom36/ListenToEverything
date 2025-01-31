package com.mf36.listentoeverything;

import static android.view.View.GONE;
import static com.mf36.listentoeverything.Album.MODE_CREATE;
import static com.mf36.listentoeverything.Album.MODE_EDIT;
import static com.mf36.listentoeverything.Global.getResString;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class AlbumActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnOk;
    Button btnCancel;
    EditText edArtist;
    EditText edYear;
    EditText edTitle;
    TextView tvCaption;
    TextView tvInfo;
    RatingBar rbRating;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch swMulti;
    final Album album = new Album();
    int mode;

    //----------------------------------------------------------------------------------------------
    void initViews(){
        tvCaption = findViewById(R.id.tvCaption);
        tvInfo = findViewById(R.id.tvInfo);
        btnOk = findViewById(R.id.btnOk);
        btnCancel = findViewById(R.id.btnCancel);
        btnOk.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        edArtist = findViewById(R.id.edArtist);
        edYear = findViewById(R.id.edYear);
        edTitle = findViewById(R.id.edAlbum);
        swMulti = findViewById(R.id.swMultiEnter);
        rbRating = findViewById(R.id.ratingBar);
        // in edit mode dont show multiple creation switch
        if (mode == MODE_EDIT) {
            swMulti.setVisibility(GONE);
            tvCaption.setText(getResString(R.string.edit_album));
            btnOk.setText(R.string.save);
        }

        edTitle.setOnEditorActionListener(  (v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE))
                saveAlbum();
            return false;
        });

    }

    //----------------------------------------------------------------------------------------------
    void initValues() {
        album.clear();
        if (mode == MODE_EDIT) {
            album.load(getIntent().getStringExtra("id"), Global.prefs);
            if (album.listenDate.isEmpty())
                tvInfo.setText(R.string.no_listen);
            else
                tvInfo.setText(String.format(getResString(R.string.album_listen_info), album.listenDate));
        } else
            tvInfo.setVisibility(GONE);
        
        edArtist.setText(album.artist);
        edTitle.setText(album.title);
        edYear.setText(album.year);
        rbRating.setRating(album.rating);
    }

    //----------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        setResult(RESULT_CANCELED);
        mode = getIntent().getIntExtra("mode", MODE_CREATE);
        initViews();
        initValues();
        setFocus(edArtist);
    }

    void setFocus(EditText edit) {
        edit.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    //----------------------------------------------------------------------------------------------
    void saveAlbum() {
        album.year = edYear.getText().toString();
        album.artist = edArtist.getText().toString();
        album.title = edTitle.getText().toString();
        album.rating = rbRating.getRating();

        switch (album.check()) {
            case 1:
                setFocus(edArtist);
                return;
            case 2:
                setFocus(edYear);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return;
            case 3:
                setFocus(edTitle);
                return;
        }

        Album dup =  MainActivity.albumAdapter.findDuplicate(album);
        if (dup != null) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.ask_duplicate)
                    .setMessage(dup.getText())
                    .setCancelable(false)
                    .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            album.save(Global.prefs);
                            if (mode == MODE_CREATE)
                                MainActivity.albumAdapter.addNew(album);
                            else
                                MainActivity.albumAdapter.update(album.id, album);

                            setResult(RESULT_OK);
                            if (!swMulti.isChecked()) {
                                finish();
                            }
                            album.clear();
                            initValues();
                            setFocus(edArtist);
                        }
                    })
                    .setNegativeButton(R.string.notSure, (dialog12, which) -> dialog12.dismiss())
                    .create();
            dialog.show();
            return;
        }

        album.save(Global.prefs);
        if (mode == MODE_CREATE)
            MainActivity.albumAdapter.addNew(album);
        else
            MainActivity.albumAdapter.update(album.id, album);

        setResult(RESULT_OK);
        if (!swMulti.isChecked())
            finish();

        album.clear();
        initValues();
        setFocus(edArtist);

    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnCancel) {
            this.finish();
        } else if (v.getId() == R.id.btnOk) {
            saveAlbum();
        }
    }
}