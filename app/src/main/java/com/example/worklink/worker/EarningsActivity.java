package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import com.example.worklink.FirestoreManager;
import com.example.worklink.R;
import com.example.worklink.models.Payment;
import com.example.worklink.models.Rating;
import com.example.worklink.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EarningsActivity extends AppCompatActivity {

    private static final String TAG = "EarningsActivity";
    TextView earningsText, ratingText, tvSelectedRange;
    Button btnDateRange, btnClear;
    ImageButton btnBack;
    ListView historyList;
    String workerId;
    private FirebaseFirestore db;

    SimpleDateFormat displaySdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    Date startDateFilter = null;
    Date endDateFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_earnings);

        // Date formatting consistent with DatePicker's UTC selection
        displaySdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getString("userId", "");

        if (TextUtils.isEmpty(workerId) && FirebaseAuth.getInstance().getCurrentUser() != null) {
            workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        btnBack = findViewById(R.id.btnBack);
        earningsText = findViewById(R.id.tvEarnings);
        ratingText = findViewById(R.id.tvOverallRating);
        tvSelectedRange = findViewById(R.id.tvSelectedRange);
        historyList = findViewById(R.id.lvReviews); 
        btnDateRange = findViewById(R.id.btnDateRange);
        btnClear = findViewById(R.id.btnClearFilter);
        
        btnBack.setOnClickListener(v -> finish());
        btnDateRange.setOnClickListener(v -> showDateRangePicker());
        
        btnClear.setOnClickListener(v -> {
            startDateFilter = null;
            endDateFilter = null;
            tvSelectedRange.setText("Showing All Time");
            btnClear.setVisibility(View.GONE);
            loadData();
        });

        if (!TextUtils.isEmpty(workerId)) {
            loadData();
        } else {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .build();

        dateRangePicker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                startDateFilter = new Date(selection.first);
                
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.setTimeInMillis(selection.second);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                endDateFilter = calendar.getTime();
                
                String displayRange = displaySdf.format(startDateFilter) + " - " + displaySdf.format(new Date(selection.second));
                tvSelectedRange.setText(displayRange);
                btnClear.setVisibility(View.VISIBLE);
                loadData();
            }
        });
    }

    private void loadData() {
        // 1. Load Overall Rating from User document
        FirestoreManager.getInstance().getUser(workerId).addOnSuccessListener(doc -> {
            if (isFinishing() || isDestroyed()) return;
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null && user.getWorkerRating() != null) {
                    ratingText.setText(String.format(Locale.getDefault(), "Rating: ⭐ %.1f", user.getWorkerRating()));
                }
            }
        });

        // 2. Load Job History
        Query paymentQuery = db.collection("payments")
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("paymentStatus", "PAID");

        if (startDateFilter != null && endDateFilter != null) {
            paymentQuery = paymentQuery.whereGreaterThanOrEqualTo("paymentDate", new Timestamp(startDateFilter))
                                     .whereLessThanOrEqualTo("paymentDate", new Timestamp(endDateFilter));
        }

        // Add ordering - THIS REQUIRES A COMPOSITE INDEX
        paymentQuery = paymentQuery.orderBy("paymentDate", Query.Direction.DESCENDING);

        paymentQuery.get().addOnSuccessListener(paymentDocs -> {
            if (isFinishing() || isDestroyed()) return;
            
            double totalEarnings = 0;
            if (paymentDocs.isEmpty()) {
                earningsText.setText("Total: ₹0.00");
                ArrayList<String> emptyList = new ArrayList<>();
                emptyList.add("No job history found.");
                historyList.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_white_text, emptyList));
                return;
            }

            List<Task<?>> detailTasks = new ArrayList<>();
            List<Payment> paymentsList = new ArrayList<>();

            for (QueryDocumentSnapshot doc : paymentDocs) {
                Payment p = doc.toObject(Payment.class);
                if (p == null || TextUtils.isEmpty(p.getBookingId())) continue;

                paymentsList.add(p);
                totalEarnings += (p.getAmount() != null ? p.getAmount() : 0.0);
                
                // Fetch Job Title via Booking
                detailTasks.add(db.collection("bookings").document(p.getBookingId()).get().continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String jId = task.getResult().getString("jobId");
                        if (!TextUtils.isEmpty(jId)) {
                            return db.collection("jobs").document(jId).get();
                        }
                    }
                    return Tasks.forResult(null);
                }));
                
                // Fetch Rating for this specific booking
                detailTasks.add(db.collection("ratings").whereEqualTo("bookingId", p.getBookingId()).limit(1).get());
            }

            final double finalTotal = totalEarnings;
            Tasks.whenAllComplete(detailTasks).addOnSuccessListener(results -> {
                if (isFinishing() || isDestroyed()) return;
                
                ArrayList<String> historyStrings = new ArrayList<>();
                earningsText.setText(String.format(Locale.getDefault(), "Total: ₹%.2f", finalTotal));

                for (int i = 0; i < paymentsList.size(); i++) {
                    Payment p = paymentsList.get(i);
                    Task<?> jobTask = detailTasks.get(i * 2);
                    Task<?> ratingTask = detailTasks.get(i * 2 + 1);

                    DocumentSnapshot jobDoc = null;
                    if (jobTask.isSuccessful() && jobTask.getResult() instanceof DocumentSnapshot) {
                        jobDoc = (DocumentSnapshot) jobTask.getResult();
                    }

                    com.google.firebase.firestore.QuerySnapshot ratingSnap = null;
                    if (ratingTask.isSuccessful() && ratingTask.getResult() instanceof com.google.firebase.firestore.QuerySnapshot) {
                        ratingSnap = (com.google.firebase.firestore.QuerySnapshot) ratingTask.getResult();
                    }

                    String title = (jobDoc != null && jobDoc.exists()) ? jobDoc.getString("title") : "Completed Job";
                    String ratingStr = "N/A";
                    String reviewStr = "No review";

                    if (ratingSnap != null && !ratingSnap.isEmpty()) {
                        Rating r = ratingSnap.getDocuments().get(0).toObject(Rating.class);
                        if (r != null) {
                            ratingStr = r.getRating() + "⭐";
                            reviewStr = TextUtils.isEmpty(r.getReview()) ? "No comment" : r.getReview();
                        }
                    }

                    String dateStr = (p.getPaymentDate() != null) ? displaySdf.format(p.getPaymentDate().toDate()) : "N/A";

                    String entry = "Job: " + title + " (" + dateStr + ")\n" +
                                   "Earned: ₹" + (p.getAmount() != null ? p.getAmount() : 0.0) + " | Rating: " + ratingStr + "\n" +
                                   "Review: \"" + reviewStr + "\"";
                    historyStrings.add(entry);
                }
                historyList.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_white_text, historyStrings));
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading history", e);
            if (!isFinishing() && !isDestroyed()) {
                if (e.getMessage() != null && e.getMessage().contains("requires an index")) {
                    Toast.makeText(this, "Index missing! Check Logcat for the link to fix.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
