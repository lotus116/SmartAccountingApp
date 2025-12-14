package com.example.smartaccountingapp.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.smartaccountingapp.model.Account;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AccountDB";
    private static final int DATABASE_VERSION = 1;

    // 表名和列名
    public static final String TABLE_ACCOUNT = "accounts";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TYPE = "type"; // 收入/支出
    public static final String COLUMN_CATEGORY = "category"; // 类别
    public static final String COLUMN_AMOUNT = "amount"; // 金额 (REAL 浮点型)
    public static final String COLUMN_DATE = "date"; // 日期 (TEXT YYYY-MM-DD)
    public static final String COLUMN_NOTE = "note"; // 备注

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建记账表
        String CREATE_ACCOUNT_TABLE = "CREATE TABLE " + TABLE_ACCOUNT + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_AMOUNT + " REAL,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_NOTE + " TEXT" + ")";
        db.execSQL(CREATE_ACCOUNT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNT);
        onCreate(db);
    }

    // ---------------------------------
    // CRUD: 增 (Create)
    // ---------------------------------
    public long addAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TYPE, account.getType());
        values.put(COLUMN_CATEGORY, account.getCategory());
        values.put(COLUMN_AMOUNT, account.getAmount());
        values.put(COLUMN_DATE, account.getDate());
        values.put(COLUMN_NOTE, account.getNote());
        long id = db.insert(TABLE_ACCOUNT, null, values);
        db.close();
        return id;
    }

    // ---------------------------------
    // CRUD: 删 (Delete)
    // ---------------------------------
    public void deleteAccount(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ACCOUNT, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteAllAccounts() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ACCOUNT, null, null);
        db.close();
    }

    // ---------------------------------
    // CRUD: 改 (Update) - 【新增/编辑功能所需】
    // ---------------------------------
    public int updateAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TYPE, account.getType());
        values.put(COLUMN_CATEGORY, account.getCategory());
        values.put(COLUMN_AMOUNT, account.getAmount());
        values.put(COLUMN_DATE, account.getDate());
        values.put(COLUMN_NOTE, account.getNote());

        int rowsAffected = db.update(TABLE_ACCOUNT, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(account.getId())});
        db.close();
        return rowsAffected;
    }


    // ---------------------------------
    // CRUD: 查 (Read) - 【列表筛选所需】
    // ---------------------------------
    public List<Account> getFilteredAccounts(String typeFilter, String categoryFilter, String startDateFilter, String endDateFilter) {
        List<Account> accountList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder whereClause = new StringBuilder();
        List<String> whereArgs = new ArrayList<>();

        if (typeFilter != null) {
            whereClause.append(COLUMN_TYPE).append(" = ?");
            whereArgs.add(typeFilter);
        }

        if (categoryFilter != null) {
            if (whereClause.length() > 0) whereClause.append(" AND ");
            whereClause.append(COLUMN_CATEGORY).append(" = ?");
            whereArgs.add(categoryFilter);
        }

        // 日期范围筛选
        if (startDateFilter != null && endDateFilter != null) {
            if (whereClause.length() > 0) whereClause.append(" AND ");
            whereClause.append(COLUMN_DATE).append(" BETWEEN ? AND ?");
            whereArgs.add(startDateFilter);
            whereArgs.add(endDateFilter);
        } else if (startDateFilter != null) { // 仅开始日期
            if (whereClause.length() > 0) whereClause.append(" AND ");
            whereClause.append(COLUMN_DATE).append(" >= ?");
            whereArgs.add(startDateFilter);
        } else if (endDateFilter != null) { // 仅结束日期
            if (whereClause.length() > 0) whereClause.append(" AND ");
            whereClause.append(COLUMN_DATE).append(" <= ?");
            whereArgs.add(endDateFilter);
        }

        String whereString = whereClause.length() > 0 ? whereClause.toString() : null;
        String[] whereArray = whereArgs.toArray(new String[0]);

        String selectQuery = "SELECT * FROM " + TABLE_ACCOUNT;
        if (whereString != null) {
            selectQuery += " WHERE " + whereString;
        }
        selectQuery += " ORDER BY " + COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC"; // 默认排序

        Cursor cursor = db.rawQuery(selectQuery, whereArray);

        if (cursor.moveToFirst()) {
            do {
                Account account = new Account();
                account.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                account.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                account.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                account.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                account.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                account.setNote(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)));
                accountList.add(account);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return accountList;
    }


    // ---------------------------------
    // 图表数据查询 (用于 ChartActivity) - 【图表所需】
    // ---------------------------------

    // 查：根据日期范围获取支出总额 (用于饼图)
    public Cursor getExpenseSummaryByRange(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") as total_amount " +
                "FROM " + TABLE_ACCOUNT +
                " WHERE " + COLUMN_DATE + " BETWEEN '" + startDate + "' AND '" + endDate + "'" +
                " AND " + COLUMN_TYPE + " = '支出'" +
                " GROUP BY " + COLUMN_CATEGORY;
        return db.rawQuery(query, null);
    }

    // 查：获取指定日期范围内的总收入
    public double getTotalIncomeByRange(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + COLUMN_AMOUNT + ") as total_income " +
                "FROM " + TABLE_ACCOUNT +
                " WHERE " + COLUMN_DATE + " BETWEEN '" + startDate + "' AND '" + endDate + "'" +
                " AND " + COLUMN_TYPE + " = '收入'";

        Cursor cursor = db.rawQuery(query, null);
        double totalIncome = 0;
        if (cursor.moveToFirst()) {
            totalIncome = cursor.getDouble(cursor.getColumnIndexOrThrow("total_income"));
        }
        cursor.close();
        return totalIncome;
    }

    // 查：获取指定日期范围内的收支趋势 (用于折线图)
    public Cursor getTrendDataByRange(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        // 超过一个月按月分组，否则按日分组
        String groupByFormat = (endDate.length() - startDate.length() > 30) ? "strftime('%Y-%m', " : "strftime('%Y-%m-%d', ";

        String query =
                "SELECT " +
                        groupByFormat + COLUMN_DATE + ") as time_key, " +
                        "SUM(CASE WHEN " + COLUMN_TYPE + " = '收入' THEN " + COLUMN_AMOUNT + " ELSE 0 END) as total_income, " +
                        "SUM(CASE WHEN " + COLUMN_TYPE + " = '支出' THEN " + COLUMN_AMOUNT + " ELSE 0 END) as total_expense " +
                        "FROM " + TABLE_ACCOUNT +
                        " WHERE " + COLUMN_DATE + " BETWEEN '" + startDate + "' AND '" + endDate + "'" +
                        " GROUP BY time_key " +
                        " ORDER BY time_key ASC";
        return db.rawQuery(query, null);
    }

    // 历史遗留方法，新代码已不再调用此方法
    public Cursor getMonthlySummary(String yearMonth) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") as total_amount " +
                "FROM " + TABLE_ACCOUNT +
                " WHERE " + COLUMN_DATE + " LIKE '" + yearMonth + "%'" +
                " AND " + COLUMN_TYPE + " = '支出'" +
                " GROUP BY " + COLUMN_CATEGORY;
        return db.rawQuery(query, null);
    }
}