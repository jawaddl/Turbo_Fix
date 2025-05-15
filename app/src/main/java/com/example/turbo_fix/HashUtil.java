package com.example.turbo_fix;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    // פונקציה ליצירת hash ממחרוזת
    public static String generateShortHash(String originalString) {
        try {
            // יצירת אובייקט של MessageDigest עם אלגוריתם SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(originalString.getBytes());

            // המרת ה-bite array ל-mString בקידוד hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            // חזרה על hash (חיתוך ה-string לקבלת מזהה קצר של 8 תווים)
            return hexString.toString().substring(0, 8); // מזהה באורך 8 תווים
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
