package com.example.smartaccountingapp.model;

import java.io.Serializable; // 【新增】导入 Serializable

// 【修改】让 Account 类实现 Serializable 接口
public class Account implements Serializable {
    // Java 建议为 Serializable 类添加 serialVersionUID
    private static final long serialVersionUID = 1L;

    private int id;
    private String type; // 收入/支出
    private String category;
    private double amount;
    private String date; // YYYY-MM-DD
    private String note;

    public Account() {
        // 无参构造函数 (Gson 反序列化需要)
    }

    public Account(int id, String type, String category, double amount, String date, String note) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }

    // --- Getter and Setter Methods ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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