package com.example.turbo_fix;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
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
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Client_Activity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextView clientIdTextView, clientNameTextView, carTypeTextView, kilometersTextView, carModelTextView;
    private TextView noAppointmentTextView;
    private TextView locationInfoTextView;
    private Button button_tor;
    private SearchView searchView;
    private FirebaseFirestore db;
    private String clientId;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private Polyline currentRoute;
    private Button navigateButton;
    private LatLng startLocation; // Current location or searched location
   
    // המיקום המדויק של המוסך בטירה
    private static final LatLng GARAGE_LOCATION = new LatLng(32.234986337467134, 34.96518895452847);
    private static final String GARAGE_ADDRESS = "דרך אל סולטאני, טירה";

    // Add tempLocation as a class variable
    private LatLng tempLocation; // Store temporary searched location
    private LatLng tempSearchLocation; // Store the searched location temporarily

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        getWindow().setStatusBarColor(Color.parseColor("#8D6E63"));

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Initialize views
        clientIdTextView = findViewById(R.id.clientIdTextView);
        clientNameTextView = findViewById(R.id.clientNameTextView);
        carModelTextView = findViewById(R.id.carModelTextView);
        kilometersTextView = findViewById(R.id.kilometersTextView);
        carTypeTextView = findViewById(R.id.carTypeTextView);
        noAppointmentTextView = findViewById(R.id.noAppointmentTextView);
        locationInfoTextView = findViewById(R.id.locationInfoTextView);
        button_tor = findViewById(R.id.button_tor);
        ImageButton wrenchButton = findViewById(R.id.wrenchButton);
        ImageButton refreshButton = findViewById(R.id.refreshButton);
        searchView = findViewById(R.id.searchView);
        navigateButton = findViewById(R.id.navigateButton);

        // Set up search functionality
        setupSearchView();

        // Set up navigation button
        setupNavigateButton();

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

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

        // Request location permissions
        requestLocationPermission();
    }

    private void setupSearchView() {
        searchView.setQueryHint("חפש כתובת מוצא...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Clear search and revert to current location if search is cleared
                if (newText.isEmpty()) {
                    if (currentLocation != null) {
                        LatLng userLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        startLocation = userLocation;
                        
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions()
                                .position(userLocation)
                                .title("המיקום שלי"));
                        addGarageMarker();
                        drawRoute(userLocation, GARAGE_LOCATION);
                        updateLocationInfo(userLocation, "המיקום הנוכחי שלי");
                    }
                }
                return false;
            }
        });
    }

    private void setupNavigateButton() {
        navigateButton.setOnClickListener(v -> {
            // Get the current search query
            String searchQuery = searchView.getQuery().toString();
            
            // If there's a search query, geocode it and use that location
            if (!searchQuery.isEmpty()) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocationName(searchQuery, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng searchedLocation = new LatLng(address.getLatitude(), address.getLongitude());
                        
                        // Create a URI for directions from searched location to garage
                        String uri = String.format(Locale.US, 
                            "https://www.google.com/maps/dir/%f,%f/%f,%f",
                            searchedLocation.latitude,
                            searchedLocation.longitude,
                            GARAGE_LOCATION.latitude,
                            GARAGE_LOCATION.longitude);
                            
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        mapIntent.setPackage("com.google.android.apps.maps");
                        
                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        } else {
                            Toast.makeText(this, "אנא התקן את Google Maps", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "לא נמצאה כתובת", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "שגיאה בחיפוש הכתובת", Toast.LENGTH_SHORT).show();
                }
            } else {
                // No search query - use current location
                if (currentLocation != null) {
                    String uri = String.format(Locale.US, 
                        "https://www.google.com/maps/dir/%f,%f/%f,%f",
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude(),
                        GARAGE_LOCATION.latitude,
                        GARAGE_LOCATION.longitude);
                        
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    mapIntent.setPackage("com.google.android.apps.maps");
                    
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(this, "אנא התקן את Google Maps", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "לא זמין מיקום נוכחי", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void searchLocation(String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng searchedLocation = new LatLng(address.getLatitude(), address.getLongitude());
                
                // Clear previous markers and route
                mMap.clear();
                
                // Add marker for searched location
                mMap.addMarker(new MarkerOptions()
                        .position(searchedLocation)
                        .title("נקודת התחלה: " + query));
                
                // Add garage marker
                addGarageMarker();
                
                // Draw route from searched location to garage
                drawRoute(searchedLocation, GARAGE_LOCATION);
                
                // Update location info with searched location
                updateLocationInfo(searchedLocation, query);
                
                // Store this as the current start location
                startLocation = searchedLocation;
            }
        } catch (IOException e) {
            Toast.makeText(this, "לא נמצאה כתובת", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    
                    // Only update the map if there's no active search
                    if (searchView.getQuery().toString().isEmpty()) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        startLocation = userLocation;
                        
                        if (mMap != null) {
                            // Clear previous markers and route
                            mMap.clear();
                            
                            // Add marker for user's location
                            mMap.addMarker(new MarkerOptions()
                                    .position(userLocation)
                                    .title("המיקום שלי"));
                            
                            // Add garage marker
                            addGarageMarker();
                            
                            // Draw route to garage
                            drawRoute(userLocation, GARAGE_LOCATION);
                            
                            // Update location info
                            updateLocationInfo(userLocation, "המיקום הנוכחי שלי");
                        }
                    }
                }
            });
        }
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        // Remove previous route if exists
        if (currentRoute != null) {
            currentRoute.remove();
        }

        // Draw new route
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(origin)
                .add(destination)
                .width(5)
                .color(Color.BLUE);
        
        currentRoute = mMap.addPolyline(polylineOptions);
        
        // Adjust camera to show entire route
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(origin);
        builder.include(destination);
        LatLngBounds bounds = builder.build();
        
        // Add padding to the bounds
        int padding = 100;
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    private void updateLocationInfo(LatLng location, String addressName) {
        String locationInfo = String.format(Locale.getDefault(),
                "נקודת התחלה: %s\n" +
                "קו רוחב: %.6f\n" +
                "קו אורך: %.6f\n" +
                "\nיעד - המוסך שלנו:\n" +
                "קו רוחב: %.6f\n" +
                "קו אורך: %.6f",
                addressName,
                location.latitude,
                location.longitude,
                GARAGE_LOCATION.latitude,
                GARAGE_LOCATION.longitude
        );
        locationInfoTextView.setText(locationInfo);
    }

    private void addGarageMarker() {
        mMap.addMarker(new MarkerOptions()
                .position(GARAGE_LOCATION)
                .title("המוסך שלנו")
                .snippet(GARAGE_ADDRESS));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Enable my location button
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        
        // Set up map UI
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        
        // Add garage marker
        addGarageMarker();
        
        // Get current location
        getCurrentLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (clientId != null && !clientId.isEmpty()) {
            checkAppointment(clientId);
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
