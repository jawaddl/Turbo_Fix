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
        backButton = findViewById(R.id.back_button); // חיבור לכפתור חזרה

        // חיבור לאקטיביטי הקודם (Admin_Activity) כאשר לוחצים על כפתור חזרה
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerIIIInfo.this, Select_admin.class);
            startActivity(intent);
            finish(); // סוגר את האקטיביטי הנוכחי
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

                        String result = "שם: " + name + "\n"
                                + "אימייל: " + email + "\n"
                                + "טלפון: " + phone + "\n"
                                + "רמת טיפול ברכב: " + serviceLevel + "\n"
                                + "קילומטראז': " + mileage + "\n"
                                + "תאונה: " + accidentInfo + "\n"
                                + "לוחית רישוי: " + vehiclePlate + "\n";

                        resultTextView.setText(result);

                        displayImages(imageUrls);

                        // Perform the API call to get vehicle data
                        if (vehiclePlate != null && !vehiclePlate.isEmpty()) {
                            String apiUrl = "https://data.gov.il/api/3/action/datastore_search?resource_id=053cea08-09bc-40ec-8f7a-156f0677aff3&q=" + vehiclePlate;

                            new FetchVehicleDataTask().execute(apiUrl);
                        }

                    } else {
                        Toast.makeText(this, "לא נמצאו תוצאות", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "לא נמצא מסמך עם מזהה: " + documentId);
                    }
                } else {
                    Toast.makeText(this, "שגיאה בחיפוש", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "שגיאה בשליפה: ", task.getException());
                }
            });
        });

        resultTextView.setText("Enter an ID address");
    }

    private void displayImages(List<String> imageUrls) {
        imageContainer.removeAllViews();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            int maxImages = Math.min(imageUrls.size(), 4);
            for (int i = 0; i < maxImages; i++) {
                final int index = i;
                ImageView imageView = new ImageView(this);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(
                        590,
                        640));
                imageView.setPadding(4, 4, 4, 4);

                Glide.with(this)
                        .load(imageUrls.get(i))
                        .into(imageView);

                imageView.setOnClickListener(v -> showFullScreenImage(imageUrls.get(index)));
                imageContainer.addView(imageView);
            }
        } else {
            Toast.makeText(this, "אין תמונות זמינות", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFullScreenImage(String imageUrl) {
        FullScreenImageDialog dialog = new FullScreenImageDialog(imageUrl);
        dialog.show(getSupportFragmentManager(), "FullScreenImageDialog");
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
