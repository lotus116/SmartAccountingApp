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
    private static final int DATABASE_VERSION = 2; // 【修改】升级版本号以应用结构变更

    // 表名和列名
    public static final String TABLE_ACCOUNT = "accounts";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID = "user_id"; // 【新增】用户ID列
    public static final String COLUMN_TYPE = "type"; // 收入/支出
    public static final String COLUMN_CATEGORY = "category"; // 类别
    public static final String COLUMN_AMOUNT = "amount"; // 金额 (REAL 浮点型)
    public static final String COLUMN_DATE = "date"; // 日期 (TEXT YYYY-MM-DD)
    public static final String COLUMN_NOTE = "note"; // 备注

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建记账表 (【修改】增加 COLUMN_USER_ID 列)
        String CREATE_ACCOUNT_TABLE = "CREATE TABLE " + TABLE_ACCOUNT + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " TEXT NOT NULL DEFAULT 'default_user'," // 新增用户ID列，设置默认值以防数据丢失
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_AMOUNT + " REAL,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_NOTE + " TEXT"
                + ")";
        db.execSQL(CREATE_ACCOUNT_TABLE);
    }

    // 【修改】处理数据库升级
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 如果是从旧版本 (V1) 升级上来 (V2)，添加 user_id 列
        if (oldVersion < 2) {
            // 默认值 'default_user' 用于保留 V1 版本的数据
            db.execSQL("ALTER TABLE " + TABLE_ACCOUNT + " ADD COLUMN " + COLUMN_USER_ID + " TEXT DEFAULT 'default_user'");
        } else {
            // 如果版本跨度较大，删除重建
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNT);
            onCreate(db);
        }
    }


    // --- CRUD 操作 ---

    // 【修改】新增 userId 字段保存
    public long addAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, account.getUserId()); // 【核心修改】保存用户ID
        values.put(COLUMN_TYPE, account.getType());
        values.put(COLUMN_CATEGORY, account.getCategory());
        values.put(COLUMN_AMOUNT, account.getAmount());
        values.put(COLUMN_DATE, account.getDate());
        values.put(COLUMN_NOTE, account.getNote());

        long id = db.insert(TABLE_ACCOUNT, null, values);
        db.close();
        return id;
    }

    // 【修改】增加 WHERE user_id 限制
    public int updateAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TYPE, account.getType());
        values.put(COLUMN_CATEGORY, account.getCategory());
        values.put(COLUMN_AMOUNT, account.getAmount());
        values.put(COLUMN_DATE, account.getDate());
        values.put(COLUMN_NOTE, account.getNote());

        // 【核心修改】增加 user_id 限制
        int rows = db.update(TABLE_ACCOUNT, values, COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(account.getId()), account.getUserId()});
        db.close();
        return rows;
    }

    // 【修改】传入 userId 参数并增加 WHERE user_id 限制
    public void deleteAccount(int accountId, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 【核心修改】增加 user_id 限制
        db.delete(TABLE_ACCOUNT, COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(accountId), userId});
        db.close();
    }

    // 【修改】传入 userId 参数，清空当前用户的记录
    public void deleteAllAccounts(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 【核心修改】增加 user_id 限制
        db.delete(TABLE_ACCOUNT, COLUMN_USER_ID + " = ?", new String[]{userId});
        db.close();
    }

    // --- 查询操作 ---

    /**
     * 【修改】新增 userId 参数，所有查询都基于当前用户
     */
    public List<Account> getFilteredAccounts(String userId, String type, String category, String startDate, String endDate, String orderBy) {
        List<Account> accountList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 1. 构建 WHERE 子句和参数列表
        StringBuilder whereClause = new StringBuilder();
        List<String> whereArgs = new ArrayList<>();

        // 【核心修改】始终根据用户ID筛选
        whereClause.append(COLUMN_USER_ID).append(" = ?");
        whereArgs.add(userId);


        if (type != null && !type.isEmpty() && !"全部".equals(type)) {
            if (whereClause.length() > 0) whereClause.append(" AND ");
            whereClause.append(COLUMN_TYPE).append(" = ?");
            whereArgs.add(type);
        }
        if (category != null && !category.isEmpty() && !"全部".equals(category)) {
            if (whereClause.length() > 0) whereClause.append(" AND ");
            whereClause.append(COLUMN_CATEGORY).append(" = ?");
            whereArgs.add(category);
        }
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            if (whereClause.length() > 0) whereClause.append(" AND ");
            whereClause.append(COLUMN_DATE).append(" BETWEEN ? AND ?");
            whereArgs.add(startDate);
            whereArgs.add(endDate);
        }

        String whereString = whereClause.length() > 0 ? whereClause.toString() : null;
        String[] whereArray = whereArgs.toArray(new String[0]);

        // 2. 执行查询
        String selectQuery = "SELECT * FROM " + TABLE_ACCOUNT;
        if (whereString != null) {
            selectQuery += " WHERE " + whereString;
        }

        String finalOrderBy = (orderBy != null && !orderBy.isEmpty()) ? orderBy : COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC";
        selectQuery += " ORDER BY " + finalOrderBy;

        Cursor cursor = db.rawQuery(selectQuery, whereArray);

        // 3. 解析 Cursor
        if (cursor.moveToFirst()) {
            do {
                Account account = new Account();
                account.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                account.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID))); // 【新增】读取用户ID
                account.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                account.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                account.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                account.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                account.setNote(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)));
                accountList.add(account);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return accountList;
    }

    /**
     * 【修改】获取当前用户总收支的摘要信息
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
     * 【修改】获取当前用户的支出饼图数据
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
     * 【修改】获取当前用户的收支趋势折线图数据
     */
    public Cursor getTrendDataByRange(String userId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        String groupByFormat;
        try {
            // 超过一个月按月分组，否则按日分组
            long diffDays = TimeUnit.DAYS.convert(dateFormat.parse(endDate).getTime() - dateFormat.parse(startDate).getTime(), TimeUnit.MILLISECONDS);
            groupByFormat = (diffDays > 30) ? "strftime('%Y-%m', " : "strftime('%Y-%m-%d', ";
        } catch (ParseException e) {
            groupByFormat = "strftime('%Y-%m-%d', "; // 错误时默认按日
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

    // 历史遗留方法，新代码已不再调用此方法
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