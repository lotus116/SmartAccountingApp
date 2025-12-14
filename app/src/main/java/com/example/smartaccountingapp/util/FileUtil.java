package com.example.smartaccountingapp.util;

import android.content.Context;
import android.util.Log;
import com.example.smartaccountingapp.model.Account;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    private static final String TAG = "FileUtil";
    private static final Gson gson = new Gson();

    // 将 List<Account> 转换为 JSON 字符串
    public static String convertAccountsToJson(List<Account> accounts) {
        return gson.toJson(accounts);
    }

    // 将 JSON 字符串转换为 List<Account>
    public static List<Account> convertJsonToAccounts(String json) {
        try {
            Type listType = new TypeToken<ArrayList<Account>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            // 【优化点：记录解析失败的日志】
            Log.e(TAG, "JSON解析失败，备份文件可能损坏或格式错误。", e);
            Log.e(TAG, "失败的JSON内容片段: " + (json != null && json.length() > 50 ? json.substring(0, 50) + "..." : ""));
            return new ArrayList<>();
        }
    }

    // 将数据保存到内部存储文件 (文件存储)
    public static boolean saveToFile(Context context, String fileName, String data) {
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(data.getBytes());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "保存文件失败", e);
            return false;
        }
    }

    // 从内部存储文件读取数据
    public static String readFromFile(Context context, String fileName) {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = context.openFileInput(fileName);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            // Log.e(TAG, "读取文件失败 (文件可能不存在)", e); // 文件不存在是正常情况，不报E级错误
            return null;
        }
    }
}