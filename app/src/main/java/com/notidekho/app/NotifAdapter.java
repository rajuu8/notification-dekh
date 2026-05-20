package com.notidekho.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DatabaseReference;
import java.util.List;

public class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.ViewHolder> {

    private List<NotifModel> list;
    private DatabaseReference dbRef;

    public NotifAdapter(List<NotifModel> list, DatabaseReference dbRef) {
        this.list  = list;
        this.dbRef = dbRef;
    }

    public void setDbRef(DatabaseReference dbRef) {
        this.dbRef = dbRef;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        NotifModel n = list.get(pos);

        h.tvApp.setText(n.app != null ? n.app : "Unknown");
        h.tvTitle.setText(n.title != null ? n.title : "");
        h.tvMsg.setText(n.msg != null ? n.msg : "");
        h.tvType.setText(n.type != null ? n.type.toUpperCase() : "");
        h.tvTime.setText(timeAgo(n.time));
        h.tvIcon.setText(getIcon(n.type));
        h.unreadDot.setVisibility(n.read ? View.INVISIBLE : View.VISIBLE);

        // Click se read mark karo
        h.itemView.setOnClickListener(v -> {
            if (!n.read && dbRef != null) {
                dbRef.child(n.key).child("read").setValue(true);
                n.read = true;
                h.unreadDot.setVisibility(View.INVISIBLE);
            }
        });
    }

    private String getIcon(String type) {
        if (type == null) return "🔔";
        switch (type) {
            case "message":  return "💬";
            case "alert":    return "⚠️";
            case "reminder": return "⏰";
            case "update":   return "🔄";
            case "promo":    return "🏷️";
            default:         return "🔔";
        }
    }

    private String timeAgo(long ts) {
        long diff = (System.currentTimeMillis() - ts) / 1000;
        if (diff < 60)    return "Abhi";
        if (diff < 3600)  return (diff / 60) + " min pehle";
        if (diff < 86400) return (diff / 3600) + " ghante pehle";
        return (diff / 86400) + " din pehle";
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvApp, tvTitle, tvMsg, tvType, tvTime, tvIcon;
        View unreadDot;

        ViewHolder(View v) {
            super(v);
            tvApp     = v.findViewById(R.id.tv_app);
            tvTitle   = v.findViewById(R.id.tv_title);
            tvMsg     = v.findViewById(R.id.tv_msg);
            tvType    = v.findViewById(R.id.tv_type);
            tvTime    = v.findViewById(R.id.tv_time);
            tvIcon    = v.findViewById(R.id.tv_icon);
            unreadDot = v.findViewById(R.id.unread_dot);
        }
    }
}
