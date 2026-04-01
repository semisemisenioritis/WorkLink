package com.example.worklink;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "WorkLinkDB";
    public static final int DB_VERSION = 7; // Updated to 7 for email column

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT, phone TEXT UNIQUE, username TEXT UNIQUE, password TEXT NOT NULL, role TEXT CHECK(role IN ('Worker','Employer')), created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE worker_profile (worker_id INTEGER PRIMARY KEY, email TEXT, skills TEXT, experience INTEGER, availability INTEGER DEFAULT 1, rating REAL DEFAULT 0, total_jobs INTEGER DEFAULT 0, FOREIGN KEY(worker_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE employer_profile (employer_id INTEGER PRIMARY KEY, email TEXT, company_name TEXT, location TEXT, rating REAL DEFAULT 0, total_jobs_posted INTEGER DEFAULT 0, FOREIGN KEY(employer_id) REFERENCES users(id))");
        
        db.execSQL("CREATE TABLE jobs (job_id INTEGER PRIMARY KEY AUTOINCREMENT, employer_id INTEGER, title TEXT, description TEXT, location TEXT, wage REAL, required_skills TEXT, workers_needed INTEGER DEFAULT 1, duration_days INTEGER DEFAULT 1, job_date DATE, status TEXT DEFAULT 'OPEN', created_at DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(employer_id) REFERENCES users(id))");
        
        db.execSQL("CREATE TABLE bookings (booking_id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER, worker_id INTEGER, status TEXT DEFAULT 'PENDING', actual_days INTEGER, check_in_time DATETIME, check_out_time DATETIME, FOREIGN KEY(job_id) REFERENCES jobs(job_id), FOREIGN KEY(worker_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE ratings (rating_id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, given_by INTEGER, given_to INTEGER, rating INTEGER CHECK(rating BETWEEN 1 AND 5), review TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(booking_id) REFERENCES bookings(booking_id))");
        db.execSQL("CREATE TABLE payments (payment_id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, amount REAL, payment_status TEXT DEFAULT 'PENDING', payment_method TEXT, payment_date DATETIME, FOREIGN KEY(booking_id) REFERENCES bookings(booking_id))");
        
        db.execSQL("CREATE TABLE applications (" +
                "application_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "job_id INTEGER, " +
                "worker_id INTEGER, " +
                "status TEXT DEFAULT 'pending' CHECK(status IN ('pending', 'accepted', 'rejected', 'cancelled', 'withdrawn')), " +
                "applied_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "message TEXT, " +
                "FOREIGN KEY(job_id) REFERENCES jobs(job_id), " +
                "FOREIGN KEY(worker_id) REFERENCES users(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE applications (" +
                    "application_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "job_id INTEGER, " +
                    "worker_id INTEGER, " +
                    "status TEXT DEFAULT 'pending' CHECK(status IN ('pending', 'accepted', 'rejected')), " +
                    "applied_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "message TEXT, " +
                    "FOREIGN KEY(job_id) REFERENCES jobs(job_id), " +
                    "FOREIGN KEY(worker_id) REFERENCES users(id))");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE jobs ADD COLUMN workers_needed INTEGER DEFAULT 1");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE jobs ADD COLUMN duration_days INTEGER DEFAULT 1");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE applications RENAME TO applications_old");
            db.execSQL("CREATE TABLE applications (" +
                    "application_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "job_id INTEGER, " +
                    "worker_id INTEGER, " +
                    "status TEXT DEFAULT 'pending' CHECK(status IN ('pending', 'accepted', 'rejected', 'cancelled', 'withdrawn')), " +
                    "applied_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "message TEXT, " +
                    "FOREIGN KEY(job_id) REFERENCES jobs(job_id), " +
                    "FOREIGN KEY(worker_id) REFERENCES users(id))");
            db.execSQL("INSERT INTO applications (application_id, job_id, worker_id, status, applied_at, message) " +
                    "SELECT application_id, job_id, worker_id, status, applied_at, message FROM applications_old");
            db.execSQL("DROP TABLE applications_old");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE bookings ADD COLUMN actual_days INTEGER");
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE users ADD COLUMN email TEXT");
            db.execSQL("ALTER TABLE worker_profile ADD COLUMN email TEXT");
            db.execSQL("ALTER TABLE employer_profile ADD COLUMN email TEXT");
        }
    }
}
