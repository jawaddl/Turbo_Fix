package com.example.turbo_fix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppointmentDetailsFragment extends Fragment {
    private TextView appointmentStatusTextView;
    private Date appointmentDate;
    private String appointmentStatus;

    public static AppointmentDetailsFragment newInstance(Date date, String status) {
        AppointmentDetailsFragment fragment = new AppointmentDetailsFragment();
        Bundle args = new Bundle();
        if (date != null) {
            args.putLong("date", date.getTime());
        }
        args.putString("status", status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            long dateMillis = args.getLong("date", -1);
            if (dateMillis != -1) {
                appointmentDate = new Date(dateMillis);
            }
            appointmentStatus = args.getString("status", "לא קיים זימון תור");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointment_details, container, false);
        appointmentStatusTextView = view.findViewById(R.id.appointmentStatusTextView);
        updateAppointmentDisplay();
        return view;
    }

    private void updateAppointmentDisplay() {
        if (appointmentDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String formattedDate = dateFormat.format(appointmentDate);
            String formattedTime = timeFormat.format(appointmentDate);
            appointmentStatusTextView.setText("תור נקבע לתאריך - " + formattedDate + "\n" + "בשעה: " + formattedTime);
            appointmentStatusTextView.setTextColor(Color.parseColor("#000000"));
        } else {
            appointmentStatusTextView.setText(appointmentStatus);
            appointmentStatusTextView.setTextColor(Color.BLACK);
        }
    }

    public void updateAppointmentInfo(Date date, String status) {
        this.appointmentDate = date;
        this.appointmentStatus = status;
        if (appointmentStatusTextView != null) {
            updateAppointmentDisplay();
        }
    }
} 