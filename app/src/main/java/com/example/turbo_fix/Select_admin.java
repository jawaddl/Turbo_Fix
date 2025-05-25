package com.example.turbo_fix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Select_admin extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_admin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button customerInfoBtn = findViewById(R.id.btn_customer_info);
        Button adminActivityBtn = findViewById(R.id.btn_admin_activity);

        customerInfoBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Select_admin.this, CustomerIIIInfo.class);
            startActivity(intent);
        });

        adminActivityBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Select_admin.this, Admin_Activity.class);
            startActivity(intent);
        });
    }
}
