package com.example.worklink;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT id, role FROM users WHERE username=? AND password=?",
                    new String[]{username.getText().toString(), password.getText().toString()}
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
                finish(); // Close login activity

            } else {
                Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
        });

        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }
}
