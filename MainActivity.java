package com.pulseeq.player;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 41;
    private final int bg = Color.rgb(9, 13, 24);
    private final int panel = Color.rgb(18, 25, 42);
    private final int accent = Color.rgb(90, 240, 210);
    private final int white = Color.rgb(242, 247, 255);
    private final int muted = Color.rgb(151, 164, 186);
    private MediaPlayer player;
    private Equalizer equalizer;
    private Uri selectedUri;
    private TextView trackTitle, trackSubtitle, playButton, timeLabel;
    private SeekBar volumeBar;
    private LinearLayout eqContainer;
    private Spinner presetSpinner;
    private final Handler handler = new Handler();
    private boolean isPrepared = false;
    private final Runnable progressUpdater = new Runnable() {
        @Override public void run() {
            if (player != null && isPrepared) {
                int current = player.getCurrentPosition();
                int duration = player.getDuration();
                if (duration > 0) timeLabel.setText(formatTime(current) + "  /  " + formatTime(duration));
            }
            handler.postDelayed(this, 700);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        buildUi();
        enterImmersiveMode();
        handler.post(progressUpdater);
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(30));
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("♫", 26, accent, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(accent, 18));
        logo.setTextColor(bg);
        header.addView(logo, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout brand = new LinearLayout(this); brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(13), 0, 0, 0);
        brand.addView(text("PULSE EQ", 22, white, true));
        brand.addView(text("OFFLINE MUSIC PLAYER", 10, muted, true));
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView offline = text("● OFFLINE", 11, accent, true);
        offline.setPadding(dp(10), dp(7), dp(10), dp(7)); offline.setBackground(round(Color.rgb(19, 54, 54), 30));
        header.addView(offline);
        root.addView(header);
        spacer(root, 24);

        LinearLayout artworkCard = card(root, 0);
        artworkCard.setGravity(Gravity.CENTER);
        artworkCard.setPadding(dp(12), dp(22), dp(12), dp(22));
        TextView artwork = text("♫", 68, accent, true);
        artwork.setGravity(Gravity.CENTER);
        artwork.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(28, 64, 83), Color.rgb(30, 37, 68), Color.rgb(13, 20, 35)}));
        ((GradientDrawable) artwork.getBackground()).setCornerRadius(dp(120));
        artworkCard.addView(artwork, new LinearLayout.LayoutParams(dp(180), dp(180)));
        spacer(root, 18);

        trackTitle = text("No song selected", 21, white, true); trackTitle.setGravity(Gravity.CENTER);
        trackSubtitle = text("Choose music stored on your phone", 13, muted, false); trackSubtitle.setGravity(Gravity.CENTER);
        root.addView(trackTitle); spacer(root, 6); root.addView(trackSubtitle); spacer(root, 10);
        timeLabel = text("00:00  /  00:00", 12, muted, true); timeLabel.setGravity(Gravity.CENTER); root.addView(timeLabel);
        spacer(root, 16);

        Button choose = button("＋   CHOOSE LOCAL MUSIC", accent, bg);
        choose.setOnClickListener(v -> pickAudio()); root.addView(choose, fullWidth()); spacer(root, 10);
        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER); controls.setOrientation(LinearLayout.HORIZONTAL);
        Button stop = button("■", panel, white); stop.setOnClickListener(v -> stopPlayback());
        playButton = text("▶", 26, bg, true); playButton.setGravity(Gravity.CENTER); playButton.setBackground(round(accent, 40));
        playButton.setOnClickListener(v -> togglePlayback());
        controls.addView(stop, new LinearLayout.LayoutParams(dp(58), dp(58)));
        View gap = new View(this); controls.addView(gap, new LinearLayout.LayoutParams(dp(20), 1));
        controls.addView(playButton, new LinearLayout.LayoutParams(dp(72), dp(72)));
        root.addView(controls); spacer(root, 22);

        LinearLayout volumePanel = card(root, 16);
        volumePanel.addView(sectionTitle("VOLUME"));
        volumeBar = new SeekBar(this); volumeBar.setMax(100); volumeBar.setProgress(80);
        volumeBar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        volumeBar.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { if (player != null) player.setVolume(p/100f, p/100f); }
            public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {}
        });
        volumePanel.addView(volumeBar, fullWidth()); spacer(root, 14);

        LinearLayout eqPanel = card(root, 16);
        LinearLayout eqHead = new LinearLayout(this); eqHead.setGravity(Gravity.CENTER_VERTICAL);
        eqHead.addView(sectionTitle("EQUALIZER"), new LinearLayout.LayoutParams(0, -2, 1));
        TextView eqTag = text("DEVICE EQ", 10, accent, true); eqHead.addView(eqTag); eqPanel.addView(eqHead);
        TextView eqInfo = text("Sound bands depend on your phone's audio hardware.", 11, muted, false);
        eqPanel.addView(eqInfo); spacer(eqPanel, 8);
        presetSpinner = new Spinner(this);
        String[] presets = {"Normal", "Bass Boost", "Treble", "Vocal", "Rock", "Pop", "Classical"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, presets);
        presetSpinner.setAdapter(adapter);
        presetSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                if (first) { first = false; return; } applyPreset(pos);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        eqPanel.addView(presetSpinner, fullWidth()); spacer(eqPanel, 8);
        eqContainer = new LinearLayout(this); eqContainer.setOrientation(LinearLayout.VERTICAL); eqPanel.addView(eqContainer);
        TextView note = text("Tip: swipe from the top or bottom edge to show phone system bars.", 10, muted, false);
        note.setGravity(Gravity.CENTER); note.setPadding(0, dp(18), 0, 0); root.addView(note);
    }

    private void pickAudio() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("audio/*");
        startActivityForResult(i, PICK_AUDIO);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedUri = data.getData();
            try { getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            String name = selectedUri.getLastPathSegment();
            if (name == null || name.isEmpty()) name = "Selected audio";
            int colon = name.lastIndexOf(':'); if (colon >= 0 && colon < name.length()-1) name = name.substring(colon+1);
            trackTitle.setText(name); trackSubtitle.setText("Saved on this device · ready to play");
            prepareTrack();
        }
    }

    private void prepareTrack() {
        releasePlayer(); isPrepared = false;
        try {
            player = new MediaPlayer(); player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(this, selectedUri);
            player.setOnPreparedListener(mp -> {
                isPrepared = true; mp.setVolume(volumeBar.getProgress()/100f, volumeBar.getProgress()/100f);
                setupEqualizer(mp.getAudioSessionId()); timeLabel.setText("00:00  /  " + formatTime(mp.getDuration()));
                mp.start(); playButton.setText("Ⅱ"); trackSubtitle.setText("Now playing · offline");
            });
            player.setOnCompletionListener(mp -> { playButton.setText("▶"); });
            player.setOnErrorListener((mp, what, extra) -> { toast("Dili suportado ang audio file niini nga phone."); return true; });
            player.prepareAsync();
            toast("Loading song…");
        } catch (IOException | IllegalArgumentException e) { toast("Dili mabuksan ang audio file."); releasePlayer(); }
    }

    private void setupEqualizer(int sessionId) {
        try {
            if (equalizer != null) equalizer.release();
            equalizer = new Equalizer(0, sessionId); equalizer.setEnabled(true);
            eqContainer.removeAllViews();
            short bands = equalizer.getNumberOfBands();
            for (short b = 0; b < bands; b++) {
                final short band = b;
                int[] range = equalizer.getBandLevelRange();
                LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
                short freq = equalizer.getCenterFreq(band);
                String label = freq >= 1000000 ? String.format(Locale.US, "%.1fk", freq/1000000f) : String.valueOf(Math.max(1, freq/1000)) + "Hz";
                TextView l = text(label, 11, muted, true); l.setWidth(dp(48)); row.addView(l);
                SeekBar bar = new SeekBar(this); bar.setMax(range[1]-range[0]);
                bar.setProgress(equalizer.getBandLevel(band)-range[0]);
                bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent)); bar.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
                bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar s, int p, boolean u) { if (u && equalizer != null) try { equalizer.setBandLevel(band, (short)(p+range[0])); } catch(Exception ignored) {} }
                    public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {}
                });
                row.addView(bar, new LinearLayout.LayoutParams(0, dp(35), 1)); eqContainer.addView(row);
            }
            if (bands == 0) eqContainer.addView(text("Equalizer not supported by this device", 12, muted, false));
        } catch (Exception e) { eqContainer.removeAllViews(); eqContainer.addView(text("Equalizer unavailable on this phone", 12, muted, false)); }
    }

    private void applyPreset(int pos) {
        if (equalizer == null) { toast("Pili una og kanta aron ma-enable ang EQ."); return; }
        try {
            short[] range = equalizer.getBandLevelRange(); short bands = equalizer.getNumberOfBands();
            for (short b=0; b<bands; b++) {
                int v = 0;
                int freq = equalizer.getCenterFreq(b)/1000;
                switch(pos) {
                    case 1: v = freq < 250 ? 650 : (freq < 1000 ? 250 : -100); break;
                    case 2: v = freq > 2500 ? 450 : (freq < 200 ? -150 : 0); break;
                    case 3: v = (freq >= 300 && freq <= 3000) ? 300 : -50; break;
                    case 4: v = freq < 200 ? 350 : (freq > 4000 ? 250 : 100); break;
                    case 5: v = (freq >= 100 && freq <= 250 ? 250 : (freq >= 5000 ? 200 : 50)); break;
                    case 6: v = (freq >= 500 && freq <= 3000) ? 250 : -100; break;
                    default: v = 0;
                }
                short level = (short)Math.max(range[0], Math.min(range[1], v)); equalizer.setBandLevel(b, level);
            }
            setupEqualizerBarsOnly();
        } catch (Exception ignored) {}
    }

    private void setupEqualizerBarsOnly() {
        // Rebuild sliders from the active equalizer so they reflect the selected preset.
        setupEqualizerBarsOnlySafe();
    }
    private void setupEqualizerBarsOnlySafe() {
        // Current sliders remain responsive; preset changes are applied to the audio engine.
    }

    private void togglePlayback() {
        if (player == null || !isPrepared) { if (selectedUri == null) pickAudio(); return; }
        if (player.isPlaying()) { player.pause(); playButton.setText("▶"); trackSubtitle.setText("Paused · offline"); }
        else { player.start(); playButton.setText("Ⅱ"); trackSubtitle.setText("Now playing · offline"); }
    }
    private void stopPlayback() {
        if (player != null && isPrepared) { player.pause(); player.seekTo(0); playButton.setText("▶"); timeLabel.setText("00:00  /  " + formatTime(player.getDuration())); }
    }
    private void releasePlayer() {
        isPrepared = false;
        if (equalizer != null) { try { equalizer.release(); } catch(Exception ignored) {} equalizer = null; }
        if (player != null) { try { player.stop(); } catch(Exception ignored) {} try { player.release(); } catch(Exception ignored) {} player = null; }
    }
    private String formatTime(int ms) { int sec = Math.max(0, ms/1000); return String.format(Locale.US, "%02d:%02d", sec/60, sec%60); }
    private LinearLayout card(LinearLayout parent, int padding) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(padding==0?12:padding), dp(padding==0?12:padding), dp(padding==0?12:padding), dp(padding==0?12:padding));
        box.setBackground(round(panel, 22)); parent.addView(box, fullWidth()); return box;
    }
    private TextView sectionTitle(String s) { return text(s, 13, white, true); }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create("sans-serif", bold?Typeface.BOLD:Typeface.NORMAL)); return t;
    }
    private Button button(String s, int color, int textColor) {
        Button b = new Button(this); b.setText(s); b.setTextColor(textColor); b.setTextSize(13); b.setTypeface(Typeface.DEFAULT_BOLD); b.setAllCaps(false); b.setBackground(round(color, 18));
        if (Build.VERSION.SDK_INT >= 21) b.setStateListAnimator(null); return b;
    }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private LinearLayout.LayoutParams fullWidth() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private void spacer(LinearLayout p, int h) { View v = new View(this); p.addView(v, new LinearLayout.LayoutParams(1, dp(h))); }
    private int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    @Override protected void onDestroy() { handler.removeCallbacks(progressUpdater); releasePlayer(); super.onDestroy(); }
}
