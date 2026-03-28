package com.example.worklink;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "WorkLinkDB";
    public static final int DB_VERSION = 1;

    // Firebase Reference for Real-time Sync
    private DatabaseReference mDatabase;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        // Initialize Firebase Reference
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public DatabaseReference getFirebaseReference() {
        return mDatabase;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT UNIQUE, username TEXT UNIQUE, password TEXT NOT NULL, role TEXT CHECK(role IN ('Worker','Employer')), created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE worker_profile (worker_id INTEGER PRIMARY KEY, skills TEXT, experience INTEGER, availability INTEGER DEFAULT 1, rating REAL DEFAULT 0, total_jobs INTEGER DEFAULT 0, FOREIGN KEY(worker_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE employer_profile (employer_id INTEGER PRIMARY KEY, company_name TEXT, location TEXT, rating REAL DEFAULT 0, total_jobs_posted INTEGER DEFAULT 0, FOREIGN KEY(employer_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE jobs (job_id INTEGER PRIMARY KEY AUTOINCREMENT, employer_id INTEGER, title TEXT, description TEXT, location TEXT, wage REAL, required_skills TEXT, job_date DATE, status TEXT DEFAULT 'OPEN', created_at DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(employer_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE bookings (booking_id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER, worker_id INTEGER, status TEXT DEFAULT 'PENDING', check_in_time DATETIME, check_out_time DATETIME, FOREIGN KEY(job_id) REFERENCES jobs(job_id), FOREIGN KEY(worker_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE ratings (rating_id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, given_by INTEGER, given_to INTEGER, rating INTEGER CHECK(rating BETWEEN 1 AND 5), review TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(booking_id) REFERENCES bookings(booking_id))");
        db.execSQL("CREATE TABLE payments (payment_id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, amount REAL, payment_status TEXT DEFAULT 'PENDING', payment_method TEXT, payment_date DATETIME, FOREIGN KEY(booking_id) REFERENCES bookings(booking_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS worker_profile");
        db.execSQL("DROP TABLE IF EXISTS employer_profile");
        db.execSQL("DROP TABLE IF EXISTS jobs");
        db.execSQL("DROP TABLE IF EXISTS bookings");
        db.execSQL("DROP TABLE IF EXISTS ratings");
        db.execSQL("DROP TABLE IF EXISTS payments");
        onCreate(db);
    }
}
