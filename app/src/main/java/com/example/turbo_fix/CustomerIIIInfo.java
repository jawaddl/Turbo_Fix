package com.example.turbo_fix;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class CustomerIIIInfo extends AppCompatActivity {

    private EditText searchEditText;
    private Button searchButton;
    private TextView resultTextView;
    private LinearLayout imageContainer;
    private Button backButton; // כפתור חזרה
    private static final String TAG = "AdminActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.customeriiiinfo);

        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        resultTextView = findViewById(R.id.result_text_view);
        imageContainer = findViewById(R.id.imageContainer);
        backButton = findViewById(R.id.back_button);

        // Set initial text
        resultTextView.setText("הזן מזהה לקוח לחיפוש");

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerIIIInfo.this, Select_admin.class);
            startActivity(intent);
            finish();
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        searchButton.setOnClickListener(v -> {
            String documentId = searchEditText.getText().toString().trim();
            if (documentId.isEmpty()) {
                Toast.makeText(this, "יש להקליד מזהה משתמש לחיפוש", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "מחפש מזהה: " + documentId);

            db.collection("USER").document(documentId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String name = document.getString("fullName");
                        String email = document.getString("email");
                        String phone = document.getString("phone");
                        String serviceLevel = document.getString("serviceLevel");
                        String mileage = document.getString("mileage");
                        String accidentStatus = document.getString("accidentStatus");
                        String accidentDetails = document.getString("accidentDetails");
                        String vehiclePlate = document.getString("vehicle");
                        List<String> imageUrls = (List<String>) document.get("imageUris");

                        String accidentInfo = (accidentStatus != null ? accidentStatus : "לא ידוע");
                        if ("כן".equals(accidentStatus) && accidentDetails != null && !accidentDetails.isEmpty()) {
                            accidentInfo += "\nפירוט: " + accidentDetails;
                        }

                        StringBuilder result = new StringBuilder();
                        result.append("שם מלא: ").append(name != null ? name : "לא ידוע").append("\n");
                        result.append("דוא״ל: ").append(email != null ? email : "לא ידוע").append("\n");
                        result.append("טלפון: ").append(phone != null ? phone : "לא ידוע").append("\n");
                        result.append("רמת טיפול: ").append(serviceLevel != null ? serviceLevel : "לא ידוע").append("\n");
                        result.append("קילומטראז׳: ").append(mileage != null ? mileage : "לא ידוע").append("\n");
                        result.append("תאונה: ").append(accidentInfo).append("\n");
                        result.append("מספר רכב: ").append(vehiclePlate != null ? vehiclePlate : "לא ידוע").append("\n");

                        resultTextView.setText(result.toString());
                        displayImages(imageUrls);

                        if (vehiclePlate != null && !vehiclePlate.isEmpty()) {
                            String apiUrl = "https://data.gov.il/api/3/action/datastore_search?resource_id=053cea08-09bc-40ec-8f7a-156f0677aff3&q=" + vehiclePlate;
                            new FetchVehicleDataTask().execute(apiUrl);
                        }

                    } else {
                        Toast.makeText(this, "לא נמצאו תוצאות", Toast.LENGTH_SHORT).show();
                        resultTextView.setText("לא נמצאו תוצאות");
                        Log.d(TAG, "לא נמצא מסמך עם מזהה: " + documentId);
                    }
                } else {
                    Toast.makeText(this, "שגיאה בחיפוש", Toast.LENGTH_SHORT).show();
                    resultTextView.setText("שגיאה בחיפוש");
                    Log.e(TAG, "שגיאה בשליפה: ", task.getException());
                }
            });
        });
    }

    private void displayImages(List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            imageContainer.removeAllViews();
            for (String imageUrl : imageUrls) {
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    dpToPx(100),
                    dpToPx(100));
                layoutParams.setMargins(0, 0, dpToPx(8), 0);
                imageView.setLayoutParams(layoutParams);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // Load thumbnail with error handling
                Glide.with(this)
                    .load(imageUrl)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(imageView);

                imageView.setOnClickListener(v -> {
                    Log.d("ImageLoading", "Attempting to show full screen image: " + imageUrl);
                    showFullScreenImage(imageUrl);
                });
                imageContainer.addView(imageView);
            }
        }
    }

    private void showFullScreenImage(String imageUrl) {
        // Create the dialog first
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullscreen_image, null);
        
        ImageView fullscreenImageView = dialogView.findViewById(R.id.fullscreen_image);
        ProgressBar loadingIndicator = dialogView.findViewById(R.id.loading_indicator);
        
        // Ensure loading indicator is visible initially
        loadingIndicator.setVisibility(View.VISIBLE);
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        // Load the image
        Glide.with(this)
            .load(imageUrl)
            .error(android.R.drawable.ic_dialog_alert)
            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, 
                    com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                    boolean isFirstResource) {
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(CustomerIIIInfo.this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                    Log.e("ImageLoading", "Failed to load image: " + imageUrl, e);
                    return false;
                }

                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                    com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                    com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                    loadingIndicator.setVisibility(View.GONE);
                    Log.d("ImageLoading", "Image loaded successfully: " + imageUrl);
                    return false;
                }
            })
            .into(fullscreenImageView);

        // Close on click
        dialogView.setOnClickListener(v -> dialog.dismiss());
        
        // Show dialog and set to full screen
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            dialog.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.MATCH_PARENT
            );
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private class FetchVehicleDataTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String apiUrl = params[0];
            StringBuilder response = new StringBuilder();

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

            } catch (Exception e) {
                Log.e(TAG, "Error fetching vehicle data: ", e);
                return "Error fetching vehicle data";
            }

            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);
                JSONArray records = json.getJSONObject("result").getJSONArray("records");

                if (records.length() > 0) {
                    JSONObject car = records.getJSONObject(0);

                    String tozeretNm = car.optString("tozeret_nm");
                    String degemNm = car.optString("degem_nm");
                    String ramatGimur = car.optString("ramat_gimur");
                    String moedAliyaLakvish = car.optString("moed_aliya_lakvish");
                    String degemManoa = car.optString("degem_manoa");
                    String tzevaRechev = car.optString("tzeva_rechev");
                    String kinuyMishari = car.optString("kinuy_mishari");

                    String carDetails = "\n\n⚙ פרטי רכב ממשרד התחבורה:\n"
                            + "יצרן: " + tozeretNm + "\n"
                            + "דגם: " + degemNm + "\n"
                            + "רמת גימור: " + ramatGimur + "\n"
                            + "עלייה לכביש: " + moedAliyaLakvish + "\n"
                            + "מנוע: " + degemManoa + "\n"
                            + "צבע: " + tzevaRechev + "\n"
                            + "כינוי מסחרי: " + kinuyMishari + "\n";

                    resultTextView.append(carDetails);
                } else {
                    resultTextView.append("\n⚠ לא נמצאו פרטים בלוחית הרישוי במשרד התחבורה.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error parsing vehicle data: ", e);
                resultTextView.append("\n⚠ שגיאה בשליפת נתוני רכב מה-API.");
            }
        }
    }
}
