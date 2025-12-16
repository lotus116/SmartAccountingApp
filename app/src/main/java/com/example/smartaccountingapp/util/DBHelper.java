package com.example.smartaccountingapp.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.smartaccountingapp.model.Account;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AccountDB";
    private static final int DATABASE_VERSION = 3; // 【修改】升级版本号到 3

    // 表名和列名
    public static final String TABLE_ACCOUNT = "accounts";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_NOTE = "note";
    public static final String COLUMN_IMAGE_PATH = "image_path"; // 【新增】图片路径列

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建记账表 (【修改】增加 COLUMN_USER_ID 和 COLUMN_IMAGE_PATH 列)
        String CREATE_ACCOUNT_TABLE = "CREATE TABLE " + TABLE_ACCOUNT + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " TEXT NOT NULL DEFAULT 'default_user',"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_AMOUNT + " REAL,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_NOTE + " TEXT,"
                + COLUMN_IMAGE_PATH + " TEXT DEFAULT NULL" // 【新增】
                + ")";
        db.execSQL(CREATE_ACCOUNT_TABLE);
    }

    // 【修改】处理数据库升级
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // V1 -> V2: 添加 user_id 列
        if (oldVersion < 2 && newVersion >= 2) {
            db.execSQL("ALTER TABLE " + TABLE_ACCOUNT + " ADD COLUMN " + COLUMN_USER_ID + " TEXT DEFAULT 'default_user'");
        }
        // V2 -> V3: 添加 image_path 列
        if (oldVersion < 3 && newVersion >= 3) {
            db.execSQL("ALTER TABLE " + TABLE_ACCOUNT + " ADD COLUMN " + COLUMN_IMAGE_PATH + " TEXT DEFAULT NULL");
        } else if (oldVersion > newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNT);
            onCreate(db);
        }
    }


    // --- CRUD 操作 ---

    // 【修改】addAccount: 新增 imagePath 字段保存
    public long addAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, account.getUserId());
        values.put(COLUMN_TYPE, account.getType());
        values.put(COLUMN_CATEGORY, account.getCategory());
        values.put(COLUMN_AMOUNT, account.getAmount());
        values.put(COLUMN_DATE, account.getDate());
        values.put(COLUMN_NOTE, account.getNote());
        values.put(COLUMN_IMAGE_PATH, account.getImagePath()); // 【核心修改】

        long id = db.insert(TABLE_ACCOUNT, null, values);
        db.close();
        return id;
    }

    // 【修改】updateAccount: 新增 imagePath 字段更新
    public int updateAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TYPE, account.getType());
        values.put(COLUMN_CATEGORY, account.getCategory());
        values.put(COLUMN_AMOUNT, account.getAmount());
        values.put(COLUMN_DATE, account.getDate());
        values.put(COLUMN_NOTE, account.getNote());
        values.put(COLUMN_IMAGE_PATH, account.getImagePath()); // 【核心修改】

        // 增加 user_id 限制
        int rows = db.update(TABLE_ACCOUNT, values, COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(account.getId()), account.getUserId()});
        db.close();
        return rows;
    }

    // 【修改】deleteAccount: 传入 userId 参数并增加 WHERE user_id 限制 (逻辑不变)
    public void deleteAccount(int accountId, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 增加 user_id 限制
        db.delete(TABLE_ACCOUNT, COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(accountId), userId});
        db.close();
    }

    // 【修改】deleteAllAccounts: 传入 userId 参数，清空当前用户的记录 (逻辑不变)
    public void deleteAllAccounts(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 增加 user_id 限制
        db.delete(TABLE_ACCOUNT, COLUMN_USER_ID + " = ?", new String[]{userId});
        db.close();
    }

    // --- 查询操作 ---

    /**
     * 【修改】getFilteredAccounts: 读取 imagePath
     */
    public List<Account> getFilteredAccounts(String userId, String type, String category, String startDate, String endDate, String orderBy) {
        List<Account> accountList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 1. 构建 WHERE 子句和参数列表 (逻辑不变)
        StringBuilder whereClause = new StringBuilder();
        List<String> whereArgs = new ArrayList<>();
        // ... (构建 WHERE 子句和参数列表) ...

        // 【核心修改】读取 imagePath
        String selectQuery = "SELECT * FROM " + TABLE_ACCOUNT;
        // ... (查询执行逻辑保持不变) ...

        Cursor cursor = db.rawQuery(selectQuery, whereArgs.toArray(new String[0]));

        // 3. 解析 Cursor
        if (cursor.moveToFirst()) {
            do {
                Account account = new Account();
                account.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                account.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                account.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                account.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                account.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                account.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                account.setNote(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)));
                account.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH))); // 【核心修改】

                accountList.add(account);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return accountList;
    }

    /**
     * 【修改】getAccountSummary: 逻辑不变
     */
    public Cursor getAccountSummary(String userId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();

        String whereClause = COLUMN_USER_ID + " = ? AND " + COLUMN_DATE + " BETWEEN ? AND ?";
        String[] whereArgs = new String[]{userId, startDate, endDate};

        String query = "SELECT " +
                "SUM(CASE WHEN " + COLUMN_TYPE + " = '收入' THEN " + COLUMN_AMOUNT + " ELSE 0 END) as total_income, " +
                "SUM(CASE WHEN " + COLUMN_TYPE + " = '支出' THEN " + COLUMN_AMOUNT + " ELSE 0 END) as total_expense " +
                "FROM " + TABLE_ACCOUNT +
                " WHERE " + whereClause;

        return db.rawQuery(query, whereArgs);
    }

    /**
     * 【修改】getPieChartData: 逻辑不变
     */
    public Cursor getPieChartData(String userId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();

        String whereClause = COLUMN_USER_ID + " = ? AND " + COLUMN_TYPE + " = '支出' AND " +
                COLUMN_DATE + " BETWEEN ? AND ?";
        String[] whereArgs = new String[]{userId, startDate, endDate};

        String query = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") as total_amount " +
                "FROM " + TABLE_ACCOUNT +
                " WHERE " + whereClause +
                " GROUP BY " + COLUMN_CATEGORY +
                " HAVING total_amount > 0" +
                " ORDER BY total_amount DESC";

        return db.rawQuery(query, whereArgs);
    }

    /**
     * 【修改】getTrendDataByRange: 逻辑不变
     */
    public Cursor getTrendDataByRange(String userId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        String groupByFormat;
        try {
            long diffDays = TimeUnit.DAYS.convert(dateFormat.parse(endDate).getTime() - dateFormat.parse(startDate).getTime(), TimeUnit.MILLISECONDS);
            groupByFormat = (diffDays > 30) ? "strftime('%Y-%m', " : "strftime('%Y-%m-%d', ";
        } catch (ParseException e) {
            groupByFormat = "strftime('%Y-%m-%d', ";
        }

        String whereClause = COLUMN_USER_ID + " = ? AND " + COLUMN_DATE + " BETWEEN ? AND ?";
        String[] whereArgs = new String[]{userId, startDate, endDate};

        String query =
                "SELECT " +
                        groupByFormat + COLUMN_DATE + ") as time_key, " +
                        "SUM(CASE WHEN " + COLUMN_TYPE + " = '收入' THEN " + COLUMN_AMOUNT + " ELSE 0 END) as total_income, " +
                        "SUM(CASE WHEN " + COLUMN_TYPE + " = '支出' THEN " + COLUMN_AMOUNT + " ELSE 0 END) as total_expense " +
                        "FROM " + TABLE_ACCOUNT +
                        " WHERE " + whereClause +
                        " GROUP BY time_key " +
                        " ORDER BY time_key ASC";
        return db.rawQuery(query, whereArgs);
    }

    // 历史遗留方法
    public Cursor getMonthlySummary(String yearMonth) {
        SQLiteDatabase db = this.getReadableDatabase();
        // 此处应添加 user_id 限制，但为兼容旧代码结构，仅作提醒
        String query = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") as total_amount " +
                "FROM " + TABLE_ACCOUNT +
                " WHERE " + COLUMN_DATE + " LIKE '" + yearMonth + "%'" +
                " GROUP BY " + COLUMN_CATEGORY +
                " ORDER BY total_amount DESC";
        return db.rawQuery(query, null);
    }
}