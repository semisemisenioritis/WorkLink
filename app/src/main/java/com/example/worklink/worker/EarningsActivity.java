package com.example.worklink.worker;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.R;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.Rating;
import com.example.worklink.api.models.WorkerStatsResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EarningsActivity extends AppCompatActivity {

    TextView earningsText, ratingText, tvSelectedRange;
    Button btnDateRange, btnClear;
    ListView reviewsList;
    SessionManager sessionManager;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker_activity_earnings);

        sessionManager = new SessionManager(this);
        workerId = sessionManager.getUserId();

        earningsText = findViewById(R.id.tvEarnings);
        ratingText = findViewById(R.id.tvOverallRating);
        tvSelectedRange = findViewById(R.id.tvSelectedRange);
        reviewsList = findViewById(R.id.lvReviews);
        btnDateRange = findViewById(R.id.btnDateRange);
        btnClear = findViewById(R.id.btnClearFilter);
        
        // Hide date range filtering for now as API doesn't support it yet
        btnDateRange.setVisibility(View.GONE);
        btnClear.setVisibility(View.GONE);
        tvSelectedRange.setText("Showing All Time (API Placeholder)");

        loadData();
    }

    private void loadData() {
        ApiClient.getService(this).getWorkerStats(workerId).enqueue(new Callback<WorkerStatsResponse>() {
            @Override
            public void onResponse(Call<WorkerStatsResponse> call, Response<WorkerStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WorkerStatsResponse stats = response.body();
                    
                    earningsText.setText(String.format(Locale.getDefault(), "Total: ₹%.2f", 
                            stats.getStats().getTotalEarningsCalculated()));
                    
                    ratingText.setText(String.format(Locale.getDefault(), "Rating: ⭐ %.1f", 
                            stats.getStats().getAverageRating()));

                    List<String> reviews = new ArrayList<>();
                    if (stats.getRecentRatings() != null) {
                        for (Rating r : stats.getRecentRatings()) {
                            reviews.add("Rating: " + r.getRating() + "⭐\n\"" + 
                                    (r.getReview() == null || r.getReview().isEmpty() ? "No comment" : r.getReview()) + "\"");
                        }
                    }

                    if (reviews.isEmpty()) {
                        reviews.add("No reviews found.");
                    }

                    reviewsList.setAdapter(new ArrayAdapter<>(EarningsActivity.this,
                            android.R.layout.simple_list_item_1, reviews));
                } else {
                    Toast.makeText(EarningsActivity.this, "Failed to load stats", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WorkerStatsResponse> call, Throwable t) {
                Toast.makeText(EarningsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
