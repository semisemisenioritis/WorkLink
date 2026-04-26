package com.example.worklink.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.example.worklink.FirestoreManager;
import com.example.worklink.R;
import com.example.worklink.models.Payment;
import com.example.worklink.models.Rating;
import com.example.worklink.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class EarningsActivity extends AppCompatActivity {

    TextView earningsText, ratingText, tvSelectedRange;
    Button btnDateRange, btnClear;
    ImageButton btnBack;
    ListView reviewsList;
    String workerId;
    private FirebaseFirestore db;

    SimpleDateFormat displaySdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    Date startDateFilter = null;
    Date endDateFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_earnings);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        workerId = sharedPreferences.getString("userId", "");

        btnBack = findViewById(R.id.btnBack);
        earningsText = findViewById(R.id.tvEarnings);
        ratingText = findViewById(R.id.tvOverallRating);
        tvSelectedRange = findViewById(R.id.tvSelectedRange);
        reviewsList = findViewById(R.id.lvReviews);
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

        loadData();
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
                endDateFilter = new Date(selection.second);
                
                String displayRange = displaySdf.format(startDateFilter) + " - " + displaySdf.format(endDateFilter);
                tvSelectedRange.setText(displayRange);
                btnClear.setVisibility(View.VISIBLE);
                loadData();
            }
        });
    }

    private void loadData() {
        // 1. Load Overall Rating from User profile
        FirestoreManager.getInstance().getUser(workerId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null && user.getWorkerRating() != null) {
                    ratingText.setText(String.format(Locale.getDefault(), "Rating: ⭐ %.1f", user.getWorkerRating()));
                }
            }
        });

        // 2. Load Payments and calculate total
        Query paymentQuery = db.collection("payments")
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("paymentStatus", "PAID");

        if (startDateFilter != null && endDateFilter != null) {
            paymentQuery = paymentQuery.whereGreaterThanOrEqualTo("paymentDate", new Timestamp(startDateFilter))
                                     .whereLessThanOrEqualTo("paymentDate", new Timestamp(endDateFilter));
        }

        paymentQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            double total = 0;
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Payment payment = doc.toObject(Payment.class);
                if (payment.getAmount() != null) {
                    total += payment.getAmount();
                }
            }
            earningsText.setText(String.format(Locale.getDefault(), "Total: ₹%.2f", total));
        });

        // 3. Load Reviews (Ratings)
        Query ratingQuery = db.collection("ratings")
                .whereEqualTo("givenTo", workerId)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        ratingQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            ArrayList<String> reviews = new ArrayList<>();
            if (queryDocumentSnapshots.isEmpty()) {
                reviews.add("No reviews found.");
            } else {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Rating r = doc.toObject(Rating.class);
                    String display = "Rating: " + r.getRating() + "⭐\n" +
                                     "\"" + (TextUtils.isEmpty(r.getReview()) ? "No comment" : r.getReview()) + "\"";
                    reviews.add(display);
                }
            }
            reviewsList.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_white_text, reviews));
        });
    }
}
