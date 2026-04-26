package com.example.worklink.employer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.FirestoreManager;
import com.example.worklink.R;
import com.example.worklink.models.Booking;
import com.example.worklink.models.Job;
import com.example.worklink.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class VerifyActivity extends AppCompatActivity {

    ListView listView;
    TextView tvTitle;
    ArrayList<VerifyItem> verifyItems;
    VerifyAdapter adapter;
    ImageButton btnBack;
    String employerId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_job_feed);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getString("userId", "");

        if (employerId.isEmpty() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            employerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        listView = findViewById(R.id.jobListView);
        verifyItems = new ArrayList<>();

        tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText("Verify Work");
        }

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        adapter = new VerifyAdapter(this, verifyItems);
        listView.setAdapter(adapter);

        loadBookings();
    }

    private void loadBookings() {
        // Fetch jobs posted by this employer
        db.collection("jobs")
                .whereEqualTo("employerId", employerId)
                .get()
                .addOnSuccessListener(jobSnapshots -> {
                    verifyItems.clear();
                    for (QueryDocumentSnapshot jobDoc : jobSnapshots) {
                        Job job = jobDoc.toObject(Job.class);
                        fetchAcceptedBookingsForJob(job);
                    }
                });
    }

    private void fetchAcceptedBookingsForJob(Job job) {
        db.collection("bookings")
                .whereEqualTo("jobId", job.getJobId())
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .addOnSuccessListener(bookingSnapshots -> {
                    for (QueryDocumentSnapshot bookingDoc : bookingSnapshots) {
                        Booking booking = bookingDoc.toObject(Booking.class);
                        fetchWorkerDetailsAndAdd(job, booking);
                    }
                });
    }

    private void fetchWorkerDetailsAndAdd(Job job, Booking booking) {
        db.collection("users").document(booking.getWorkerId()).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                User user = userDoc.toObject(User.class);
                if (user != null) {
                    verifyItems.add(new VerifyItem(
                        booking.getBookingId(),
                        user.getName(),
                        job.getTitle(),
                        job.getWage() != null ? job.getWage() : 0.0,
                        user.getId(),
                        job.getDurationDays() != null ? job.getDurationDays() : 1
                    ));
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    static class VerifyItem {
        String bookingId, workerId;
        int duration;
        String workerName, jobTitle;
        double dailyWage, total;

        VerifyItem(String bId, String wName, String jTitle, double wage, String wId, int dur) {
            this.bookingId = bId;
            this.workerName = wName;
            this.jobTitle = jTitle;
            this.dailyWage = wage;
            this.workerId = wId;
            this.duration = dur;
            this.total = wage * dur;
        }
    }

    private class VerifyAdapter extends ArrayAdapter<VerifyItem> {
        VerifyAdapter(Context context, ArrayList<VerifyItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_verify, parent, false);
            }

            VerifyItem item = getItem(position);
            TextView info = convertView.findViewById(R.id.tvVerifyInfo);
            Button btnComplete = convertView.findViewById(R.id.btnMarkComplete);
            Button btnTerminate = convertView.findViewById(R.id.btnTerminate);

            info.setText("Worker: " + item.workerName + "\nJob: " + item.jobTitle + 
                         "\nTotal: ₹" + item.total + " (" + item.duration + " days)");

            btnComplete.setOnClickListener(v -> {
                db.collection("bookings").document(item.bookingId)
                        .update("status", "COMPLETED", "actualDays", item.duration)
                        .addOnSuccessListener(aVoid -> {
                            Intent intent = new Intent(VerifyActivity.this, PaymentRatingActivity.class);
                            intent.putExtra("bookingId", item.bookingId);
                            intent.putExtra("workerId", item.workerId);
                            intent.putExtra("amount", item.total);
                            startActivity(intent);
                            finish();
                        });
            });

            btnTerminate.setOnClickListener(v -> showTerminateDialog(item));

            return convertView;
        }
    }

    private void showTerminateDialog(VerifyItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Terminate Employment");
        builder.setMessage("Enter the actual number of days worked by " + item.workerName + ":");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Max days: " + item.duration);
        builder.setView(input);

        builder.setPositiveButton("Proceed to Payment", (dialog, which) -> {
            String daysStr = input.getText().toString();
            if (daysStr.isEmpty()) return;

            int daysWorked = Integer.parseInt(daysStr);
            if (daysWorked > item.duration) {
                Toast.makeText(this, "Days worked cannot exceed original duration", Toast.LENGTH_SHORT).show();
                return;
            }

            double newTotal = daysWorked * item.dailyWage;

            db.collection("bookings").document(item.bookingId)
                    .update("status", "TERMINATED", "actualDays", daysWorked)
                    .addOnSuccessListener(aVoid -> {
                        Intent intent = new Intent(VerifyActivity.this, PaymentRatingActivity.class);
                        intent.putExtra("bookingId", item.bookingId);
                        intent.putExtra("workerId", item.workerId);
                        intent.putExtra("amount", newTotal);
                        startActivity(intent);
                        finish();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
