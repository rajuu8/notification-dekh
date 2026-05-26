package com.notidekho.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotifAdapter adapter;
    private TextView tvStatus, tvCount, tvEmpty, tvCode, tvUnread;
    private ImageView ivQR, ivLogo;
    private LinearLayout layoutQR;
    private DatabaseReference dbRef;
    private List<NotifModel> notifList = new ArrayList<>();
    private String uniqueCode;
    private boolean qrVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        tvStatus     = findViewById(R.id.tv_status);
        tvCount      = findViewById(R.id.tv_count);
        tvEmpty      = findViewById(R.id.tv_empty);
        tvCode       = findViewById(R.id.tv_code);
        tvUnread     = findViewById(R.id.tv_unread);
        ivQR         = findViewById(R.id.iv_qr);
        layoutQR     = findViewById(R.id.layout_qr);

        uniqueCode = getUniqueCode();
        tvCode.setText(uniqueCode);
        generateQR(uniqueCode);

        // Firebase — sirf is user ka data
        dbRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(uniqueCode).child("notifications");

        // Code register karo
        FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("codes").child(uniqueCode).setValue(true);

        adapter = new NotifAdapter(notifList, dbRef);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // QR toggle
        findViewById(R.id.btn_show_qr).setOnClickListener(v -> {
            qrVisible = !qrVisible;
            layoutQR.setVisibility(qrVisible ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            dbRef.removeValue();
            notifList.clear();
            adapter.notifyDataSetChanged();
            tvCount.setText("0");
            tvUnread.setText("0 unread");
            tvEmpty.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Sab clear ho gaya!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_refresh).setOnClickListener(v -> loadNotifications());

        loadNotifications();
    }

    private String getUniqueCode() {
        android.content.SharedPreferences prefs = getSharedPreferences("notidekho", MODE_PRIVATE);
        String code = prefs.getString("unique_code", null);
        if (code == null) {
            code = String.format("%04d", new Random().nextInt(10000));
            prefs.edit().putString("unique_code", code).apply();
        }
        return code;
    }

    private void generateQR(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 500, 500);
            int w = matrix.getWidth(), h = matrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            for (int x = 0; x < w; x++)
                for (int y = 0; y < h; y++)
                    bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF1E3A5F : 0xFFFFFFFF);
            ivQR.setImageBitmap(bmp);
        } catch (WriterException e) { e.printStackTrace(); }
    }

    private void loadNotifications() {
        tvStatus.setText("Sync ho raha hai...");
        dbRef.orderByChild("time").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                notifList.clear();
                int unreadCount = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    NotifModel model = new NotifModel();
                    model.key   = child.getKey();
                    model.app   = child.child("app").getValue(String.class);
                    model.title = child.child("title").getValue(String.class);
                    model.msg   = child.child("msg").getValue(String.class);
                    model.type  = child.child("type").getValue(String.class);
                    model.read  = Boolean.TRUE.equals(child.child("read").getValue(Boolean.class));
                    Long t      = child.child("time").getValue(Long.class);
                    model.time  = t != null ? t : 0L;
                    notifList.add(model);
                    if (!model.read) unreadCount++;
                }
                Collections.reverse(notifList);
                adapter.notifyDataSetChanged();
                tvCount.setText(String.valueOf(notifList.size()));
                tvUnread.setText(unreadCount + " unread");
                tvStatus.setText("Live sync — " + notifList.size() + " notifications");
                tvEmpty.setVisibility(notifList.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                tvStatus.setText("Error: " + error.getMessage());
            }
        });
    }
}
