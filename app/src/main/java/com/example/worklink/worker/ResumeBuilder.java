package com.example.worklink.worker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.widget.Toast;

import com.example.worklink.DBHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class ResumeBuilder {

    private Context context;
    private DBHelper dbHelper;

    public ResumeBuilder(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public File generateResume(int workerId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // 1. Get Basic Info
        Cursor cursor = db.rawQuery(
                "SELECT u.name, u.phone, w.skills, w.rating " +
                "FROM users u JOIN worker_profile w ON u.id = w.worker_id " +
                "WHERE u.id = ?", new String[]{String.valueOf(workerId)});

        File file = null;
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String phone = cursor.getString(1);
            String skills = cursor.getString(2);
            double rating = cursor.getDouble(3);

            // 2. Start PDF Creation
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

            String expQuery = "SELECT j.title, j.description, j.location, j.job_date, " +
                    "CASE WHEN b.actual_days IS NULL OR b.actual_days = 0 THEN j.duration_days ELSE b.actual_days END as duration, " +
                    "b.status, j.required_skills " +
                    "FROM bookings b " +
                    "JOIN jobs j ON b.job_id = j.job_id " +
                    "WHERE b.worker_id = ? AND (b.status = 'COMPLETED' OR b.status = 'TERMINATED') " +
                    "ORDER BY j.job_date DESC";
            
            Cursor expCursor = db.rawQuery(expQuery, new String[]{String.valueOf(workerId)});
            
            if (expCursor.getCount() == 0) {
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
                canvas.drawText("No verified work history found.", 70, y, paint);
            } else {
                while (expCursor.moveToNext()) {
                    if (y > 750) break; // Simple page overflow protection

                    String title = expCursor.getString(0);
                    String desc = expCursor.getString(1);
                    String loc = expCursor.getString(2);
                    String date = expCursor.getString(3);
                    int duration = expCursor.getInt(4);
                    String jobSkills = expCursor.getString(6);

                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText(title + " | " + date, 70, y, paint);
                    y += 20;
                    
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    canvas.drawText("Skills: " + jobSkills, 70, y, paint);
                    y += 20;
                    
                    canvas.drawText("Location: " + loc + " | Duration: " + duration + " days", 70, y, paint);
                    y += 20;
                    
                    // Truncate description if too long for PDF line
                    String d = desc.length() > 60 ? desc.substring(0, 57) + "..." : desc;
                    canvas.drawText("Description: " + d, 70, y, paint);
                    y += 40;
                }
            }
            expCursor.close();

            // Footer
            paint.setTextSize(10);
            paint.setColor(Color.GRAY);
            canvas.drawText("Generated by WorkLink - Professional Service Platform", 180, 810, paint);

            pdfDocument.finishPage(page);
            file = new File(context.getExternalFilesDir(null), name.replace(" ", "_") + "_Resume.pdf");

            try {
                pdfDocument.writeTo(new FileOutputStream(file));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                pdfDocument.close();
            }
        }
        cursor.close();
        return file;
    }
}
