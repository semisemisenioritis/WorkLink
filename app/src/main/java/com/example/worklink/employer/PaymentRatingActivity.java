package com.example.worklink.employer;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.R;
import com.example.worklink.api.ApiClient;
import com.example.worklink.api.SessionManager;
import com.example.worklink.api.models.Payment;
import com.example.worklink.api.models.Rating;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentRatingActivity extends AppCompatActivity {

    EditText amount, review;
    RatingBar ratingBar;
    Button submit;
    SessionManager sessionManager;
    int bookingId;
    int employerId;
    int workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_payment_rating);

        sessionManager = new SessionManager(this);
        employerId = sessionManager.getUserId();

        bookingId = getIntent().getIntExtra("bookingId", -1);
        workerId = getIntent().getIntExtra("workerId", -1);
        double suggestedAmount = getIntent().getDoubleExtra("amount", 0.0);

        amount = findViewById(R.id.etAmount);
        review = findViewById(R.id.etReview);
        ratingBar = findViewById(R.id.ratingBar);
        submit = findViewById(R.id.btnSubmit);
        
        amount.setText(String.valueOf(suggestedAmount));

        submit.setOnClickListener(v -> {
            String amountStr = amount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double paymentAmount = Double.parseDouble(amountStr);
            
            // 1. Record Payment
            Payment payment = new Payment();
            payment.setBookingId(bookingId);
            payment.setAmount(paymentAmount);
            payment.setPaymentStatus("COMPLETED");
            payment.setPaymentMethod("UPI");
            payment.setPaymentDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));

            ApiClient.getService(this).createPayment(payment).enqueue(new Callback<Payment>() {
                @Override
                public void onResponse(Call<Payment> call, Response<Payment> response) {
                    if (response.isSuccessful()) {
                        submitRating();
                    } else {
                        Toast.makeText(PaymentRatingActivity.this, "Payment failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Payment> call, Throwable t) {
                    Toast.makeText(PaymentRatingActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void submitRating() {
        Rating rating = new Rating();
        rating.setBookingId(bookingId);
        rating.setGivenBy(employerId);
        rating.setGivenTo(workerId);
        rating.setRating((int) ratingBar.getRating());
        rating.setReview(review.getText().toString());

        ApiClient.getService(this).createRating(rating).enqueue(new Callback<Rating>() {
            @Override
            public void onResponse(Call<Rating> call, Response<Rating> response) {
                Toast.makeText(PaymentRatingActivity.this, "Payment & Rating Submitted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<Rating> call, Throwable t) {
                Toast.makeText(PaymentRatingActivity.this, "Rating failed, but payment was successful", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
