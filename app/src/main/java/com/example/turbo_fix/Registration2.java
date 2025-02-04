package com.example.turbo_fix;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Registration2 extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText mileageEditText, accidentDescriptionEditText;
    private Spinner maintenanceLevelSpinner, accidentSpinner;
    private Button uploadImageButton, saveButton;
    private ImageView vehicleImageView;
    private TextView titleTextView;
    private Uri imageUri;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration2);

        // אתחול של האובייקטים ב-XML
        titleTextView = findViewById(R.id.titleTextView);
        mileageEditText = findViewById(R.id.kilometerEditText);
        maintenanceLevelSpinner = findViewById(R.id.treatmentSpinner);
        accidentSpinner = findViewById(R.id.accidentSpinner);
        accidentDescriptionEditText = findViewById(R.id.accidentDescription);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        saveButton = findViewById(R.id.saveButton);
        vehicleImageView = findViewById(R.id.vehicleImageView);

        firestore = FirebaseFirestore.getInstance();

        // כותרת
        titleTextView.setText("פרטי רכב");

        // הגבלת שדה קילומטראז' למספרים בלבד
        mileageEditText.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source.toString().matches("\\d+")) {
                    return source;
                }
                return "";
            }
        }});

        // הגדרת ספינר לרמת טיפול
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.maintenance_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        maintenanceLevelSpinner.setAdapter(adapter);

        // הגדרת ספינר לשאלה אם הרכב עבר תאונה
        ArrayAdapter<CharSequence> accidentAdapter = ArrayAdapter.createFromResource(this,
                R.array.accident_options, android.R.layout.simple_spinner_item);
        accidentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accidentSpinner.setAdapter(accidentAdapter);

        // הצגת שדה תאונה רק אם נבחר "כן"
        accidentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (accidentSpinner.getSelectedItem().toString().equals("כן")) {
                    accidentDescriptionEditText.setVisibility(View.VISIBLE);
                } else {
                    accidentDescriptionEditText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // העלאת תמונה
        uploadImageButton.setOnClickListener(v -> openFileChooser());

        // שמירת נתונים
        saveButton.setOnClickListener(v -> saveCarDetails());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            vehicleImageView.setVisibility(View.VISIBLE);
            vehicleImageView.setImageURI(imageUri);
            Toast.makeText(this, "תמונה נבחרה", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCarDetails() {
        String mileage = mileageEditText.getText().toString().trim();
        String maintenanceLevel = maintenanceLevelSpinner.getSelectedItem().toString();
        String accidentStatus = accidentSpinner.getSelectedItem().toString();
        String accidentDescription = accidentStatus.equals("כן") ? accidentDescriptionEditText.getText().toString().trim() : "";

        if (mileage.isEmpty()) {
            Toast.makeText(this, "אנא הכנס קילומטראז'", Toast.LENGTH_SHORT).show();
            return;
        }

        // הוספת המידע ל-Firestore
        CarDetails carDetails = new CarDetails(mileage, maintenanceLevel, accidentStatus, accidentDescription);

        // שמירת המידע ב-Firestore
        CollectionReference carsRef = firestore.collection("cars");
        carsRef.add(carDetails)
                .addOnSuccessListener(documentReference -> Toast.makeText(Registration2.this, "הנתונים נשמרו בהצלחה", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(Registration2.this, "שגיאה בשמירת הנתונים", Toast.LENGTH_SHORT).show());

        // אם יש תמונה להעלות
        if (imageUri != null) {
            uploadImageToFirestore(imageUri);
        }
    }

    private void uploadImageToFirestore(Uri imageUri) {
        try {
            // המרת התמונה ל-Bitmap
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // המרת ה-Bitmap ל-byte[]
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] imageData = byteArrayOutputStream.toByteArray();

            // יצירת אובייקט שיכיל את נתוני התמונה
            CollectionReference carImagesRef = firestore.collection("car_images");
            carImagesRef.add(new CarImage(imageData))
                    .addOnSuccessListener(documentReference -> Toast.makeText(Registration2.this, "תמונה נשמרה בהצלחה", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(Registration2.this, "שגיאה בהעלאת התמונה", Toast.LENGTH_SHORT).show());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה בעיבוד התמונה", Toast.LENGTH_SHORT).show();
        }
    }

    // מחלקה לייצוג נתוני הרכב
    public static class CarDetails {
        private String mileage;
        private String maintenanceLevel;
        private String accidentStatus;
        private String accidentDescription;

        public CarDetails(String mileage, String maintenanceLevel, String accidentStatus, String accidentDescription) {
            this.mileage = mileage;
            this.maintenanceLevel = maintenanceLevel;
            this.accidentStatus = accidentStatus;
            this.accidentDescription = accidentDescription;
        }

        public String getMileage() {
            return mileage;
        }

        public String getMaintenanceLevel() {
            return maintenanceLevel;
        }

        public String getAccidentStatus() {
            return accidentStatus;
        }

        public String getAccidentDescription() {
            return accidentDescription;
        }
    }

    // מחלקה לייצוג נתוני התמונה
    public static class CarImage {
        private byte[] imageData;

        public CarImage(byte[] imageData) {
            this.imageData = imageData;
        }

        public byte[] getImageData() {
            return imageData;
        }
    }
}
