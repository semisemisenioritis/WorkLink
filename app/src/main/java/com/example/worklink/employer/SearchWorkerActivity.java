package com.example.worklink.employer;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

import com.example.worklink.DBHelper;
import com.example.worklink.R;

public class SearchWorkerActivity extends AppCompatActivity {

    EditText skill;
    Button search;
    ListView listView;
    DBHelper dbHelper;
    ArrayList<String> workers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employer_activity_search_worker);

        skill = findViewById(R.id.etSkill);
        search = findViewById(R.id.btnSearch);
        listView = findViewById(R.id.listWorkers);
        dbHelper = new DBHelper(this);

        search.setOnClickListener(v -> {
            workers = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT u.name, w.skills, w.rating FROM users u " +
                            "JOIN worker_profile w ON u.id=w.worker_id " +
                            "WHERE w.skills LIKE ? AND w.availability=1",
                    new String[]{"%" + skill.getText().toString() + "%"}
            );

            while (cursor.moveToNext()) {
                String data = cursor.getString(0) + " | " +
                        cursor.getString(1) + " | ⭐" +
                        cursor.getDouble(2);
                workers.add(data);
            }

            listView.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, workers));
        });
    }
}
