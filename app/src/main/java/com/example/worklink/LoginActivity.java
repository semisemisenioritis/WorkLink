package com.example.worklink;

import android.content.Intent;
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
                    "SELECT * FROM users WHERE username=? AND password=?",
                    new String[]{username.getText().toString(), password.getText().toString()}
            );

            if (cursor.moveToFirst()) {
                String role = cursor.getString(5);

                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                if (role.equals("Worker")) {
                    startActivity(new Intent(this, WorkerDashBoard.class));
                } else {
                    //startActivity(new Intent(this, EmployerDashboard.class));
                }

            } else {
                Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
            }
        });

        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }
}