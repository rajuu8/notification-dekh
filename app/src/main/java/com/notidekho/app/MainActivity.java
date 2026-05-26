package com.notidekho.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
    private TextView tvStatus, tvCount, tvEmpty, tvCode;
    private ImageView ivQR;
    private DatabaseReference dbRef;
    private List<NotifModel> notifList = new ArrayList<>();
    private String uniqueCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        tvStatus     = findViewById(R.id.tv_status);
        tvCount      = findViewById(R.id.tv_count);
        tvEmpty      = findViewById(R.id.tv_empty);
        tvCode       = findViewById(R.id.tv_code);
        ivQR         = findViewById(R.id.iv_qr);

        // Unique code generate karo ya load karo
        uniqueCode = getUniqueCode();
        tvCode.setText(uniqueCode);

        // QR generate karo
        generateQR(uniqueCode);

        // Firebase — sirf is user ka data
        dbRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(uniqueCode).child("notifications");

        adapter = new NotifAdapter(notifList, dbRef);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Code Firebase mein register karo
        FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("codes").child(uniqueCode).setValue(true);

        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            dbRef.removeValue();
            notifList.clear();
            adapter.notifyDataSetChanged();
            tvCount.setText("0");
            tvEmpty.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Sab clear ho gaya!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_refresh).setOnClickListener(v -> {
            tvStatus.setText("Refresh ho raha hai...");
            loadNotifications();
        });

        loadNotifications();
    }

    private String getUniqueCode() {
        // SharedPreferences mein save karo taki app restart pe same code rahe
        android.content.SharedPreferences prefs = getSharedPreferences("notidekho", MODE_PRIVATE);
        String code = prefs.getString("unique_code", null);
        if (code == null) {
            // 4 digit random code banao
            code = String.format("%04d", new Random().nextInt(10000));
            prefs.edit().putString("unique_code", code).apply();
        }
        return code;
    }

    private void generateQR(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 400, 400);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ivQR.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void loadNotifications() {
        tvStatus.setText("Firebase se load ho raha hai...");
        dbRef.orderByChild("time").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                notifList.clear();
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
                }
                Collections.reverse(notifList);
                adapter.notifyDataSetChanged();
                tvCount.setText(String.valueOf(notifList.size()));
                tvStatus.setText("Code: " + uniqueCode + " — " + notifList.size() + " notifications");
                tvEmpty.setVisibility(notifList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvStatus.setText("Error: " + error.getMessage());
            }
        });
    }
}
