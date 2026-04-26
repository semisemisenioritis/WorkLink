package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.FirestoreManager;
import com.example.worklink.R;
import com.example.worklink.models.Application;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ApplicationsActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<ApplicationItem> applicationItems;
    ApplicationAdapter adapter;
    ImageButton btnBack;
    String workerId;
    private ListenerRegistration appListener;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_applications);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getString("userId", "");

        if (workerId.isEmpty() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        listView = findViewById(R.id.lvApplications);
        btnBack = findViewById(R.id.btnBack);
        applicationItems = new ArrayList<>();
        
        adapter = new ApplicationAdapter(this, applicationItems);
        listView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        startRealtimeApplicationsListener();
    }

    private void startRealtimeApplicationsListener() {
        appListener = FirestoreManager.getInstance().getApplicationsForWorker(workerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        applicationItems.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Application app = doc.toObject(Application.class);
                            fetchJobDetailsAndAdd(app);
                        }
                    }
                });
    }

    private void fetchJobDetailsAndAdd(Application app) {
        // Fetch the Job title because it's not stored in the application document
        db.collection("jobs").document(app.getJobId()).get().addOnSuccessListener(doc -> {
            String title = doc.exists() ? doc.getString("title") : "Unknown Job";
            String date = app.getAppliedAt() != null ? app.getAppliedAt().toDate().toString() : "";
            
            applicationItems.add(new ApplicationItem(
                    app.getApplicationId(),
                    title,
                    app.getStatus(),
                    date
            ));
            adapter.notifyDataSetChanged();
        });
    }

    private void withdrawApplication(String appId) {
        db.collection("applications").document(appId).update("status", "withdrawn")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Application Withdrawn", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appListener != null) appListener.remove();
    }

    static class ApplicationItem {
        String id;
        String title, status, date;

        ApplicationItem(String id, String title, String status, String date) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.date = date;
        }
    }

    private class ApplicationAdapter extends ArrayAdapter<ApplicationItem> {
        ApplicationAdapter(Context context, ArrayList<ApplicationItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_application, parent, false);
            }

            ApplicationItem item = getItem(position);
            TextView info = convertView.findViewById(R.id.tvAppInfo);
            Button btnWithdraw = convertView.findViewById(R.id.btnWithdraw);

            String displayStatus = item.status.substring(0, 1).toUpperCase() + item.status.substring(1);
            info.setText("Job: " + item.title + "\nStatus: " + displayStatus + "\nDate: " + item.date);

            if ("pending".equals(item.status)) {
                btnWithdraw.setVisibility(View.VISIBLE);
                btnWithdraw.setOnClickListener(v -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Withdraw")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Yes", (dialog, which) -> withdrawApplication(item.id))
                            .setNegativeButton("No", null).show();
                });
            } else {
                btnWithdraw.setVisibility(View.GONE);
            }
            return convertView;
        }
    }
}
