package com.example.turbo_fix;

public class UserData {
    private String email;
    private String password;

    // קונסטרוקטור ברירת מחדל
    public UserData() {}

    // קונסטרוקטור עם שני פרמטרים
    public UserData(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // גטרים וסטרים לשדות
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}