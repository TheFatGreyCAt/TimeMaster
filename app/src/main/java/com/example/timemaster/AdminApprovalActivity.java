package com.example.timemaster; // <--- DÒNG NÀY QUAN TRỌNG NHẤT

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminApprovalActivity extends AppCompatActivity {

    private RecyclerView rcvApprovals;
    private ApprovalAdapter adapter;
    private List<DocumentSnapshot> mListRequests;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approval);

        db = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        rcvApprovals = findViewById(R.id.rcv_approvals);
        rcvApprovals.setLayoutManager(new LinearLayoutManager(this));

        mListRequests = new ArrayList<>();
        adapter = new ApprovalAdapter(mListRequests);
        rcvApprovals.setAdapter(adapter);

        loadPendingRequests();
    }

    private void loadPendingRequests() {
        db.collection("update_requests")
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        mListRequests.clear();
                        mListRequests.addAll(value.getDocuments());
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    class ApprovalAdapter extends RecyclerView.Adapter<ApprovalAdapter.ApprovalViewHolder> {
        List<DocumentSnapshot> list;
        public ApprovalAdapter(List<DocumentSnapshot> list) { this.list = list; }

        @NonNull @Override
        public ApprovalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_approval_request, parent, false);
            return new ApprovalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ApprovalViewHolder holder, int position) {
            DocumentSnapshot doc = list.get(position);
            String oldName = doc.getString("oldName");
            String newName = doc.getString("newName");
            String userId = doc.getString("userId");
            Map<String, Object> newData = (Map<String, Object>) doc.get("newData");

            holder.tvInfo.setText("Cũ: " + oldName + "\nMới: " + newName);

            holder.btnApprove.setOnClickListener(v -> {
                if (userId != null && newData != null) {
                    db.collection("users").document(userId).update(newData).addOnSuccessListener(a -> {
                        db.collection("update_requests").document(doc.getId()).delete();
                        Toast.makeText(AdminApprovalActivity.this, "Đã duyệt", Toast.LENGTH_SHORT).show();
                    });
                }
            });

            holder.btnReject.setOnClickListener(v -> {
                db.collection("update_requests").document(doc.getId()).delete();
                Toast.makeText(AdminApprovalActivity.this, "Đã từ chối", Toast.LENGTH_SHORT).show();
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ApprovalViewHolder extends RecyclerView.ViewHolder {
            TextView tvInfo;
            Button btnApprove, btnReject;
            public ApprovalViewHolder(@NonNull View itemView) {
                super(itemView);
                tvInfo = itemView.findViewById(R.id.tv_request_info);
                btnApprove = itemView.findViewById(R.id.btn_approve);
                btnReject = itemView.findViewById(R.id.btn_reject);
            }
        }
    }
}