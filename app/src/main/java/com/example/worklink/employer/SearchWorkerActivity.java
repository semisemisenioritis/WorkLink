package com.example.worklink.employer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.FirestoreManager;
import com.example.worklink.R;
import com.example.worklink.models.Application;
import com.example.worklink.models.Job;
import com.example.worklink.models.User;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchWorkerActivity extends AppCompatActivity {

    Spinner spJobs;
    Button btnWithdrawJob, btnConfirmBooking;
    ImageButton btnBack;
    LinearLayout layoutJobActions, layoutFilters;
    CheckBox cbSortRating;
    ChipGroup chipGroupSkills;
    ListView listView;
    
    ArrayList<WorkerRecord> currentWorkerRecords = new ArrayList<>();
    ArrayList<Job> employerJobs = new ArrayList<>();
    ArrayList<String> jobTitles = new ArrayList<>();
    
    String employerId;
    Set<String> selectedFilterSkills = new HashSet<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_search_worker);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        employerId = sharedPreferences.getString("userId", "");

        if (TextUtils.isEmpty(employerId) && FirebaseAuth.getInstance().getCurrentUser() != null) {
            employerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        btnBack = findViewById(R.id.btnBack);
        spJobs = findViewById(R.id.spJobs);
        btnWithdrawJob = findViewById(R.id.btnWithdrawJob);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        layoutJobActions = findViewById(R.id.layoutJobActions);
        layoutFilters = findViewById(R.id.layoutFilters);
        cbSortRating = findViewById(R.id.cbSortRating);
        chipGroupSkills = findViewById(R.id.chipGroupSkills);
        listView = findViewById(R.id.listWorkers);

        btnBack.setOnClickListener(v -> finish());

        loadEmployerJobs();

        spJobs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!employerJobs.isEmpty()) {
                    Job selectedJob = employerJobs.get(position);
                    layoutJobActions.setVisibility(View.VISIBLE);
                    layoutFilters.setVisibility(View.VISIBLE);
                    
                    setupSkillChips(selectedJob.getRequiredSkills());
                    updateActionButtons(selectedJob.getJobId());
                    loadApplicants(selectedJob.getJobId());
                } else {
                    layoutJobActions.setVisibility(View.GONE);
                    layoutFilters.setVisibility(View.GONE);
                    currentWorkerRecords.clear();
                    applyFiltersAndSort();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        cbSortRating.setOnCheckedChangeListener((buttonView, isChecked) -> applyFiltersAndSort());

        btnWithdrawJob.setOnClickListener(v -> {
            int pos = spJobs.getSelectedItemPosition();
            if (pos != AdapterView.INVALID_POSITION && !employerJobs.isEmpty()) {
                withdrawJob(employerJobs.get(pos).getJobId());
            }
        });

        btnConfirmBooking.setOnClickListener(v -> {
            int pos = spJobs.getSelectedItemPosition();
            if (pos != AdapterView.INVALID_POSITION && !employerJobs.isEmpty()) {
                confirmBooking(employerJobs.get(pos).getJobId());
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
        if (TextUtils.isEmpty(skills)) return;

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
        db.collection("jobs")
            .whereEqualTo("employerId", employerId)
            .whereEqualTo("status", "OPEN")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                employerJobs.clear();
                jobTitles.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Job job = doc.toObject(Job.class);
                    employerJobs.add(job);
                    jobTitles.add(job.getTitle());
                }

                if (jobTitles.isEmpty()) {
                    jobTitles.add("No Open Jobs Found");
                    layoutJobActions.setVisibility(View.GONE);
                    layoutFilters.setVisibility(View.GONE);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white_text, jobTitles);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spJobs.setAdapter(adapter);
            });
    }

    private void loadApplicants(String jobId) {
        currentWorkerRecords.clear();
        db.collection("applications")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Application app = doc.toObject(Application.class);
                    fetchWorkerDetails(app);
                }
            });
    }

    private void fetchWorkerDetails(Application app) {
        db.collection("users").document(app.getWorkerId()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null && user.getAvailability() != null && user.getAvailability() == 1) {
                    currentWorkerRecords.add(new WorkerRecord(
                        app.getApplicationId(), user.getId(), user.getName(), 
                        user.getSkills(), user.getWorkerRating() != null ? user.getWorkerRating() : 0.0
                    ));
                    applyFiltersAndSort();
                }
            }
        });
    }

    private void applyFiltersAndSort() {
        ArrayList<WorkerRecord> filteredList = new ArrayList<>();
        for (WorkerRecord w : currentWorkerRecords) {
            boolean match = true;
            String workerSkills = (w.skills != null ? w.skills.toLowerCase() : "");
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
                tv.setText(w.name + " | " + (w.skills != null ? w.skills : "No skills") + " | ⭐" + String.format("%.1f", w.rating));
                return v;
            }
        };
        listView.setAdapter(adapter);
    }

    private void updateActionButtons(String jobId) {
        db.collection("bookings").whereEqualTo("jobId", jobId).get().addOnSuccessListener(snaps -> {
            int acceptedCount = snaps.size();
            btnConfirmBooking.setEnabled(acceptedCount > 0);
            btnConfirmBooking.setAlpha(acceptedCount > 0 ? 1.0f : 0.5f);
        });
    }

    private void acceptApplication(String appId, String workerId) {
        db.collection("applications").document(appId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String jobId = doc.getString("jobId");
                Map<String, Object> booking = new HashMap<>();
                booking.put("jobId", jobId);
                booking.put("workerId", workerId);
                booking.put("status", "ACCEPTED");
                
                db.collection("bookings").add(booking).addOnSuccessListener(ref -> {
                    db.collection("applications").document(appId).update("status", "accepted");
                    checkCapacityAndReload(jobId);
                });
            }
        });
    }

    private void checkCapacityAndReload(String jobId) {
        db.collection("jobs").document(jobId).get().addOnSuccessListener(doc -> {
            Job job = doc.toObject(Job.class);
            db.collection("bookings").whereEqualTo("jobId", jobId).get().addOnSuccessListener(snaps -> {
                if (job != null && snaps.size() >= job.getWorkersNeeded()) {
                    db.collection("jobs").document(jobId).update("status", "FILLED");
                }
                loadEmployerJobs();
            });
        });
    }

    private void rejectApplication(String appId) {
        db.collection("applications").document(appId).update("status", "rejected")
            .addOnSuccessListener(aVoid -> {
                int pos = spJobs.getSelectedItemPosition();
                if (pos != AdapterView.INVALID_POSITION && !employerJobs.isEmpty()) {
                    loadApplicants(employerJobs.get(pos).getJobId());
                }
            });
    }

    private void withdrawJob(String jobId) {
        db.collection("jobs").document(jobId).update("status", "CANCELLED");
        db.collection("applications").whereEqualTo("jobId", jobId).get().addOnSuccessListener(snaps -> {
            for (DocumentSnapshot doc : snaps) {
                doc.getReference().update("status", "cancelled");
            }
        });
        loadEmployerJobs();
    }

    private void confirmBooking(String jobId) {
        db.collection("bookings").whereEqualTo("jobId", jobId).get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) return;
            new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage("Continue with " + snaps.size() + " workers?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.collection("jobs").document(jobId).update("status", "FILLED");
                    db.collection("applications").whereEqualTo("jobId", jobId)
                        .whereEqualTo("status", "pending").get().addOnSuccessListener(pending -> {
                            for (DocumentSnapshot doc : pending) doc.getReference().update("status", "rejected");
                            loadEmployerJobs();
                        });
                })
                .setNegativeButton("No", null).show();
        });
    }

    static class WorkerRecord {
        String appId, id;
        String name, skills;
        double rating;
        WorkerRecord(String appId, String id, String name, String skills, double rating) {
            this.appId = appId; this.id = id; this.name = name; this.skills = skills; this.rating = rating;
        }
    }
}
