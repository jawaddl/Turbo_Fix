package com.example.turbo_fix;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Service_selection extends AppCompatActivity {

    Button scheduleAppointmentBtn, urgentCallBtn, backToClientBtn;
    String clientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_selection);

        scheduleAppointmentBtn = findViewById(R.id.scheduleAppointmentBtn);
        urgentCallBtn = findViewById(R.id.urgentCallBtn);
        backToClientBtn = findViewById(R.id.backToClientBtn); // כפתור חזרה

        // קבלת מזהה הלקוח
        Intent intent = getIntent();
        clientId = intent.getStringExtra("clientId");

        scheduleAppointmentBtn.setOnClickListener(v -> {
            Intent torIntent = new Intent(Service_selection.this, Make_An_appointment.class);
            torIntent.putExtra("clientId", clientId);
            startActivity(torIntent);
        });

        urgentCallBtn.setOnClickListener(v -> {
            Intent intentCall = new Intent(Intent.ACTION_DIAL);
            intentCall.setData(Uri.parse("tel:0536256922"));
            startActivity(intentCall);
        });

        backToClientBtn.setOnClickListener(v -> {
            Intent backIntent = new Intent(Service_selection.this, Client_Activity.class);
            backIntent.putExtra("clientId", clientId); // אם אתה רוצה להעביר חזרה את המזהה
            startActivity(backIntent);
            finish(); // סוגר את המסך הנוכחי כדי שלא ייערם
        });
    }
}
