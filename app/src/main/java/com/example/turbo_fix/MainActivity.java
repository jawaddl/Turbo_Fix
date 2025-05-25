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

        // בדיקה אם המשתמש הוא ADMIN
        if (email.equals("a") && password.equals("b")) {
            Intent intent = new Intent(MainActivity.this, Select_admin.class);
            startActivity(intent);
            finish();
            return;
        }

        firestore.collection("USER")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                            String clientId = documentSnapshot.getId();

                            Intent intent = new Intent(MainActivity.this, Client_Activity.class);
                            intent.putExtra("clientId", clientId);
                            startActivity(intent);
                            finish();

                            Toast.makeText(MainActivity.this, "ההתחברות הצליחה", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "פרטי ההתחברות שגויים", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "שגיאה בהתחברות", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}


