package com.example.turbo_fix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Registration extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        // קישור רכיבי הממשק לקוד
        EditText fullName = findViewById(R.id.full_name);
        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.email_password);
        EditText phoneNumber = findViewById(R.id.phone_number);
        EditText licensePlate = findViewById(R.id.license_plate);
        Button submitButton = findViewById(R.id.submit_button);

        // ניהול Insets עבור email
        ViewCompat.setOnApplyWindowInsetsListener(email, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // לחיצה על כפתור המשך
        submitButton.setOnClickListener(v -> {
            String name = fullName.getText().toString().trim();
            String emailText = email.getText().toString().trim();
            String passwordText = password.getText().toString().trim();
            String phone = phoneNumber.getText().toString().trim();
            String vehicle = licensePlate.getText().toString().trim();

            // בדיקות תקינות
            if (name.isEmpty()) { showToast("יש למלא שם מלא"); return; }
            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) { showToast("האימייל שהוזן אינו תקין"); return; }
            if (passwordText.length() < 6) { showToast("הסיסמה חייבת לכלול לפחות 6 תווים"); return; }
            if (phone.length() != 10 || !phone.matches("\\d+")) { showToast("מספר הטלפון חייב לכלול 10 ספרות"); return; }
            if (vehicle.length() < 7 || vehicle.length() > 8 || !vehicle.matches("\\d+")) { showToast("מספר לוחית הרישוי חייב לכלול 7 או 8 ספרות"); return; }

            // מעבר למסך ההרשמה השני עם הנתונים
            Intent intent = new Intent(Registration.this, Registration2.class);
            intent.putExtra("fullName", name);
            intent.putExtra("email", emailText);
            intent.putExtra("password", passwordText);
            intent.putExtra("phoneNumber", phone);  // שם מתוקן
            intent.putExtra("licensePlate", vehicle);  // שם מתוקן
            startActivity(intent);
        });
    }

    private void showToast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
