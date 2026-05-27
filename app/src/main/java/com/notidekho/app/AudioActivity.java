package com.notidekho.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.*;
import io.agora.rtc2.*;

public class AudioActivity extends AppCompatActivity {

    private static final String AGORA_APP_ID = "f5a163996c7b4ff699c03a163c4c66a6";
    private static final String AGORA_TOKEN  = "007eJxTYPDcrfss67vHsxKdtW5TBR1t6hQ4oqKPRgt1diz64KrxfZYCQ5ppoqGZsaWlWbJ5kklampmlZbKBMUgo2STZzCzRLOa8WFZDICPD7yOPGRkZIBDE52PIyy/JLK7MS45PLE3JzGdgAABXdyNX";
    private static final String CHANNEL      = "notisync_audio";
    private static final int MIC_PERM        = 300;

    private RtcEngine engine;
    private DatabaseReference audioRef;
    private TextView tvStatus, tvPhone1Status;
    private Button btnListen, btnStop;
    private String userCode;
    private boolean isListening = false;
    private boolean autoConnect = false;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        userCode    = getSharedPreferences("notidekho", MODE_PRIVATE)
                .getString("unique_code", null);
        autoConnect = getSharedPreferences("notidekho", MODE_PRIVATE)
                .getBoolean("auto_audio", false);

        tvStatus       = findViewById(R.id.tv_audio_status);
        tvPhone1Status = findViewById(R.id.tv_phone1_status);
        btnListen      = findViewById(R.id.btn_listen);
        btnStop        = findViewById(R.id.btn_stop);

        // Speaker force on
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
        audioManager.setStreamVolume(
            AudioManager.STREAM_VOICE_CALL,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
            0
        );

        audioRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(userCode).child("audio");

        audioRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                String s = snap.getValue(String.class);
                if ("streaming".equals(s)) {
                    tvPhone1Status.setText("Phone 1 streaming kar raha hai");
                    tvPhone1Status.setTextColor(0xFF16A34A);
                } else {
                    tvPhone1Status.setText("Phone 1 ready hai");
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
                        tvStatus.setText("Sun raha hai...");
                        tvStatus.setTextColor(0xFF16A34A);
                        btnListen.setEnabled(false);
                        btnStop.setEnabled(true);
                        audioManager.setSpeakerphoneOn(true);
                        engine.setEnableSpeakerphone(true);
                        engine.adjustPlaybackSignalVolume(400);
                        engine.muteAllRemoteAudioStreams(false);
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
                public void onRemoteAudioStateChanged(int uid, int state, int reason, int elapsed) {
                    runOnUiThread(() -> {
                        if (state == 2) {
                            tvStatus.setText("Audio aa raha hai!");
                            tvStatus.setTextColor(0xFF16A34A);
                        }
                    });
                }
                @Override
                public void onUserOffline(int uid, int reason) {
                    runOnUiThread(() -> tvStatus.setText("Phone 1 ne band kiya"));
                }
            };
            engine = RtcEngine.create(cfg);
            engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            engine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            engine.enableAudio();
            engine.setAudioProfile(
                Constants.AUDIO_PROFILE_DEFAULT,
                Constants.AUDIO_SCENARIO_CHATROOM
            );
            engine.setEnableSpeakerphone(true);
            engine.adjustPlaybackSignalVolume(400);
            engine.muteAllRemoteAudioStreams(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startListening() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = false;
        options.publishMicrophoneTrack = false;

        engine.joinChannel(AGORA_TOKEN, CHANNEL, 0, options);
        audioRef.child("request").setValue("start");
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
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        if (engine != null) {
            RtcEngine.destroy();
            engine = null;
        }
    }
}
