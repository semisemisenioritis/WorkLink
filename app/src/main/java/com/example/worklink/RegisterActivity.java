package com.example.worklink;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText name, phone, username, password;
    Spinner role;
    Button register;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = findViewById(R.id.etName);
        phone = findViewById(R.id.etPhone);
        username = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        role = findViewById(R.id.spRole);
        register = findViewById(R.id.btnSubmit);

        dbHelper = new DBHelper(this);

        String[] roles = {"Worker", "Employer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roles);
        role.setAdapter(adapter);

        register.setOnClickListener(v -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String selectedRole = role.getSelectedItem().toString();

            ContentValues values = new ContentValues();
            values.put("name", name.getText().toString());
            values.put("phone", phone.getText().toString());
            values.put("username", username.getText().toString());
            values.put("password", password.getText().toString());
            values.put("role", selectedRole);

            long userId = db.insert("users", null, values);

            if (userId != -1) {
                // Create profile based on role
                if ("Worker".equals(selectedRole)) {
                    ContentValues workerValues = new ContentValues();
                    workerValues.put("worker_id", userId);
                    workerValues.put("skills", "None"); // Default value
                    workerValues.put("experience", 0);
                    db.insert("worker_profile", null, workerValues);
                } else {
                    ContentValues employerValues = new ContentValues();
                    employerValues.put("employer_id", userId);
                    db.insert("employer_profile", null, employerValues);
                }

                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Registration Failed (Username/Phone may exist)", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
