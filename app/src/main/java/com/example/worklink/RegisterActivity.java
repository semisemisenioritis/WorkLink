package com.example.worklink;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText name, phone, username, password;
    Spinner role;
    Button register;
    ImageButton btnBack;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnBack = findViewById(R.id.btnBack);
        name = findViewById(R.id.etName);
        phone = findViewById(R.id.etPhone);
        username = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        role = findViewById(R.id.spRole);
        register = findViewById(R.id.btnSubmit);

        dbHelper = new DBHelper(this);

        btnBack.setOnClickListener(v -> finish());

        String[] roles = {"Worker", "Employer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_white_text, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        role.setAdapter(adapter);

        register.setOnClickListener(v -> {
            String nameText = name.getText().toString().trim();
            String phoneText = phone.getText().toString().trim();
            String userText = username.getText().toString().trim();
            String passText = password.getText().toString().trim();
            String selectedRole = role.getSelectedItem().toString();

            // 1. Check for empty fields
            if (TextUtils.isEmpty(nameText) || TextUtils.isEmpty(phoneText) ||
                TextUtils.isEmpty(userText) || TextUtils.isEmpty(passText)) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Name validation (Alpha only)
            if (!nameText.matches("^[a-zA-Z\\s]+$")) {
                name.setError("Name must contain only letters");
                return;
            }

            // 3. Phone validation (10 digits)
            if (!phoneText.matches("^\\d{10}$")) {
                phone.setError("Phone must be exactly 10 digits");
                return;
            }

            // 4. Username validation (Alphanumeric)
            if (!userText.matches("^[a-zA-Z0-9]+$")) {
                username.setError("Username must be alphanumeric");
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", nameText);
            values.put("phone", phoneText);
            values.put("username", userText);
            values.put("password", passText);
            values.put("role", selectedRole);

            try {
                long userId = db.insertOrThrow("users", null, values);

                if (userId != -1) {
                    if ("Worker".equals(selectedRole)) {
                        ContentValues workerValues = new ContentValues();
                        workerValues.put("worker_id", userId);
                        workerValues.put("skills", "None");
                        workerValues.put("experience", 0);
                        db.insert("worker_profile", null, workerValues);
                    } else {
                        ContentValues employerValues = new ContentValues();
                        employerValues.put("employer_id", userId);
                        db.insert("employer_profile", null, employerValues);
                    }

                    Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Username or Phone already exists", Toast.LENGTH_LONG).show();
            }
        });
    }
}
