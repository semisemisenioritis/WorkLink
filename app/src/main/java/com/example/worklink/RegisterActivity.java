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

            ContentValues values = new ContentValues();
            values.put("name", name.getText().toString());
            values.put("phone", phone.getText().toString());
            values.put("username", username.getText().toString());
            values.put("password", password.getText().toString());
            values.put("role", role.getSelectedItem().toString());

            db.insert("users", null, values);

            Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
