package com.example.turbo_fix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = this.getWindow();
        window.setStatusBarColor(0xFF8D6E63);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        firestore = FirebaseFirestore.getInstance();

        loginButton.setOnClickListener(view -> loginUser());

        // הגדרת מאזין לכפתור ההרשמה
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Registration.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();  // הוספתי trim כדי לוודא שאין רווחים
        String password = passwordEditText.getText().toString().trim();  // הוספתי trim כדי לוודא שאין רווחים

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            Toast.makeText(MainActivity.this, "ההתחברות הצליחה", Toast.LENGTH_SHORT).show();
                            // המעבר לאקטיביטי הבא
                            //startActivity(new Intent(MainActivity.this, DashboardActivity.class));  // הוספתי את המעבר לאקטיביטי
                        } else {
                            Toast.makeText(MainActivity.this, "פרטי ההתחברות שגויים", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("LoginError", "שגיאה בביצוע השאילתה", task.getException()); // הדפסת שגיאה אם קיימת
                        Toast.makeText(MainActivity.this, "שגיאה בהתחברות", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
