package com.example.worklink;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.worklink.worker.WorkerDashBoard;
import com.example.worklink.employer.EmployerDashBoard;

public class LoginActivity extends AppCompatActivity {

    EditText username, password;
    Button login, registerBtn;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        login = findViewById(R.id.btnLogin);
        registerBtn = findViewById(R.id.btnRegister);

        dbHelper = new DBHelper(this);

        login.setOnClickListener(v -> {
            String userText = username.getText().toString().trim();
            String passText = password.getText().toString().trim();

            // 1. Check for empty fields
            if (TextUtils.isEmpty(userText) || TextUtils.isEmpty(passText)) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Alphanumeric check for username (to match registration style)
            if (!userText.matches("^[a-zA-Z0-9]+$")) {
                username.setError("Invalid username format");
                return;
            }

            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT id, role FROM users WHERE username=? AND password=?",
                    new String[]{userText, passText}
            );

            if (cursor.moveToFirst()) {
                int userId = cursor.getInt(0);
                String role = cursor.getString(1);

                // Save user session
                SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("userId", userId);
                editor.putString("role", role);
                editor.apply();

                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                if (role.equals("Worker")) {
                    startActivity(new Intent(this, WorkerDashBoard.class));
                } else {
                    startActivity(new Intent(this, EmployerDashBoard.class));
                }
                finish();

            } else {
                Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
        });

        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }
}
