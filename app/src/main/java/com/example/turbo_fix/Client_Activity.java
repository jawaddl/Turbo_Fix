package com.example.turbo_fix;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Client_Activity extends AppCompatActivity {

    private TextView clientIdTextView, clientNameTextView, carTypeTextView, kilometersTextView, carModelTextView;
    private TextView noAppointmentTextView;
    private Button button_tor;
    private FirebaseFirestore db;
    private String clientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        getWindow().setStatusBarColor(Color.parseColor("#8D6E63"));

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        clientIdTextView = findViewById(R.id.clientIdTextView);
        clientNameTextView = findViewById(R.id.clientNameTextView);
        carModelTextView = findViewById(R.id.carModelTextView);
        kilometersTextView = findViewById(R.id.kilometersTextView);
        carTypeTextView = findViewById(R.id.carTypeTextView);
        noAppointmentTextView = findViewById(R.id.noAppointmentTextView);
        button_tor = findViewById(R.id.button_tor);
        ImageButton wrenchButton = findViewById(R.id.wrenchButton);
        ImageButton refreshButton = findViewById(R.id.refreshButton);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        clientId = intent.getStringExtra("clientId");

        if (clientId != null && !clientId.isEmpty()) {
            clientIdTextView.setText("מזהה לקוח: " + clientId);
            fetchClientData(clientId);
            checkAppointment(clientId);
        } else {
            Toast.makeText(this, "לא התקבל מזהה לקוח", Toast.LENGTH_SHORT).show();
        }

        refreshButton.setOnClickListener(v -> {
            if (clientId != null && !clientId.isEmpty()) {
                checkAppointment(clientId);
            }
        });

        wrenchButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(Client_Activity.this, v);
            popupMenu.getMenu().add("עדכון קילומטראז׳");
            popupMenu.getMenu().add("שינוי מספר טלפון");
            popupMenu.getMenu().add("שינוי סיסמא");

            popupMenu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                switch (title) {
                    case "עדכון קילומטראז׳":
                        showUpdateDialog("קילומטראז׳ חדש", "mileage", InputType.TYPE_CLASS_NUMBER);
                        break;
                    case "שינוי מספר טלפון":
                        showUpdateDialog("מספר טלפון חדש", "phone", InputType.TYPE_CLASS_PHONE);
                        break;
                    case "שינוי סיסמא":
                        showUpdateDialog("סיסמה חדשה", "password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        break;
                }
                return true;
            });

            popupMenu.show();
        });

        button_tor.setOnClickListener(v -> {
            Intent torIntent = new Intent(Client_Activity.this, Service_selection.class);
            torIntent.putExtra("clientId", clientId);
            startActivity(torIntent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (clientId != null && !clientId.isEmpty()) {
            checkAppointment(clientId); // ריענון נתוני תור
        }
    }

    private void fetchClientData(String clientId) {
        DocumentReference clientRef = db.collection("USER").document(clientId);

        clientRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String fullName = document.getString("fullName");
                String vehicle = document.getString("vehicle");
                String mileage = document.getString("mileage");
                String phone = document.getString("phone");

                SpannableString nameSpan = new SpannableString("  שלום: " + fullName);
                nameSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, nameSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                nameSpan.setSpan(new RelativeSizeSpan(1.3f), 0, nameSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                clientNameTextView.setText(nameSpan);

                setStyledText(carTypeTextView, "מספר לוחית", vehicle != null ? "  " + vehicle : "לא קיים");
                setStyledText(kilometersTextView, "ק\"מ", mileage != null ? mileage : "לא קיים");
                setStyledText(carModelTextView, "מספר בעלים", phone != null ? phone : "לא קיים");
            } else {
                Toast.makeText(Client_Activity.this, "הלקוח לא נמצא", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(Client_Activity.this, "שגיאה בהבאת הנתונים", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkAppointment(String clientId) {
        db.collection("USER").document(clientId)
                .collection("appointments")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot appointment = queryDocumentSnapshots.getDocuments().get(0);
                        Date appointmentDate = appointment.getDate("date");
                        
                        if (appointmentDate != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            String formattedDate = dateFormat.format(appointmentDate);
                            String formattedTime = timeFormat.format(appointmentDate);
                            noAppointmentTextView.setText("תור נקבע לתאריך - " + formattedDate + "\n" + "בשעה: " + formattedTime);
                            noAppointmentTextView.setTextColor(Color.parseColor("#000000"));
                            noAppointmentTextView.setVisibility(View.VISIBLE);
                        } else {
                            noAppointmentTextView.setText("תור קיים אך חסר מידע");
                            noAppointmentTextView.setTextColor(Color.RED);
                            noAppointmentTextView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        noAppointmentTextView.setText("לא קיים זימון תור");
                        noAppointmentTextView.setTextColor(Color.BLACK);
                        noAppointmentTextView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    noAppointmentTextView.setText("שגיאה בבדיקת תור");
                    noAppointmentTextView.setTextColor(Color.RED);
                    noAppointmentTextView.setVisibility(View.VISIBLE);
                });
    }

    private void setStyledText(TextView textView, String label, String value) {
        SpannableString spannable = new SpannableString(label + "\n" + value);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(1.1f), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannable);
    }

    private void showUpdateDialog(String title, String fieldKey, int inputType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setInputType(inputType);
        input.setHint(title);
        builder.setView(input);

        builder.setPositiveButton("שמור", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                updateFieldInFirestore(fieldKey, newValue);
            } else {
                Toast.makeText(this, "השדה ריק", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("בטל", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateFieldInFirestore(String fieldKey, String newValue) {
        DocumentReference clientRef = db.collection("USER").document(clientId);
        clientRef.update(fieldKey, newValue)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "המידע עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                    fetchClientData(clientId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "עדכון נכשל", Toast.LENGTH_SHORT).show();
                });
    }
}
