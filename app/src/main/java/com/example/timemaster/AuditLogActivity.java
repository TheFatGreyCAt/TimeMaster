package com.example.timemaster; // Đổi package cho đúng dự án của bạn

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.timemaster.data.model.AuditLog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AuditLogActivity extends AppCompatActivity {

    private RecyclerView rcvLogs;
    private LogAdapter adapter;
    private List<AuditLog> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_log);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        rcvLogs = findViewById(R.id.rcv_logs);
        rcvLogs.setLayoutManager(new LinearLayoutManager(this));

        mList = new ArrayList<>();
        adapter = new LogAdapter(mList);
        rcvLogs.setAdapter(adapter);

        loadLogs();
    }

    private void loadLogs() {
        FirebaseFirestore.getInstance().collection("admin_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Mới nhất lên đầu
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        mList.clear();
                        mList.addAll(value.toObjects(AuditLog.class));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogVH> {
        List<AuditLog> list;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        LogAdapter(List<AuditLog> list) { this.list = list; }

        @NonNull @Override
        public LogVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audit_log, parent, false);
            return new LogVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LogVH holder, int position) {
            AuditLog log = list.get(position);
            holder.tvAction.setText(log.getAction());
            holder.tvDetails.setText(log.getDetails());
            holder.tvAdmin.setText("Bởi: " + log.getAdminEmail());
            if (log.getTimestamp() != null) {
                holder.tvTime.setText(sdf.format(log.getTimestamp().toDate()));
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class LogVH extends RecyclerView.ViewHolder {
            TextView tvAction, tvDetails, tvTime, tvAdmin;
            public LogVH(@NonNull View v) {
                super(v);
                tvAction = v.findViewById(R.id.tv_action);
                tvDetails = v.findViewById(R.id.tv_details);
                tvTime = v.findViewById(R.id.tv_time);
                tvAdmin = v.findViewById(R.id.tv_admin);
            }
        }
    }
}