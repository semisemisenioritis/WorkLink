package com.example.worklink;

import com.example.worklink.models.Application;
import com.example.worklink.models.Job;
import com.example.worklink.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class FirestoreManager {
    private static FirestoreManager instance;
    private final FirebaseFirestore db;

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    // --- USER OPERATIONS ---

    public Task<Void> saveUser(User user) {
        return db.collection("users").document(user.getId()).set(user);
    }

    public Task<DocumentSnapshot> getUser(String userId) {
        return db.collection("users").document(userId).get();
    }

    // --- JOB OPERATIONS ---

    public Task<Void> postJob(Job job) {
        CollectionReference jobsRef = db.collection("jobs");
        String id = jobsRef.document().getId();
        job.setJobId(id);
        return jobsRef.document(id).set(job);
    }

    public Query getAvailableJobsQuery() {
        // Simplified query: Removing orderBy to bypass the need for a composite index while it builds
        return db.collection("jobs")
                .whereEqualTo("status", "OPEN");
    }

    // --- APPLICATION OPERATIONS ---

    public Task<Void> applyForJob(Application application) {
        CollectionReference appRef = db.collection("applications");
        String id = appRef.document().getId();
        application.setApplicationId(id);
        return appRef.document(id).set(application);
    }

    public Query getApplicationsForWorker(String workerId) {
        return db.collection("applications")
                .whereEqualTo("workerId", workerId);
    }
}
