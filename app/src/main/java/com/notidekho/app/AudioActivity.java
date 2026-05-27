package com.notidekho.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import io.agora.rtc2.*;

public class AudioActivity extends AppCompatActivity {

    private static final String AGORA_APP_ID = "f5a163996c7b4ff699c03a163c4c66a6";
    private static final String AGORA_TOKEN  = "007eJxTYEjcGPF2oqXX1aebL5w8bbc3K+7Np+3HrKWeRmb2um/V4lJQYEgzTTQ0M7a0NEs2TzJJSzOztEw2MAYJJZskm5klmjnZimU1BDIytC6ZwMrIAIEgPgdDXn5JZnFlXjIDAwBVwCH4";
    private static final String CHANNEL_NAME = "notisync";

    private RtcEngine agoraEngine;
    private DatabaseReference audioRef;
    private TextView tvStatus, tvListening;
    private Button btnListen, btnStop;
    private String userCode;
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        userCode   = getSharedPreferences("notidekho", MODE_PRIVATE)
                .getString("unique_code", null);

        tvStatus   = findViewById(R.id.tv_audio_status);
        tvListening = findViewById(R.id.tv_listening);
        btnListen  = findViewById(R.id.btn_listen);
        btnStop    = findViewById(R.id.btn_stop);

        audioRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(userCode).child("audio");

        initAgora();

        // Phone 1 ka status listen karo
        audioRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if ("streaming".equals(status)) {
                    tvStatus.setText("Phone 1 streaming kar raha hai 🎙️");
                    tvStatus.setTextColor(0xFF16A34A);
                } else {
                    tvStatus.setText("Phone 1 ready hai");
                    tvStatus.setTextColor(0xFF94A3B8);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });

        btnListen.setOnClickListener(v -> startListening());
        btnStop.setOnClickListener(v -> stopListening());
    }

    private void initAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext    = getApplicationContext();
            config.mAppId      = AGORA_APP_ID;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    runOnUiThread(() -> {
                        tvListening.setText("Sun raha hai... 👂");
                        tvListening.setVisibility(View.VISIBLE);
                        btnListen.setEnabled(false);
                        btnStop.setEnabled(true);
                    });
                }
                @Override
                public void onLeaveChannel(RtcStats stats) {
                    runOnUiThread(() -> {
                        tvListening.setVisibility(View.GONE);
                        btnListen.setEnabled(true);
                        btnStop.setEnabled(false);
                    });
                }
            };
            agoraEngine = RtcEngine.create(config);
            agoraEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            agoraEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            agoraEngine.enableAudio();
            agoraEngine.muteAllRemoteAudioStreams(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startListening() {
        // Firebase pe request bhejo
        audioRef.child("request").setValue("start");
        // Agora channel join karo
        agoraEngine.joinChannel(AGORA_TOKEN, CHANNEL_NAME, 0, null);
        isListening = true;
        btnListen.setText("Connecting...");
        btnListen.setEnabled(false);
    }

    private void stopListening() {
        audioRef.child("request").setValue("stop");
        agoraEngine.leaveChannel();
        isListening = false;
        tvListening.setVisibility(View.GONE);
        btnListen.setText("Sun na Shuru Karo 🎧");
        btnListen.setEnabled(true);
        btnStop.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isListening) stopListening();
        if (agoraEngine != null) {
            RtcEngine.destroy();
            agoraEngine = null;
        }
    }
}
