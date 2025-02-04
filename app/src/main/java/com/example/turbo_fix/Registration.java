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

import com.example.turbo_fix.User;

import com.google.firebase.firestore.FirebaseFirestore;

public class Registration extends AppCompatActivity {

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        // חיבור לדאטה בייס של Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

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

            // בדיקת תקינות
            if (name.isEmpty()) {
                showToast("יש למלא שם מלא");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                showToast("האימייל שהוזן אינו תקין");
                return;
            }

            if (passwordText.length() < 6) {
                showToast("הסיסמה חייבת לכלול לפחות 6 תווים");
                return;
            }

            if (phone.length() != 10 || !phone.matches("\\d+")) {
                showToast("מספר הטלפון חייב לכלול 10 ספרות");
                return;
            }

            if (vehicle.length() < 7 || vehicle.length() > 8 || !vehicle.matches("\\d+")) {
                showToast("מספר לוחית הרישוי חייב לכלול 7 או 8 ספרות");
                return;
            }

            // יצירת אובייקט User
            String userId = firestore.collection("users").document().getId(); // יצירת מפתח ייחודי למשתמש
            User user = new User(name, emailText, passwordText, phone, vehicle);

            // שמירת הנתונים בדאטה בייס של Firestore
            firestore.collection("users").document(userId).set(user)
                    .addOnSuccessListener(aVoid -> {
                        showToast("ההרשמה בוצעה בהצלחה");
                        // מעבר לחלון הבא
                        Intent intent = new Intent(Registration.this, Registration2.class);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        showToast("הייתה בעיה בשמירה, נסה שנית");
                    });
        });
    }

    // פונקציה להצגת הודעות שגיאה
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

