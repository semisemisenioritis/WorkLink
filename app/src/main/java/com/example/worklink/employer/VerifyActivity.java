package com.example.worklink.employer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worklink.R;
import com.example.worklink.models.Booking;
import com.example.worklink.models.Job;
import com.example.worklink.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VerifyActivity extends AppCompatActivity {

    private static final String TAG = "VerifyActivity";
    ListView listView;
    TextView tvTitle;
    ArrayList<VerifyItem> verifyItems;
    VerifyAdapter adapter;
    ImageButton btnBack;
    String employerId;
    private FirebaseFirestore db;
    private List<ListenerRegistration> bookingListeners = new ArrayList<>();
    private Set<String> pendingBookingIds = new HashSet<>();

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

        loadBookingsRealtime();
    }

    private void loadBookingsRealtime() {
        if (employerId.isEmpty()) return;
        
        db.collection("jobs")
                .whereEqualTo("employerId", employerId)
                .get()
                .addOnSuccessListener(jobSnapshots -> {
                    for (ListenerRegistration lr : bookingListeners) lr.remove();
                    bookingListeners.clear();
                    
                    for (QueryDocumentSnapshot jobDoc : jobSnapshots) {
                        Job job = jobDoc.toObject(Job.class);
                        String jId = job.getJobId() != null ? job.getJobId() : jobDoc.getId();
                        job.setJobId(jId);
                        attachBookingListener(job);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading jobs", e));
    }

    private void attachBookingListener(Job job) {
        ListenerRegistration lr = db.collection("bookings")
                .whereEqualTo("jobId", job.getJobId())
                .whereEqualTo("status", "ACCEPTED")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Booking listener error", error);
                        return;
                    }
                    if (value != null) {
                        runOnUiThread(() -> updateItemsFromBookings(job, value.getDocuments()));
                    }
                });
        bookingListeners.add(lr);
    }

    private void updateItemsFromBookings(Job job, List<DocumentSnapshot> bookingDocs) {
        List<String> currentBookingIds = new ArrayList<>();
        for (DocumentSnapshot doc : bookingDocs) {
            currentBookingIds.add(doc.getId());
        }
        
        // 1. Remove items that are no longer in "ACCEPTED" status
        verifyItems.removeIf(item -> item.jobId.equals(job.getJobId()) && !currentBookingIds.contains(item.bookingId));
        
        // 2. Add new items
        for (DocumentSnapshot doc : bookingDocs) {
            String bId = doc.getId();
            // Check if already in list or fetch is pending
            boolean alreadyListed = false;
            for (VerifyItem item : verifyItems) {
                if (item.bookingId.equals(bId)) {
                    alreadyListed = true;
                    break;
                }
            }
            
            if (!alreadyListed && !pendingBookingIds.contains(bId)) {
                Booking booking = doc.toObject(Booking.class);
                if (booking != null) {
                    booking.setBookingId(bId);
                    pendingBookingIds.add(bId);
                    fetchWorkerDetailsAndAdd(job, booking);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchWorkerDetailsAndAdd(Job job, Booking booking) {
        db.collection("users").document(booking.getWorkerId()).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                User user = userDoc.toObject(User.class);
                if (user != null) {
                    VerifyItem newItem = new VerifyItem(
                        booking.getBookingId(),
                        job.getJobId(),
                        user.getName(),
                        job.getTitle(),
                        job.getWage() != null ? job.getWage() : 0.0,
                        user.getId(),
                        job.getDurationDays() != null ? job.getDurationDays() : 1
                    );
                    
                    runOnUiThread(() -> {
                        pendingBookingIds.remove(booking.getBookingId());
                        // Double check if it's still relevant (hasn't been removed by a new snapshot)
                        // This would happen if attachBookingListener fired again while we were fetching.
                        // But since we are on UI thread now, and updateItemsFromBookings also runs on UI thread,
                        // we can check if the booking is still "accepted".
                        // For simplicity, we just add it and rely on the next snapshot to clean up if needed.
                        
                        // Prevent duplicates just in case
                        boolean exists = false;
                        for (VerifyItem item : verifyItems) {
                            if (item.bookingId.equals(newItem.bookingId)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            verifyItems.add(newItem);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        }).addOnFailureListener(e -> {
            runOnUiThread(() -> pendingBookingIds.remove(booking.getBookingId()));
        });
    }

    static class VerifyItem {
        String bookingId, jobId, workerId;
        int duration;
        String workerName, jobTitle;
        double dailyWage, total;

        VerifyItem(String bId, String jId, String wName, String jTitle, double wage, String wId, int dur) {
            this.bookingId = bId;
            this.jobId = jId;
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
            if (item == null) return convertView;

            TextView info = convertView.findViewById(R.id.tvVerifyInfo);
            Button btnComplete = convertView.findViewById(R.id.btnMarkComplete);
            Button btnTerminate = convertView.findViewById(R.id.btnTerminate);

            info.setText("Worker: " + item.workerName + "\nJob: " + item.jobTitle + 
                         "\nTotal: ₹" + item.total + " (" + item.duration + " days)");

            btnComplete.setOnClickListener(v -> {
                Intent intent = new Intent(VerifyActivity.this, PaymentRatingActivity.class);
                intent.putExtra("bookingId", item.bookingId);
                intent.putExtra("workerId", item.workerId);
                intent.putExtra("amount", item.total);
                intent.putExtra("newStatus", "COMPLETED");
                intent.putExtra("actualDays", item.duration);
                startActivity(intent);
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
        builder.setView(input);

        builder.setPositiveButton("Proceed to Payment", (dialog, which) -> {
            String daysStr = input.getText().toString();
            if (daysStr.isEmpty()) return;
            try {
                int daysWorked = Integer.parseInt(daysStr);
                if (daysWorked > item.duration) {
                    Toast.makeText(this, "Cannot exceed " + item.duration + " days", Toast.LENGTH_SHORT).show();
                    return;
                }
                double newTotal = daysWorked * item.dailyWage;
                Intent intent = new Intent(VerifyActivity.this, PaymentRatingActivity.class);
                intent.putExtra("bookingId", item.bookingId);
                intent.putExtra("workerId", item.workerId);
                intent.putExtra("amount", newTotal);
                intent.putExtra("newStatus", "TERMINATED");
                intent.putExtra("actualDays", daysWorked);
                startActivity(intent);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ListenerRegistration lr : bookingListeners) lr.remove();
    }
}
