package com.example.turbo_fix;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import android.content.SharedPreferences;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setStatusBarColor(Color.parseColor("#8D6E63"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        firestore = FirebaseFirestore.getInstance();

        loginButton.setOnClickListener(view -> loginUser());

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Registration.class);
            startActivity(intent);
            finish();
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for admin login (case insensitive)
        if (email.equalsIgnoreCase("a") && password.equalsIgnoreCase("b")) {
            // Save admin credentials
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", "a");
            editor.putString("password", "b");
            editor.apply();

            // Navigate to admin screen
            startActivity(new Intent(MainActivity.this, Select_admin.class));
            finish();
            return;
        }

        // Regular user login
        firestore.collection("USER")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                            String storedPassword = documentSnapshot.getString("password");
                            
                            // Check if password matches
                            if (password.equals(storedPassword)) {
                                String clientId = documentSnapshot.getId();
                                Intent intent = new Intent(MainActivity.this, Client_Activity.class);
                                intent.putExtra("clientId", clientId);
                                startActivity(intent);
                                finish();
                                Toast.makeText(MainActivity.this, "ההתחברות הצליחה", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "סיסמה שגויה", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "משתמש לא נמצא", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "שגיאה בהתחברות: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
