package com.example.turbo_fix;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppointmentDetailsFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userId;
    private String appointmentId;
    private String clientName;
    private Date appointmentDate;
    private String description;
    private Boolean knowsProblem;

    public static AppointmentDetailsFragment newInstance(String userId, String appointmentId, 
            String clientName, Date date, String description, Boolean knowsProblem) {
        AppointmentDetailsFragment fragment = new AppointmentDetailsFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putString("appointmentId", appointmentId);
        args.putString("clientName", clientName);
        args.putLong("date", date.getTime());
        args.putString("description", description);
        args.putBoolean("knowsProblem", knowsProblem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        
        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("userId");
            appointmentId = args.getString("appointmentId");
            clientName = args.getString("clientName");
            appointmentDate = new Date(args.getLong("date"));
            description = args.getString("description");
            knowsProblem = args.getBoolean("knowsProblem");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_appointment_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView clientNameText = view.findViewById(R.id.clientNameText);
        TextView clientIdText = view.findViewById(R.id.clientIdText);
        TextView dateText = view.findViewById(R.id.dateText);
        TextView timeText = view.findViewById(R.id.timeText);
        TextView problemTypeText = view.findViewById(R.id.problemTypeText);
        TextView descriptionText = view.findViewById(R.id.descriptionText);
        LinearLayout imagesContainer = view.findViewById(R.id.imagesContainer);
        ProgressBar loadingProgress = view.findViewById(R.id.loadingProgress);
        ImageButton copyButton = view.findViewById(R.id.copyButton);

        // Format date and time separately
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedDate = dateFormat.format(appointmentDate);
        String formattedTime = timeFormat.format(appointmentDate);

        clientNameText.setText("שם לקוח: " + clientName);
        clientIdText.setText("מזהה לקוח: " + userId);
        dateText.setText("תאריך: " + formattedDate);
        timeText.setText("שעה: " + formattedTime);
        problemTypeText.setText("סוג פנייה: " + (knowsProblem ? "תקלה ידועה" : "סימפטומים"));
        descriptionText.setText("תיאור: " + description);

        // Setup copy button
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Client ID", userId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "מזהה לקוח הועתק", Toast.LENGTH_SHORT).show();
        });

        // Load images if they exist
        loadingProgress.setVisibility(View.VISIBLE);
        db.collection("USER")
            .document(userId)
            .collection("appointments")
            .document(appointmentId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                List<String> imageUrls = (List<String>) documentSnapshot.get("images");
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    for (String imageUrl : imageUrls) {
                        StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
                        ImageView imageView = new ImageView(requireContext());
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            getResources().getDimensionPixelSize(R.dimen.appointment_image_size),
                            getResources().getDimensionPixelSize(R.dimen.appointment_image_size)
                        );
                        params.setMargins(8, 8, 8, 8);
                        imageView.setLayoutParams(params);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imagesContainer.addView(imageView);

                        // Load image using Glide
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Glide.with(this)
                                .load(uri)
                                .centerCrop()
                                .placeholder(R.drawable.loading_placeholder)
                                .into(imageView);
                        });
                    }
                }
                loadingProgress.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> {
                loadingProgress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "שגיאה בטעינת תמונות: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
} 