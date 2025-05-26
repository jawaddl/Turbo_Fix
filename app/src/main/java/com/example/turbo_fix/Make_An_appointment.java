package com.example.turbo_fix;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.*;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;

public class Make_An_appointment extends AppCompatActivity {

    EditText symptomsEditText, problemDetailsEditText;
    Button uploadImageButton, nextBtn, selectDateButton, backButton;
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
    Calendar selectedDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setStatusBarColor(Color.parseColor("#8D6E63"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_an_appointment);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(0xFF8D6E63);
        }

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
        backButton = findViewById(R.id.backButton);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        db.collection("USER").document(clientId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        fullName = document.getString("fullName");
                        if (fullName != null) {
                            welcomeTextView.setText("שלום: " + fullName);
                        }
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

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(Make_An_appointment.this, Service_selection.class);
            intent.putExtra("clientId", clientId);
            startActivity(intent);
            finish();
        });
    }

    private void openDatePicker() {
        Calendar now = Calendar.getInstance();
        Calendar maxDate = (Calendar) now.clone();
        maxDate.add(Calendar.DAY_OF_YEAR, 6);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar pickedDate = Calendar.getInstance();
                    pickedDate.set(year, month, dayOfMonth, 0, 0, 0);
                    pickedDate.set(Calendar.MILLISECOND, 0);

                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    if (pickedDate.before(today) || pickedDate.after(maxDate)) {
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

        // Format the date part for Firebase queries
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault()); // Use device's time zone
        String selectedDateStr = dateFormat.format(date.getTime());

        // If selected date is today, filter out past times
        List<String> availableTimes = new ArrayList<>(validTimes);
        Calendar now = Calendar.getInstance();
        if (dateFormat.format(now.getTime()).equals(selectedDateStr)) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeFormat.setTimeZone(TimeZone.getDefault()); // Use device's time zone
            String currentTime = timeFormat.format(now.getTime());
            availableTimes.removeIf(time -> time.compareTo(currentTime) <= 0);
            
            if (availableTimes.isEmpty()) {
                Toast.makeText(this, "לא נותרו תורים זמינים להיום", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create a list to store unavailable times
        List<String> unavailableTimes = new ArrayList<>();

        // First check if the entire day is blocked
        db.collection("USER")
            .document("admin_settings")
            .collection("blocked_hours")
            .document(selectedDateStr)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && Boolean.TRUE.equals(documentSnapshot.getBoolean("blocked"))) {
                    Toast.makeText(this, "יום זה חסום על ידי המנהל", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Then check for specific blocked hours
                db.collection("USER")
                    .document("admin_settings")
                    .collection("blocked_hours")
                    .whereEqualTo("blocked", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                        // Get blocked hours
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String docId = doc.getId();
                        if (docId.startsWith(selectedDateStr)) {
                                String hour = doc.getString("hour");
                                if (hour != null) {
                                    unavailableTimes.add(hour);
                                }
                            }
                        }

                        // Then check booked appointments
                        db.collection("appointments_global")
                            .whereEqualTo("booked", true)
                            .get()
                            .addOnSuccessListener(appointmentsSnapshot -> {
                                // Get booked times
                                for (DocumentSnapshot doc : appointmentsSnapshot.getDocuments()) {
                                    String docId = doc.getId();
                                    if (docId.startsWith(selectedDateStr)) {
                                        String[] parts = docId.split("_");
                                        if (parts.length > 1) {
                                            unavailableTimes.add(parts[1]);
                                        }
                                    }
                                }

                                // Remove unavailable times
                                availableTimes.removeAll(unavailableTimes);

                    if (availableTimes.isEmpty()) {
                        Toast.makeText(this, "אין תורים פנויים ביום זה", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Sort available times
                    Collections.sort(availableTimes);
                    
                    String[] timeArray = availableTimes.toArray(new String[0]);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("בחר שעה פנויה");
                    builder.setItems(timeArray, (dialog, which) -> {
                                    if (selectedDate == null) {
                                        selectedDate = Calendar.getInstance();
                                    }
                                    selectedDate.setTime(date.getTime()); // Copy the date from the parameter
                                    String[] timeParts = timeArray[which].split(":");
                                    selectedDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                                    selectedDate.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
                                    selectedDate.set(Calendar.SECOND, 0);
                                    selectedDate.set(Calendar.MILLISECOND, 0);
                                    selectedDate.setTimeZone(TimeZone.getDefault()); // Use device's time zone
                                    
                                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                    displayFormat.setTimeZone(TimeZone.getDefault()); // Use device's time zone
                                    selectedDateTextView.setText(displayFormat.format(selectedDate.getTime()));
                    });
                    builder.show();
                            });
                    });
            });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryLauncher.launch(Intent.createChooser(intent, "בחר עד 4 תמונות"));
    }

    private void addThumbnail(Uri uri) {
        // Create a container for the image and delete button
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        container.setPadding(8, 8, 8, 8);

        // Create and setup the image view
        ImageView imageView = new ImageView(this);
        int size = dpToPx(100);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(size, size);
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Create and setup the delete button
        Button deleteButton = new Button(this);
        deleteButton.setText("X");
        deleteButton.setTextColor(Color.WHITE);
        deleteButton.setBackgroundColor(Color.RED);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            dpToPx(30),
            dpToPx(30)
        );
        buttonParams.gravity = Gravity.CENTER;
        buttonParams.topMargin = dpToPx(4);
        deleteButton.setLayoutParams(buttonParams);

        // Load the image
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(imageView);

        // Add click listener to delete button
        deleteButton.setOnClickListener(v -> {
            imageUris.remove(uri);
            imageContainer.removeView(container);
            if (imageUris.isEmpty()) {
                imageContainer.setVisibility(View.GONE);
            }
        });

        // Add views to container
        container.addView(imageView);
        container.addView(deleteButton);
        imageContainer.addView(container);
        imageContainer.setVisibility(View.VISIBLE);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void checkDateAvailability(String description) {
        // First check if user already has an appointment
        db.collection("USER").document(clientId)
                .collection("appointments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "כבר קיים לך תור! לא ניתן לקבוע תור נוסף.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Check if this time slot is already booked by another user
                    db.collection("USER")
                            .get()
                        .addOnSuccessListener(userSnapshots -> {
                            final boolean[] isTimeSlotTaken = {false};
                            final int[] processedUsers = {0};
                            final int totalUsers = userSnapshots.size() - 1; // Minus admin_settings

                            for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                                if (userDoc.getId().equals("admin_settings")) {
                                    continue;
                                }

                                userDoc.getReference()
                                    .collection("appointments")
                                    .whereEqualTo("date", new com.google.firebase.Timestamp(new Date(selectedDate.getTimeInMillis())))
                                    .get()
                                    .addOnSuccessListener(appointmentSnapshots -> {
                                        if (!appointmentSnapshots.isEmpty()) {
                                            isTimeSlotTaken[0] = true;
                                        }
                                        
                                        processedUsers[0]++;
                                        if (processedUsers[0] == totalUsers) {
                                            // All users checked
                                            if (isTimeSlotTaken[0]) {
                                                Toast.makeText(Make_An_appointment.this, 
                                                    "תאריך זה כבר תפוס. אנא בחר תאריך אחר.", 
                                                    Toast.LENGTH_LONG).show();
                                } else {
                                                uploadAppointmentToFirebase(description);
                                            }
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה בבדיקת זמינות: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "שגיאה בבדיקת תורים קיימים: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show());
    }

    private void uploadAppointmentToFirebase(String description) {
        Map<String, Object> appointment = new HashMap<>();
        appointment.put("description", description);
        appointment.put("knowsProblem", knowsProblem);

        // Store exactly what was selected, without any timezone adjustments
        appointment.put("date", new com.google.firebase.Timestamp(selectedDate.getTime()));
        appointment.put("timestamp", new com.google.firebase.Timestamp(new Date()));

        List<String> imageUrls = new ArrayList<>();

        Runnable finishUpload = () -> {
            appointment.put("imageUrls", imageUrls);
            
            // Create the user's appointment
                    db.collection("USER").document(clientId)
                        .collection("appointments")
                        .add(appointment)
                        .addOnSuccessListener(doc -> {
                            Toast.makeText(this, "הבקשה נשלחה בהצלחה!", Toast.LENGTH_SHORT).show();
                            
                            // Return to Client_Activity
                            Intent intent = new Intent(Make_An_appointment.this, Client_Activity.class);
                            intent.putExtra("clientId", clientId);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        };

        if (imageUris.isEmpty()) {
            finishUpload.run();
        } else {
            final int totalImages = imageUris.size();
            final int[] uploadedCount = {0};
            for (Uri uri : imageUris) {
                StorageReference ref = storage.getReference("appointment_images/" + UUID.randomUUID());
                ref.putFile(uri).continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                }).addOnSuccessListener(uriResult -> {
                    imageUrls.add(uriResult.toString());
                    uploadedCount[0]++;
                    if (uploadedCount[0] == totalImages) {
                        finishUpload.run();
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בהעלאת תמונה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }
}

