package com.example.worklink.employer;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class SearchWorkerActivity extends AppCompatActivity {

    Spinner spJobs;
    Button btnWithdrawJob, btnConfirmBooking;
    LinearLayout layoutJobActions, layoutFilters;
    CheckBox cbSortRating;
    ChipGroup chipGroupSkills;
    ListView listView;
    DBHelper dbHelper;
    
    ArrayList<WorkerRecord> currentWorkerRecords = new ArrayList<>();
    
    ArrayList<String> jobTitles;
    ArrayList<Integer> jobIds;
    ArrayList<String> jobSkills;
    
    int employerId;
    Set<String> selectedFilterSkills = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_search_worker);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getInt("userId", -1);

        spJobs = findViewById(R.id.spJobs);
        btnWithdrawJob = findViewById(R.id.btnWithdrawJob);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        layoutJobActions = findViewById(R.id.layoutJobActions);
        layoutFilters = findViewById(R.id.layoutFilters);
        cbSortRating = findViewById(R.id.cbSortRating);
        chipGroupSkills = findViewById(R.id.chipGroupSkills);
        listView = findViewById(R.id.listWorkers);
        dbHelper = new DBHelper(this);

        loadEmployerJobs();

        spJobs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!jobIds.isEmpty()) {
                    int selectedJobId = jobIds.get(position);
                    String skills = jobSkills.get(position);
                    
                    layoutJobActions.setVisibility(View.VISIBLE);
                    layoutFilters.setVisibility(View.VISIBLE);
                    
                    setupSkillChips(skills);
                    updateActionButtons(selectedJobId);
                    loadApplicants(selectedJobId);
                } else {
                    layoutJobActions.setVisibility(View.GONE);
                    layoutFilters.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        cbSortRating.setOnCheckedChangeListener((buttonView, isChecked) -> applyFiltersAndSort());

        btnWithdrawJob.setOnClickListener(v -> {
            int position = spJobs.getSelectedItemPosition();
            if (position != AdapterView.INVALID_POSITION && !jobIds.isEmpty()) {
                withdrawJob(jobIds.get(position));
            }
        });

        btnConfirmBooking.setOnClickListener(v -> {
            int position = spJobs.getSelectedItemPosition();
            if (position != AdapterView.INVALID_POSITION && !jobIds.isEmpty()) {
                confirmBooking(jobIds.get(position));
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            WorkerRecord selected = (WorkerRecord) parent.getItemAtPosition(position);
            new AlertDialog.Builder(this)
                    .setTitle("Review Applicant")
                    .setMessage("Accept application from " + selected.name + "?")
                    .setPositiveButton("Accept", (dialog, which) -> acceptApplication(selected.appId, selected.id))
                    .setNegativeButton("Reject", (dialog, which) -> rejectApplication(selected.appId))
                    .setNeutralButton("Cancel", null)
                    .show();
        });
    }

    private void setupSkillChips(String skills) {
        chipGroupSkills.removeAllViews();
        selectedFilterSkills.clear();
        if (skills == null || skills.isEmpty()) return;

        String[] skillArray = skills.split(",");
        for (String skill : skillArray) {
            String cleanSkill = skill.trim();
            if (cleanSkill.isEmpty()) continue;

            Chip chip = new Chip(this);
            chip.setText(cleanSkill);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.dark_surface);
            chip.setTextColor(getResources().getColor(R.color.white));
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedFilterSkills.add(cleanSkill.toLowerCase());
                else selectedFilterSkills.remove(cleanSkill.toLowerCase());
                applyFiltersAndSort();
            });
            chipGroupSkills.addView(chip);
        }
    }

    private void loadEmployerJobs() {
        jobTitles = new ArrayList<>();
        jobIds = new ArrayList<>();
        jobSkills = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT job_id, title, workers_needed, required_skills FROM jobs WHERE employer_id=? AND status='OPEN'", 
                new String[]{String.valueOf(employerId)});

        while (cursor.moveToNext()) {
            int jId = cursor.getInt(0);
            String title = cursor.getString(1);
            int needed = cursor.getInt(2);
            String skills = cursor.getString(3);
            
            Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE job_id=?", new String[]{String.valueOf(jId)});
            int filled = 0;
            if (countCursor.moveToFirst()) filled = countCursor.getInt(0);
            countCursor.close();

            jobIds.add(jId);
            jobTitles.add(title + " (" + filled + "/" + needed + ")");
            jobSkills.add(skills);
        }
        cursor.close();

        if (jobTitles.isEmpty()) {
            jobTitles.add("No Open Jobs Found");
            layoutJobActions.setVisibility(View.GONE);
            layoutFilters.setVisibility(View.GONE);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white_text, jobTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spJobs.setAdapter(adapter);
    }

    private void loadApplicants(int jobId) {
        currentWorkerRecords.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT a.application_id, u.id, u.name, w.skills, w.rating FROM applications a " +
                "JOIN users u ON a.worker_id = u.id " +
                "JOIN worker_profile w ON u.id = w.worker_id " +
                "WHERE a.job_id = ? AND a.status = 'pending' AND w.availability = 1";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(jobId)});
        while (cursor.moveToNext()) {
            currentWorkerRecords.add(new WorkerRecord(
                cursor.getInt(0), cursor.getInt(1), cursor.getString(2), 
                cursor.getString(3), cursor.getDouble(4)
            ));
        }
        cursor.close();
        applyFiltersAndSort();
    }

    private void applyFiltersAndSort() {
        ArrayList<WorkerRecord> filteredList = new HashSet<>(currentWorkerRecords).size() > 0 ? new ArrayList<>() : new ArrayList<>();
        
        for (WorkerRecord w : currentWorkerRecords) {
            boolean match = true;
            String workerSkills = w.skills.toLowerCase();
            for (String s : selectedFilterSkills) {
                if (!workerSkills.contains(s)) {
                    match = false;
                    break;
                }
            }
            if (match) filteredList.add(w);
        }

        if (cbSortRating.isChecked()) {
            Collections.sort(filteredList, (a, b) -> Double.compare(b.rating, a.rating));
        }

        ArrayAdapter<WorkerRecord> adapter = new ArrayAdapter<WorkerRecord>(this, R.layout.list_item_white_text, filteredList) {
            @Override
            public View getView(int pos, View convert, android.view.ViewGroup parent) {
                View v = super.getView(pos, convert, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);
                WorkerRecord w = getItem(pos);
                tv.setText(w.name + " | " + w.skills + " | ⭐" + w.rating);
                return v;
            }
        };
        listView.setAdapter(adapter);
    }

    private void updateActionButtons(int jobId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE job_id=?", new String[]{String.valueOf(jobId)});
        int acceptedCount = 0;
        if (cursor.moveToFirst()) acceptedCount = cursor.getInt(0);
        cursor.close();

        btnConfirmBooking.setEnabled(acceptedCount > 0);
        btnConfirmBooking.setAlpha(acceptedCount > 0 ? 1.0f : 0.5f);
    }

    private void acceptApplication(int appId, int workerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT job_id FROM applications WHERE application_id=?", new String[]{String.valueOf(appId)});
        if (cursor.moveToFirst()) {
            int jobId = cursor.getInt(0);
            ContentValues bookingValues = new ContentValues();
            bookingValues.put("job_id", jobId);
            bookingValues.put("worker_id", workerId);
            bookingValues.put("status", "ACCEPTED");
            db.insert("bookings", null, bookingValues);
            db.execSQL("UPDATE applications SET status='accepted' WHERE application_id=?", new Object[]{appId});
            
            // Check capacity
            Cursor jobCursor = db.rawQuery("SELECT workers_needed FROM jobs WHERE job_id=?", new String[]{String.valueOf(jobId)});
            if (jobCursor.moveToFirst()) {
                int needed = jobCursor.getInt(0);
                Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE job_id=?", new String[]{String.valueOf(jobId)});
                int filled = 0;
                if (countCursor.moveToFirst()) filled = countCursor.getInt(0);
                countCursor.close();
                if (filled >= needed) db.execSQL("UPDATE jobs SET status='FILLED' WHERE job_id=?", new Object[]{jobId});
            }
            jobCursor.close();
            loadEmployerJobs(); 
        }
        cursor.close();
    }

    private void rejectApplication(int appId) {
        dbHelper.getWritableDatabase().execSQL("UPDATE applications SET status='rejected' WHERE application_id=?", new Object[]{appId});
        loadApplicants(jobIds.get(spJobs.getSelectedItemPosition()));
    }

    private void withdrawJob(int jobId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("UPDATE jobs SET status='CANCELLED' WHERE job_id=?", new Object[]{jobId});
            db.execSQL("UPDATE applications SET status='cancelled' WHERE job_id=? AND (status='accepted' OR status='pending')", new Object[]{jobId});
            db.execSQL("DELETE FROM bookings WHERE job_id=?", new Object[]{jobId});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        loadEmployerJobs(); 
    }

    private void confirmBooking(int jobId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE job_id=?", new String[]{String.valueOf(jobId)});
        int acceptedCount = 0;
        if (cursor.moveToFirst()) acceptedCount = cursor.getInt(0);
        cursor.close();

        if (acceptedCount == 0) return;

        new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage("Continue with " + acceptedCount + " workers?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SQLiteDatabase wdb = dbHelper.getWritableDatabase();
                    wdb.execSQL("UPDATE jobs SET status='FILLED' WHERE job_id=?", new Object[]{jobId});
                    wdb.execSQL("UPDATE applications SET status='rejected' WHERE job_id=? AND status='pending'", new Object[]{jobId});
                    loadEmployerJobs();
                })
                .setNegativeButton("No", null).show();
    }

    static class WorkerRecord {
        int appId, id;
        String name, skills;
        double rating;
        WorkerRecord(int appId, int id, String name, String skills, double rating) {
            this.appId = appId; this.id = id; this.name = name; this.skills = skills; this.rating = rating;
        }
    }
}
