package com.example.worklink.worker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.widget.Toast;

import com.example.worklink.models.Booking;
import com.example.worklink.models.Job;
import com.example.worklink.models.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ResumeBuilder {

    private Context context;
    private FirebaseFirestore db;

    public ResumeBuilder(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    public void generateResume(String workerId, ResumeCallback callback) {
        db.collection("users").document(workerId).get().addOnSuccessListener(userDoc -> {
            if (!userDoc.exists()) {
                callback.onError("User not found");
                return;
            }

            User user = userDoc.toObject(User.class);
            String name = user.getName();
            String phone = user.getPhone();
            String skills = user.getSkills() != null ? user.getSkills() : "None";
            double rating = user.getWorkerRating() != null ? user.getWorkerRating() : 0.0;

            // Fetch experience
            db.collection("bookings")
                .whereEqualTo("workerId", workerId)
                .whereIn("status", List.of("COMPLETED", "TERMINATED"))
                .get()
                .addOnSuccessListener(bookingSnaps -> {
                    
                    PdfDocument pdfDocument = new PdfDocument();
                    Paint paint = new Paint();
                    Paint titlePaint = new Paint();

                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                    PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                    Canvas canvas = page.getCanvas();

                    int y = 50;

                    // Title
                    titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    titlePaint.setTextSize(24);
                    titlePaint.setColor(Color.BLUE);
                    canvas.drawText("WORKLINK RESUME", 180, y, titlePaint);
                    y += 50;

                    // Name & Contact
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    paint.setTextSize(18);
                    canvas.drawText(name, 50, y, paint);
                    y += 30;
                    
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    paint.setTextSize(14);
                    canvas.drawText("Phone: " + phone, 50, y, paint);
                    y += 25;
                    canvas.drawText("Rating: " + String.format(Locale.US, "%.1f", rating) + " / 5.0", 50, y, paint);
                    y += 40;

                    // Skills Section
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("CORE SKILLS", 50, y, paint);
                    y += 25;
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    String[] skillArray = skills.split(",");
                    for (String skill : skillArray) {
                        canvas.drawText("• " + skill.trim(), 70, y, paint);
                        y += 20;
                    }
                    y += 30;

                    // Experience Section
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("WORK EXPERIENCE", 50, y, paint);
                    y += 30;

                    if (bookingSnaps.isEmpty()) {
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
                        canvas.drawText("No verified work history found.", 70, y, paint);
                    } else {
                        // For simplicity in this callback-nested version, we only list booking basic info
                        // In a production app, you'd fetch Job titles for each booking using Tasks.whenAll
                        for (QueryDocumentSnapshot doc : bookingSnaps) {
                            if (y > 750) break;
                            Booking b = doc.toObject(Booking.class);
                            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                            canvas.drawText("Job ID: " + b.getJobId() + " | Status: " + b.getStatus(), 70, y, paint);
                            y += 20;
                            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                            canvas.drawText("Duration: " + (b.getActualDays() != null ? b.getActualDays() : "N/A") + " days", 70, y, paint);
                            y += 40;
                        }
                    }

                    // Footer
                    paint.setTextSize(10);
                    paint.setColor(Color.GRAY);
                    canvas.drawText("Generated by WorkLink - Professional Service Platform", 180, 810, paint);

                    pdfDocument.finishPage(page);
                    File file = new File(context.getExternalFilesDir(null), name.replace(" ", "_") + "_Resume.pdf");

                    try {
                        pdfDocument.writeTo(new FileOutputStream(file));
                        callback.onSuccess(file);
                    } catch (IOException e) {
                        callback.onError(e.getMessage());
                    } finally {
                        pdfDocument.close();
                    }
                });
        });
    }

    public interface ResumeCallback {
        void onSuccess(File file);
        void onError(String error);
    }
}
