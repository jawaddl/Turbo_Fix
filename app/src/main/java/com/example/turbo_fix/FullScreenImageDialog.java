package com.example.turbo_fix;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.bumptech.glide.Glide;

public class FullScreenImageDialog extends AppCompatDialogFragment {

    private String imageUrl;

    public FullScreenImageDialog(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // מגדיר את תוכן הדיאלוג
        dialog.setContentView(R.layout.dialog_layout);

        // הופך את הדיאלוג למסך מלא
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        // מציג את התמונה
        ImageView fullScreenImageView = dialog.findViewById(R.id.fullScreenImageView);
        Glide.with(getActivity())
                .load(imageUrl)
                .fitCenter()
                .into(fullScreenImageView);

        // כפתור סגירה
        ImageView closeButton = dialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        return dialog;
    }
}
