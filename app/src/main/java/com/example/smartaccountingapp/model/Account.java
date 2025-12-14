package com.example.smartaccountingapp.model;

import java.io.Serializable;

public class Account implements Serializable {
    private int id;
    private String userId; // 【新增】用户ID，用于区分不同用户的记录
    private String type; // 收入/支出
    private String category;
    private double amount;
    private String date; // YYYY-MM-DD
    private String note;

    public Account() {
        // 无参构造函数 (Gson 反序列化需要)
    }

    // 【修改】构造函数新增 userId 参数
    public Account(int id, String userId, String type, String category, double amount, String date, String note) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }

    // --- Getter and Setter Methods ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // 【新增】
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}