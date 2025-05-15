package com.example.turbo_fix;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.*;

public class Make_An_appointment extends AppCompatActivity {

    EditText symptomsEditText, problemDetailsEditText;
    Button uploadImageButton, nextBtn, selectDateButton;
    LinearLayout imageContainer;
    RadioGroup radioGroup;
    RadioButton radioYes, radioNo;
    TextView welcomeTextView, selectedDateTextView;

    List<Uri> imageUris = new ArrayList<>();
    FirebaseFirestore db;
    FirebaseStorage storage;

    ActivityResultLauncher<Intent> galleryLauncher;
    boolean knowsProblem = false;

    String clientId;
    String fullName = "";
    Date selectedDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_an_appointment);

        clientId = getIntent().getStringExtra("clientId");
        if (clientId == null) {
            Toast.makeText(this, "אין מזהה לקוח זמין!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        symptomsEditText = findViewById(R.id.symptomsEditText);
        problemDetailsEditText = findViewById(R.id.problemDetailsEditText);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        nextBtn = findViewById(R.id.nextBtn);
        imageContainer = findViewById(R.id.imageContainer);
        radioGroup = findViewById(R.id.radioGroup);
        radioYes = findViewById(R.id.radioYes);
        radioNo = findViewById(R.id.radioNo);
        welcomeTextView = findViewById(R.id.welcomeTextView);
        selectDateButton = findViewById(R.id.selectDateButton);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        db.collection("USER").document(clientId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        fullName = document.getString("fullName");
                        welcomeTextView.setText("שלום: " + fullName);
                    }
                });

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioYes) {
                knowsProblem = true;
                problemDetailsEditText.setVisibility(View.VISIBLE);
                uploadImageButton.setVisibility(View.VISIBLE);
                imageContainer.setVisibility(View.VISIBLE);
                symptomsEditText.setVisibility(View.GONE);
            } else {
                knowsProblem = false;
                problemDetailsEditText.setVisibility(View.GONE);
                uploadImageButton.setVisibility(View.GONE);
                imageContainer.setVisibility(View.GONE);
                symptomsEditText.setVisibility(View.VISIBLE);
            }
        });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUris.clear();
                        imageContainer.removeAllViews();
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < Math.min(count, 4); i++) {
                                Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                                imageUris.add(uri);
                                addThumbnail(uri);
                            }
                        } else if (result.getData().getData() != null) {
                            Uri uri = result.getData().getData();
                            imageUris.add(uri);
                            addThumbnail(uri);
                        }
                    }
                });

        uploadImageButton.setOnClickListener(v -> openGallery());
        selectDateButton.setOnClickListener(v -> openDatePicker());

        nextBtn.setOnClickListener(v -> {
            String description = knowsProblem
                    ? problemDetailsEditText.getText().toString().trim()
                    : symptomsEditText.getText().toString().trim();

            if (description.isEmpty()) {
                Toast.makeText(this, "נא למלא תיאור", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedDate == null) {
                Toast.makeText(this, "נא לבחור תאריך ושעה!", Toast.LENGTH_SHORT).show();
                return;
            }

            checkDateAvailability(description);
        });
    }

    private void openDatePicker() {
        Calendar now = Calendar.getInstance();
        Calendar maxDate = (Calendar) now.clone();
        maxDate.add(Calendar.DAY_OF_YEAR, 6);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar pickedDate = Calendar.getInstance();
                    pickedDate.set(year, month, dayOfMonth);

                    if (pickedDate.before(now) || pickedDate.after(maxDate)) {
                        Toast.makeText(this, "ניתן לבחור תור רק לשבוע הקרוב!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    openTimePicker(pickedDate);
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(now.getTimeInMillis());
        dialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        dialog.show();
    }

    private void openTimePicker(Calendar date) {
        List<String> validTimes = Arrays.asList("08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00",
                "11:30", "12:00", "12:30", "13:00", "13:30", "14:00");

        String[] timeArray = validTimes.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("בחר שעה");
        builder.setItems(timeArray, (dialog, which) -> {
            String[] parts = timeArray[which].split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            date.set(Calendar.HOUR_OF_DAY, hour);
            date.set(Calendar.MINUTE, minute);
            date.set(Calendar.SECOND, 0);
            date.set(Calendar.MILLISECOND, 0);

            selectedDate = date.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            selectedDateTextView.setText("תאריך שנבחר: " + sdf.format(selectedDate));
        });
        builder.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryLauncher.launch(Intent.createChooser(intent, "בחר עד 4 תמונות"));
    }

    private void addThumbnail(Uri uri) {
        ImageView imageView = new ImageView(this);
        imageView.setImageURI(uri);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        imageView.setPadding(8, 8, 8, 8);
        imageContainer.addView(imageView);
    }

    private void checkDateAvailability(String description) {
        // הפורמט חייב להיות אחיד וללא תלות באזור זמן, אז נשמור תמיד ב-UTC
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
        dateKeyFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateKey = dateKeyFormat.format(selectedDate);

        db.collection("appointments_global")
                .document(dateKey)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Toast.makeText(this, "תאריך זה כבר תפוס. אנא בחר תאריך אחר.", Toast.LENGTH_LONG).show();
                    } else {
                        uploadAppointmentToFirebase(description, dateKey);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בבדיקת תאריך: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void uploadAppointmentToFirebase(String description, String dateKey) {
        Map<String, Object> appointment = new HashMap<>();
        appointment.put("description", description);
        appointment.put("knowsProblem", knowsProblem);

        // שומרים את התאריך במועד הנכון (UTC)
        appointment.put("date", selectedDate);
        appointment.put("timestamp", new Date());

        List<String> imageUrls = new ArrayList<>();

        Runnable finishUpload = () -> {
            appointment.put("imageUrls", imageUrls);
            db.collection("USER").document(clientId)
                    .collection("appointments")
                    .add(appointment)
                    .addOnSuccessListener(doc -> {
                        db.collection("appointments_global").document(dateKey).set(Collections.singletonMap("booked", true));
                        Toast.makeText(this, "הבקשה נשלחה בהצלחה!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        };

        if (imageUris.isEmpty()) {
            finishUpload.run();
        } else {
            for (Uri uri : imageUris) {
                StorageReference ref = storage.getReference("appointment_images/" + UUID.randomUUID());
                ref.putFile(uri).continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                }).addOnSuccessListener(uriResult -> {
                    imageUrls.add(uriResult.toString());
                    if (imageUrls.size() == imageUris.size()) {
                        finishUpload.run();
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בהעלאת תמונה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }
}
