package com.notidekho.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotifAdapter adapter;
    private TextView tvStatus, tvCount, tvEmpty;
    private DatabaseReference dbRef;
    private List<NotifModel> notifList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        tvStatus     = findViewById(R.id.tv_status);
        tvCount      = findViewById(R.id.tv_count);
        tvEmpty      = findViewById(R.id.tv_empty);

        adapter = new NotifAdapter(notifList, dbRef);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        dbRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("notifications");

        // Adapter ko dbRef do
        adapter.setDbRef(dbRef);

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

    private void loadNotifications() {
        tvStatus.setText("Firebase se load ho raha hai...");

        dbRef.orderByChild("time").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                notifList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NotifModel model = new NotifModel();
                    model.key     = child.getKey();
                    model.app     = child.child("app").getValue(String.class);
                    model.title   = child.child("title").getValue(String.class);
                    model.msg     = child.child("msg").getValue(String.class);
                    model.type    = child.child("type").getValue(String.class);
                    model.read    = Boolean.TRUE.equals(child.child("read").getValue(Boolean.class));
                    Long t        = child.child("time").getValue(Long.class);
                    model.time    = t != null ? t : 0L;
                    notifList.add(model);
                }
                // Latest pehle dikhao
                Collections.reverse(notifList);

                adapter.notifyDataSetChanged();
                tvCount.setText(String.valueOf(notifList.size()));
                tvStatus.setText("Firebase connected — " + notifList.size() + " notifications");

                if (notifList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvStatus.setText("Error: " + error.getMessage());
            }
        });
    }
}
