package com.example.turbo_fix;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.app.Dialog;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.CheckBox;
import android.widget.ArrayAdapter;
import com.google.android.material.button.MaterialButton;
import android.content.SharedPreferences;
import com.google.firebase.firestore.SetOptions;
import java.util.Collections;
import java.util.TimeZone;
import android.widget.ListView;
import java.util.HashSet;
import java.util.Set;

public class Admin_Activity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyStateLayout;
    private ChipGroup filterChipGroup;
    private FirebaseFirestore db;
    private AppointmentsAdapter adapter;
    private List<AppointmentItem> appointments;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setStatusBarColor(Color.parseColor("#8D6E63"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Get admin credentials
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String password = prefs.getString("password", "");

        // Verify admin credentials
        if (!username.equals("a") || !password.equals("b")) {
            Toast.makeText(this, "נא להתחבר כמנהל", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Create admin settings document if it doesn't exist
        Map<String, Object> adminSettings = new HashMap<>();
        adminSettings.put("isAdmin", true);
        adminSettings.put("lastUpdated", new Date());

        db.collection("USER")
            .document("admin_settings")
            .set(adminSettings, SetOptions.merge())
            .addOnFailureListener(e -> {
                Toast.makeText(this, "שגיאה בהגדרות מנהל: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        // Initialize views
        recyclerView = findViewById(R.id.appointmentsRecyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        // Setup block hours button
        MaterialButton blockHoursButton = findViewById(R.id.blockHoursButton);
        blockHoursButton.setOnClickListener(v -> showBlockHoursDialog());

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(Admin_Activity.this, Select_admin.class);
            startActivity(intent);
            finish();
        });

        // Setup RecyclerView
        appointments = new ArrayList<>();
        adapter = new AppointmentsAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadAppointments);

        // Setup filter chips
        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                loadAppointments();
            }
        });

        // Initial load
        loadAppointments();
    }

    private void showBlockHoursDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_block_hours, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("חסום שעות");

        CalendarView calendarView = dialogView.findViewById(R.id.calendarView);
        ListView hourListView = dialogView.findViewById(R.id.hourListView);
        CheckBox allDayCheckBox = dialogView.findViewById(R.id.allDayCheckBox);

        // Set min date to today
        calendarView.setMinDate(System.currentTimeMillis());
        
        // Set max date to 2 weeks from now
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DAY_OF_YEAR, 14);
        calendarView.setMaxDate(maxDate.getTimeInMillis());

        // Setup hour list with checkboxes
        List<String> hours = Arrays.asList("08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
                "11:00", "11:30", "12:00", "12:30", "13:00", "13:30", "14:00");
        Set<String> selectedHours = new HashSet<>();
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_multiple_choice, hours);
        hourListView.setAdapter(adapter);
        hourListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Handle all day checkbox
        allDayCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hourListView.setEnabled(!isChecked);
            if (isChecked) {
                selectedHours.clear();
                for (int i = 0; i < hourListView.getCount(); i++) {
                    hourListView.setItemChecked(i, false);
                }
            }
        });

        // Handle hour selection
        hourListView.setOnItemClickListener((parent, view, position, id) -> {
            String hour = hours.get(position);
            if (hourListView.isItemChecked(position)) {
                selectedHours.add(hour);
            } else {
                selectedHours.remove(hour);
            }
        });

        final Calendar selectedDate = Calendar.getInstance();
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
        });

        builder.setView(dialogView);
        builder.setPositiveButton("חסום", (dialog, which) -> {
            if (allDayCheckBox.isChecked()) {
                blockEntireDay(selectedDate.getTime());
                Toast.makeText(this, "כל היום נחסם בהצלחה", Toast.LENGTH_SHORT).show();
            } else if (!selectedHours.isEmpty()) {
                // Block all selected hours at once
                List<String> hoursToBlock = new ArrayList<>(selectedHours);
                Collections.sort(hoursToBlock);
                
                // Create a batch operation
                for (String hour : hoursToBlock) {
                    blockSpecificHour(selectedDate.getTime(), hour);
                }
                
                // Show single success message
                Toast.makeText(this, "השעות שנבחרו נחסמו בהצלחה", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "נא לבחור לפחות שעה אחת", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void blockEntireDay(Date date) {
        if (!isAdmin()) {
            Toast.makeText(this, "נא להתחבר כמנהל", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if trying to block past day
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTime(date);
        selectedCal.set(Calendar.HOUR_OF_DAY, 0);
        selectedCal.set(Calendar.MINUTE, 0);
        selectedCal.set(Calendar.SECOND, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        if (selectedCal.before(now)) {
            Toast.makeText(this, "לא ניתן לחסום ימים שכבר עברו", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = dateFormat.format(date);

        Map<String, Object> blockData = new HashMap<>();
        blockData.put("blocked", true);
        blockData.put("blockType", "full_day");
        blockData.put("timestamp", new com.google.firebase.Timestamp(new Date()));

        // Store blocked hours under admin's document
        db.collection("USER")
            .document("admin_settings")
            .collection("blocked_hours")
            .document(dateStr)
            .set(blockData)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "היום נחסם בהצלחה", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "שגיאה בחסימת היום: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void blockSpecificHour(Date date, String hour) {
        if (!isAdmin()) {
            Toast.makeText(this, "נא להתחבר כמנהל", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = dateFormat.format(date);
        String timeSlotId = dateStr + "_" + hour;

        Map<String, Object> blockData = new HashMap<>();
        blockData.put("blocked", true);
        blockData.put("blockType", "specific_hour");
        blockData.put("hour", hour);
        blockData.put("timestamp", new com.google.firebase.Timestamp(new Date()));

        // Store blocked hours under admin's document
        db.collection("USER")
            .document("admin_settings")
            .collection("blocked_hours")
            .document(timeSlotId)
            .set(blockData);
    }

    private boolean isAdmin() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String password = prefs.getString("password", "");
        return username.equals("a") && password.equals("b");
    }

    private void deleteExpiredAppointments() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.add(Calendar.DAY_OF_YEAR, -1); // Delete appointments older than yesterday
        Date cutoffDate = cal.getTime();

        // Clean up user appointments
        db.collection("USER")
            .get()
            .addOnSuccessListener(userSnapshots -> {
                for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                    if (userDoc.getId().equals("admin_settings")) continue;

                    userDoc.getReference()
                        .collection("appointments")
                        .whereLessThan("date", new com.google.firebase.Timestamp(cutoffDate))
                        .get()
                        .addOnSuccessListener(appointmentSnapshots -> {
                            for (DocumentSnapshot appointmentDoc : appointmentSnapshots.getDocuments()) {
                                appointmentDoc.getReference().delete();
                            }
                        });
                }
            });
    }

    private void loadAppointments() {
        // Delete expired appointments first
        deleteExpiredAppointments();
        
        swipeRefresh.setRefreshing(true);
        appointments.clear();

        // Get selected filter
        int checkedChipId = filterChipGroup.getCheckedChipId();
        
        // Calculate date range
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        final com.google.firebase.Timestamp startTimestamp;
        final com.google.firebase.Timestamp endTimestamp;
        
        if (checkedChipId == R.id.chipToday) {
            startTimestamp = new com.google.firebase.Timestamp(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
            endTimestamp = new com.google.firebase.Timestamp(cal.getTime());
        } else if (checkedChipId == R.id.chipTomorrow) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            startTimestamp = new com.google.firebase.Timestamp(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
            endTimestamp = new com.google.firebase.Timestamp(cal.getTime());
        } else {
            startTimestamp = null;
            endTimestamp = null;
        }

        // Query all users
        db.collection("USER")
            .get()
            .addOnSuccessListener(userSnapshots -> {
                int totalUsers = userSnapshots.size();
                int[] processedUsers = {0};

                for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                    if (userDoc.getId().equals("admin_settings")) continue;
                    
                    Query appointmentsQuery = userDoc.getReference().collection("appointments");
                    
                    if (startTimestamp != null && endTimestamp != null) {
                        appointmentsQuery = appointmentsQuery
                            .whereGreaterThanOrEqualTo("date", startTimestamp)
                            .whereLessThan("date", endTimestamp);
                    }

                    appointmentsQuery.get().addOnSuccessListener(appointmentSnapshots -> {
                        for (DocumentSnapshot appointmentDoc : appointmentSnapshots.getDocuments()) {
                            AppointmentItem item = new AppointmentItem();
                            item.userId = userDoc.getId();
                            item.appointmentId = appointmentDoc.getId();
                            item.clientName = userDoc.getString("fullName");
                            
                            // Get the timestamp and add 3 hours for admin view
                            com.google.firebase.Timestamp timestamp = appointmentDoc.getTimestamp("date");
                            if (timestamp != null) {
                                Calendar adjustedCal = Calendar.getInstance();
                                adjustedCal.setTime(timestamp.toDate());
                                adjustedCal.add(Calendar.HOUR_OF_DAY, 3); // Add 3 hours for admin view
                                item.date = adjustedCal.getTime();
                            }
                            
                            item.description = appointmentDoc.getString("description");
                            item.knowsProblem = appointmentDoc.getBoolean("knowsProblem");
                            
                            if (item.date != null) {
                                appointments.add(item);
                            }
                        }

                        processedUsers[0]++;
                        if (processedUsers[0] == totalUsers - 1) {
                            // Sort appointments by date
                            Collections.sort(appointments, (a1, a2) -> a1.date.compareTo(a2.date));
                            updateUI();
                        }
                    });
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "שגיאה בטעינת התורים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            });
    }

    private Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private void updateUI() {
        swipeRefresh.setRefreshing(false);
        if (appointments.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showAppointmentDetails(AppointmentItem appointment) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_appointment_details, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        TextView clientNameText = dialogView.findViewById(R.id.clientNameText);
        TextView clientIdText = dialogView.findViewById(R.id.clientIdText);
        TextView dateText = dialogView.findViewById(R.id.dateText);
        TextView timeText = dialogView.findViewById(R.id.timeText);
        TextView problemTypeText = dialogView.findViewById(R.id.problemTypeText);
        TextView descriptionText = dialogView.findViewById(R.id.descriptionText);
        LinearLayout imagesContainer = dialogView.findViewById(R.id.imagesContainer);
        ProgressBar loadingProgress = dialogView.findViewById(R.id.loadingProgress);
        ImageButton copyButton = dialogView.findViewById(R.id.copyButton);

        // Format date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        String formattedDate = dateFormat.format(appointment.date);
        String formattedTime = timeFormat.format(appointment.date);

        clientNameText.setText("שם לקוח: " + appointment.clientName);
        clientIdText.setText("מזהה לקוח: " + appointment.userId);
        dateText.setText("תאריך: " + formattedDate);
        timeText.setText("שעה: " + formattedTime);
        problemTypeText.setText("סוג פנייה: " + (appointment.knowsProblem ? "תקלה ידועה" : "סימפטומים"));
        descriptionText.setText("תיאור: " + appointment.description);

        // Load images if they exist
        loadingProgress.setVisibility(View.VISIBLE);
        db.collection("USER")
            .document(appointment.userId)
            .collection("appointments")
            .document(appointment.appointmentId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                List<String> imageUrls = (List<String>) documentSnapshot.get("imageUrls");
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    // Create a horizontal linear layout for the row
                    LinearLayout rowLayout = new LinearLayout(this);
                    rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                    rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                    rowLayout.setGravity(android.view.Gravity.CENTER);
                    imagesContainer.addView(rowLayout);

                    // Calculate image size (3 images per row with spacing)
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    int spacing = 8; // dp to px conversion needed
                    float density = getResources().getDisplayMetrics().density;
                    int spacingPx = (int) (spacing * density);
                    int totalSpacing = spacingPx * 4; // 2 outer margins + 2 inner spaces
                    int imageSize = (screenWidth - totalSpacing) / 3;
                    // Reduce size by 2.5 times
                    imageSize = (int) (imageSize / 2.5);

                    for (String imageUrl : imageUrls) {
                        ImageView imageView = new ImageView(this);
                        // Set layout params for square shape
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            imageSize,
                            imageSize
                        );
                        params.setMargins(spacingPx, spacingPx, 0, spacingPx);
                        imageView.setLayoutParams(params);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imageView.setBackgroundColor(Color.parseColor("#EEEEEE"));
                        rowLayout.addView(imageView);

                        // Load image using Glide with square sizing
                        Glide.with(this)
                            .load(imageUrl)
                            .override(imageSize, imageSize)
                            .centerCrop()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_dialog_alert)
                            .into(imageView);

                        // Make images clickable to view full size
                        imageView.setOnClickListener(v -> {
                            Dialog fullscreenDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                            ImageView fullImageView = new ImageView(this);
                            fullImageView.setLayoutParams(new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
                            fullImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            fullscreenDialog.setContentView(fullImageView);

                            // Load full size image
                            Glide.with(this)
                                .load(imageUrl)
                                .fitCenter()
                                .into(fullImageView);

                            fullImageView.setOnClickListener(view -> fullscreenDialog.dismiss());
                            fullscreenDialog.show();
                        });
                    }
                }
                loadingProgress.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> {
                loadingProgress.setVisibility(View.GONE);
                Toast.makeText(this, "שגיאה בטעינת תמונות: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        builder.setPositiveButton("סגור", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteAppointment(AppointmentItem appointment) {
        new AlertDialog.Builder(this)
            .setTitle("מחיקת תור")
            .setMessage("האם אתה בטוח שברצונך למחוק תור זה?")
            .setPositiveButton("כן", (dialog, which) -> {
                // Delete from user's appointments
                db.collection("USER")
                    .document(appointment.userId)
                    .collection("appointments")
                    .document(appointment.appointmentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Admin_Activity.this, "התור נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                        loadAppointments();
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(Admin_Activity.this, "שגיאה במחיקת התור: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
            })
            .setNegativeButton("לא", null)
            .show();
    }

    private class AppointmentItem {
        String userId;
        String appointmentId;
        String clientName;
        Date date;
        String description;
        Boolean knowsProblem;
        long timezoneOffset;
    }

    private class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppointmentItem appointment = appointments.get(position);
            
            holder.clientNameText.setText(appointment.clientName);
            holder.clientIdText.setText("מזהה לקוח: " + appointment.userId);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.appointmentDateText.setText(sdf.format(appointment.date));
            
            holder.problemDescriptionText.setText(appointment.description);
            
            holder.deleteButton.setOnClickListener(v -> deleteAppointment(appointment));
            holder.viewDetailsButton.setOnClickListener(v -> showAppointmentDetails(appointment));
        }

        @Override
        public int getItemCount() {
            return appointments.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView clientNameText;
            TextView clientIdText;
            TextView appointmentDateText;
            TextView problemDescriptionText;
            ImageButton deleteButton;
            View viewDetailsButton;

            ViewHolder(View itemView) {
                super(itemView);
                clientNameText = itemView.findViewById(R.id.clientNameText);
                clientIdText = itemView.findViewById(R.id.clientIdText);
                appointmentDateText = itemView.findViewById(R.id.appointmentDateText);
                problemDescriptionText = itemView.findViewById(R.id.problemDescriptionText);
                deleteButton = itemView.findViewById(R.id.deleteButton);
                viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
            }
        }
    }
}
