package com.example.smartaccountingapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private static final String PREF_NAME = "SmartAccountingPrefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 保存当前登录用户的ID，并标记为已登录。
     * @param context Context
     * @param userId 用户的ID (通常是用户名)
     */
    public static void setCurrentUserId(Context context, String userId) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_CURRENT_USER_ID, userId);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * 获取当前登录用户的ID。
     * @param context Context
     * @return 当前用户ID，如果未登录则返回 null。
     */
    public static String getCurrentUserId(Context context) {
        return getPrefs(context).getString(KEY_CURRENT_USER_ID, null);
    }

    /**
     * 检查用户是否已登录。
     * @param context Context
     * @return 是否已登录。
     */
    public static boolean isLoggedIn(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * 退出登录，清空当前用户ID和登录状态。
     * @param context Context
     */
    public static void logout(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.remove(KEY_CURRENT_USER_ID);
        editor.remove(KEY_IS_LOGGED_IN);
        editor.apply();
    }
}