package com.notidekho.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.*;
import io.agora.rtc2.*;

public class AudioActivity extends AppCompatActivity {

    private static final String AGORA_APP_ID = "f5a163996c7b4ff699c03a163c4c66a6";
    private static final String AGORA_TOKEN  = "007eJxTYEjcGPF2oqXX1aebL5w8bbc3K+7Np+3HrKWeRmb2um/V4lJQYEgzTTQ0M7a0NEs2TzJJSzOztEw2MAYJJZskm5klmjnZimU1BDIytC6ZwMrIAIEgPgdDXn5JZnFlXjIDAwBVwCH4";
    private static final String CHANNEL      = "notisync_audio";
    private static final int MIC_PERM        = 300;

    private RtcEngine engine;
    private DatabaseReference audioRef;
    private TextView tvStatus, tvPhone1Status;
    private Button btnListen, btnStop;
    private String userCode;
    private boolean isListening = false;
    private boolean autoConnect  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        userCode      = getSharedPreferences("notidekho", MODE_PRIVATE)
                .getString("unique_code", null);
        autoConnect   = getSharedPreferences("notidekho", MODE_PRIVATE)
                .getBoolean("auto_audio", false);

        tvStatus      = findViewById(R.id.tv_audio_status);
        tvPhone1Status = findViewById(R.id.tv_phone1_status);
        btnListen     = findViewById(R.id.btn_listen);
        btnStop       = findViewById(R.id.btn_stop);

        audioRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(userCode).child("audio");

        // Phone 1 ka status dekho
        audioRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                String s = snap.getValue(String.class);
                if ("streaming".equals(s)) {
                    tvPhone1Status.setText("🟢 Phone 1 streaming kar raha hai");
                    tvPhone1Status.setTextColor(0xFF16A34A);
                } else if ("idle".equals(s) || s == null) {
                    tvPhone1Status.setText("⚪ Phone 1 ready hai");
                    tvPhone1Status.setTextColor(0xFF94A3B8);
                }
            }
            @Override public void onCancelled(DatabaseError e) {}
        });

        btnListen.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERM);
            } else {
                startListening();
                // Auto connect save karo
                getSharedPreferences("notidekho", MODE_PRIVATE)
                    .edit().putBoolean("auto_audio", true).apply();
            }
        });

        btnStop.setOnClickListener(v -> {
            stopListening();
            getSharedPreferences("notidekho", MODE_PRIVATE)
                .edit().putBoolean("auto_audio", false).apply();
        });

        initAgora();

        // Auto connect — agar pehle permission di thi
        if (autoConnect) {
            startListening();
        }
    }

    private void initAgora() {
        try {
            RtcEngineConfig cfg = new RtcEngineConfig();
            cfg.mContext     = getApplicationContext();
            cfg.mAppId       = AGORA_APP_ID;
            cfg.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String ch, int uid, int e) {
                    runOnUiThread(() -> {
                        isListening = true;
                        tvStatus.setText("🎧 Sun raha hai...");
                        tvStatus.setTextColor(0xFF16A34A);
                        btnListen.setEnabled(false);
                        btnStop.setEnabled(true);
                    });
                }
                @Override
                public void onLeaveChannel(RtcStats s) {
                    runOnUiThread(() -> {
                        isListening = false;
                        tvStatus.setText("Band ho gaya");
                        tvStatus.setTextColor(0xFF94A3B8);
                        btnListen.setEnabled(true);
                        btnStop.setEnabled(false);
                    });
                }
                @Override
                public void onUserOffline(int uid, int reason) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Phone 1 ne band kiya");
                        tvStatus.setTextColor(0xFFEF4444);
                    });
                }
            };
            engine = RtcEngine.create(cfg);
            // IMPORTANT: Audience mode - sirf suno
            engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            engine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            engine.enableAudio();
            engine.setAudioProfile(
                Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY,
                Constants.AUDIO_SCENARIO_CHATROOM
            );
            // Speaker on karo
            engine.setEnableSpeakerphone(true);
            engine.adjustPlaybackSignalVolume(400); // Volume boost
            engine.muteAllRemoteAudioStreams(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startListening() {
        audioRef.child("request").setValue("start");
        engine.joinChannel(AGORA_TOKEN, CHANNEL, 0, null);
        tvStatus.setText("Connect ho raha hai...");
        btnListen.setEnabled(false);
    }

    private void stopListening() {
        audioRef.child("request").setValue("stop");
        engine.leaveChannel();
        isListening = false;
        tvStatus.setText("Band ho gaya");
        btnListen.setEnabled(true);
        btnStop.setEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == MIC_PERM && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
            getSharedPreferences("notidekho", MODE_PRIVATE)
                .edit().putBoolean("auto_audio", true).apply();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine != null) {
            RtcEngine.destroy();
            engine = null;
        }
    }
}
