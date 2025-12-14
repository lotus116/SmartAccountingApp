package com.example.smartaccountingapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD_PREFIX = "password_";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public PrefsManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean(KEY_LOGGED_IN, isLoggedIn);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_LOGGED_IN, false);
    }

    public void saveUser(String username, String password) {
        editor.putString(KEY_USERNAME, username); // 保存当前用户名 (可用于显示)
        editor.putString(KEY_PASSWORD_PREFIX + username, password); // 密码以用户名作为key的一部分存储
        editor.apply();
    }

    public String getSavedPassword(String username) {
        return pref.getString(KEY_PASSWORD_PREFIX + username, null);
    }

    public void logout() {
        setLoggedIn(false);
        // 清除当前用户名等信息（可选）
    }
}