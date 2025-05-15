package com.example.turbo_fix;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Registration2 extends AppCompatActivity {

    private EditText mileage, accidentDetails;
    private Spinner serviceLevelSpinner, accidentSpinner;
    private Button uploadImageButton, submitButton;
    private List<Uri> imageUris = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration2);

        // אתחול Firestore ו-Firebase Storage
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // קישור רכיבי הממשק
        mileage = findViewById(R.id.mileage);
        accidentDetails = findViewById(R.id.accident_details);
        serviceLevelSpinner = findViewById(R.id.service_level_spinner);
        accidentSpinner = findViewById(R.id.accident_spinner);
        uploadImageButton = findViewById(R.id.upload_image_button);
        submitButton = findViewById(R.id.submit_button);

        // אתחול Spinner של רמת טיפול
        ArrayAdapter<CharSequence> serviceLevelAdapter = ArrayAdapter.createFromResource(this,
                R.array.service_level_options, android.R.layout.simple_spinner_item);
        serviceLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceLevelSpinner.setAdapter(serviceLevelAdapter);

        // אתחול Spinner של תאונה
        ArrayAdapter<CharSequence> accidentAdapter = ArrayAdapter.createFromResource(this,
                R.array.accident_options, android.R.layout.simple_spinner_item);
        accidentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accidentSpinner.setAdapter(accidentAdapter);

        // כפתור העלאת תמונה
        uploadImageButton.setOnClickListener(v -> openImagePicker());

        // כפתור השלים
        submitButton.setOnClickListener(v -> {
            String mileageText = mileage.getText().toString().trim();
            String accidentText = accidentDetails.getText().toString().trim();

            if (mileageText.isEmpty()) {
                showToast("יש להזין קילומטראז'");
                return;
            }

            // קבלת נתונים ממסך 1
            Intent intent = getIntent();
            String name = intent.getStringExtra("fullName");
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            String phone = intent.getStringExtra("phoneNumber");
            String vehicle = intent.getStringExtra("licensePlate");

            String serviceLevel = serviceLevelSpinner.getSelectedItem().toString();
            String accidentStatus = accidentSpinner.getSelectedItem().toString();

            // יצירת Map עם כל הנתונים
            Map<String, Object> user = new HashMap<>();
            user.put("fullName", name);
            user.put("email", email);
            user.put("password", password);
            user.put("phone", phone);
            user.put("vehicle", vehicle);
            user.put("mileage", mileageText);
            user.put("accidentDetails", accidentText);
            user.put("serviceLevel", serviceLevel);
            user.put("accidentStatus", accidentStatus);

            // יצירת מזהה ייחודי למשתמש בעזרת ה-HashUtil
            String userId = db.collection("USER").document().getId();
            String shortUserId = HashUtil.generateShortHash(userId);
            user.put("userId", shortUserId);  // הוספת המזהה הקצר למפה

            // אם יש תמונות, עלינו להעלות אותן ל-Firebase Storage
            if (!imageUris.isEmpty()) {
                uploadImagesToFirebase(imageUris, user);
            } else {
                saveUserToFirestore(user, shortUserId); // אם אין תמונות, נשמור רק את הנתונים
            }
        });

        // שינוי שדה תאונה
        if (accidentSpinner != null) {
            accidentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
                    if (position == 1) { // אם נבחר "לא"
                        accidentDetails.setVisibility(View.GONE);
                        accidentDetails.setText("");
                    } else if (position == 0) { // אם נבחר "כן"
                        accidentDetails.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {}
            });
        }
    }

    // פתיחת אלבום התמונות
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // מאפשר לבחור יותר מתמונה אחת
        imagePickerLauncher.launch(intent);
    }

    // תוצאה מהפתיחה של גלריית התמונות
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            // אם נבחרו כמה תמונות
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                if (imageUris.size() < 5) {
                                    imageUris.add(imageUri); // הוספת כל תמונה לרשימה
                                }
                            }
                        } else if (result.getData().getData() != null) {
                            // אם נבחרה תמונה אחת
                            if (imageUris.size() < 5) {
                                imageUris.add(result.getData().getData());
                            }
                        }
                    }
                }
            });

    // העלאת התמונות ל-Firebase Storage
    private void uploadImagesToFirebase(List<Uri> imageUris, Map<String, Object> user) {
        for (Uri imageUri : imageUris) {
            StorageReference imageRef = storageRef.child("images/" + System.currentTimeMillis() + ".jpg");

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // שמירה של כל קישור לתמונה ב-Map
                            if (!user.containsKey("imageUris")) {
                                user.put("imageUris", new ArrayList<String>());
                            }
                            List<String> imageUrls = (List<String>) user.get("imageUris");
                            imageUrls.add(uri.toString());  // הוספת הקישור
                            user.put("imageUris", imageUrls); // עדכון ה-Map

                            if (imageUrls.size() == imageUris.size()) {
                                saveUserToFirestore(user, user.get("userId").toString());  // שמירה ב-Firestore אם כל התמונות הועלו
                            }
                        }).addOnFailureListener(e -> showToast("הייתה שגיאה בקבלת קישור התמונה"));
                    })
                    .addOnFailureListener(e -> showToast("הייתה שגיאה בהעלאת התמונה"));
        }
    }

    // שמירת נתוני המשתמש ב-Firestore
    private void saveUserToFirestore(Map<String, Object> user, String userId) {
        db.collection("USER").document(userId)  // שימוש במזההה הייחודי
                .set(user)
                .addOnSuccessListener(documentReference -> {
                    showToast("ההרשמה הושלמה בהצלחה!");
                    Intent clientIntent = new Intent(Registration2.this, Client_Activity.class);
                    clientIntent.putExtra("clientId", userId);  // שליחה של המזההה
                    startActivity(clientIntent);
                    finish();
                })
                .addOnFailureListener(e -> showToast("הייתה שגיאה בשמירת הנתונים"));
    }

    // הצגת הודעות טוסט
    private void showToast(String message) {
        Toast.makeText(Registration2.this, message, Toast.LENGTH_SHORT).show();
    }
}