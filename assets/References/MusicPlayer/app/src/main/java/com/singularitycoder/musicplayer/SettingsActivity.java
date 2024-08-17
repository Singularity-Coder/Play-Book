package com.singularitycoder.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private CheckBox cbShufflePlay;
    private CheckBox cbAutoPlayNext;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initViews();
        preferences = getSharedPreferences(Constants.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        boolean shufflePlay = preferences.getBoolean(Constants.SHUFFLE_PLAY_KEY, false);
        boolean autoPlayNext = preferences.getBoolean(Constants.AUTO_PLAY_NEXT_KEY, true);
        cbShufflePlay.setChecked(shufflePlay);
        cbAutoPlayNext.setChecked(autoPlayNext);
    }

    private void initViews() {
        cbShufflePlay = findViewById(R.id.cb_shuffle_play);
        cbAutoPlayNext = findViewById(R.id.cb_auto_play_next);
        cbShufflePlay.setOnCheckedChangeListener(this);
        cbAutoPlayNext.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.cb_shuffle_play) {
            shufflePlayToggle(isChecked);
        } else if (id == R.id.cb_auto_play_next) {
            autoPlayNextToggle(isChecked);
        }
    }

    private void shufflePlayToggle(boolean isChecked) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.SHUFFLE_PLAY_KEY, isChecked);
        editor.commit();
    }

    private void autoPlayNextToggle(boolean isChecked) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.AUTO_PLAY_NEXT_KEY, isChecked);
        editor.commit();
    }
}
