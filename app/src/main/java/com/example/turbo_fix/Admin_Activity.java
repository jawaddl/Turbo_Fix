package com.example.turbo_fix;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Admin_Activity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyStateLayout;
    private ChipGroup filterChipGroup;
    private FirebaseFirestore db;
    private AppointmentsAdapter adapter;
    private List<AppointmentItem> appointments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setStatusBarColor(Color.parseColor("#8D6E63"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        recyclerView = findViewById(R.id.appointmentsRecyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        filterChipGroup = findViewById(R.id.filterChipGroup);

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

    private void loadAppointments() {
        swipeRefresh.setRefreshing(true);
        appointments.clear();

        // Get selected filter
        int checkedChipId = filterChipGroup.getCheckedChipId();
        
        // Calculate date range
        final Date startDate = new Date();
        final Date endDate;
        
        if (checkedChipId == R.id.chipToday) {
            endDate = getEndOfDay(startDate);
        } else if (checkedChipId == R.id.chipTomorrow) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            startDate.setTime(cal.getTimeInMillis());
            endDate = getEndOfDay(startDate);
        } else if (checkedChipId == R.id.chipWeek) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 7);
            endDate = cal.getTime();
        } else {
            endDate = null;
        }

        // Query all users
        db.collection("USER")
            .get()
            .addOnSuccessListener(userSnapshots -> {
                int totalUsers = userSnapshots.size();
                int[] processedUsers = {0};

                for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
                    Query appointmentsQuery = userDoc.getReference().collection("appointments");
                    
                    if (endDate != null) {
                        appointmentsQuery = appointmentsQuery
                            .whereGreaterThanOrEqualTo("date", startDate)
                            .whereLessThanOrEqualTo("date", endDate);
                    }

                    appointmentsQuery.get().addOnSuccessListener(appointmentSnapshots -> {
                        for (DocumentSnapshot appointmentDoc : appointmentSnapshots.getDocuments()) {
                            AppointmentItem item = new AppointmentItem();
                            item.userId = userDoc.getId();
                            item.appointmentId = appointmentDoc.getId();
                            item.clientName = userDoc.getString("fullName");
                            item.date = appointmentDoc.getDate("date");
                            item.description = appointmentDoc.getString("description");
                            item.knowsProblem = appointmentDoc.getBoolean("knowsProblem");
                            appointments.add(item);
                        }

                        processedUsers[0]++;
                        if (processedUsers[0] == totalUsers) {
                            // All users processed
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("פרטי תור");

        // Create the detail message
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String details = String.format(
            "שם לקוח: %s\n" +
            "מזהה לקוח: %s\n" +
            "תאריך: %s\n" +
            "סוג פנייה: %s\n" +
            "תיאור: %s",
            appointment.clientName,
            appointment.userId,
            sdf.format(appointment.date),
            appointment.knowsProblem ? "תקלה ידועה" : "סימפטומים",
            appointment.description
        );

        builder.setMessage(details);
        builder.setPositiveButton("סגור", null);
        builder.show();
    }

    private void deleteAppointment(AppointmentItem appointment) {
        new AlertDialog.Builder(this)
            .setTitle("מחיקת תור")
            .setMessage("האם אתה בטוח שברצונך למחוק תור זה?")
            .setPositiveButton("כן", (dialog, which) -> {
                // Delete from appointments_global first
                SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault());
                String dateKey = dateKeyFormat.format(appointment.date);
                
                db.collection("appointments_global")
                    .document(dateKey)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Then delete from user's appointments
                        db.collection("USER")
                            .document(appointment.userId)
                            .collection("appointments")
                            .document(appointment.appointmentId)
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(Admin_Activity.this, "התור נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                            })
                            .addOnFailureListener(e -> 
                                Toast.makeText(Admin_Activity.this, "שגיאה במחיקת התור: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                    });
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
