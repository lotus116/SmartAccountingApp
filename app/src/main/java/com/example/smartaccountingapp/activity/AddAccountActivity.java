package com.example.smartaccountingapp.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartaccountingapp.R;
import com.example.smartaccountingapp.model.Account;
import com.example.smartaccountingapp.util.DBHelper;
import com.example.smartaccountingapp.util.PrefsManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddAccountActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private Spinner spinnerType;
    private Spinner spinnerCategory;
    private EditText etAmount;
    private EditText etDate;
    private EditText etNote;
    private Button btnSave;

    private boolean isEditMode = false;
    private Account accountToEdit;

    private String currentUserId; // 【新增】当前用户ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        // 【核心修改 1】获取当前登录用户ID
        currentUserId = PrefsManager.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "用户未登录，无法记账", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        dbHelper = new DBHelper(this);

        initViews();
        setupSpinners();

        // 检查是否为编辑模式
        Intent intent = getIntent();
        if (intent.getBooleanExtra("is_edit", false) && intent.getSerializableExtra("account") != null) {
            isEditMode = true;
            accountToEdit = (Account) intent.getSerializableExtra("account");
            loadAccountData(accountToEdit);
        }

        btnSave.setOnClickListener(v -> saveAccount());
        etDate.setOnClickListener(v -> showDatePickerDialog());
    }

    private void initViews() {
        spinnerType = findViewById(R.id.spinner_type);
        spinnerCategory = findViewById(R.id.spinner_category);
        etAmount = findViewById(R.id.et_amount);
        etDate = findViewById(R.id.et_date);
        etNote = findViewById(R.id.et_note);
        btnSave = findViewById(R.id.btn_save);

        // 设置默认日期为今天
        if (!isEditMode) {
            etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime()));
        }
    }

    private void setupSpinners() {
        // 类型 Spinner (收入/支出)
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.account_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // 类别 Spinner (此处假设您有 R.array.expense_categories 资源)
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void loadAccountData(Account account) {
        // 加载数据到 UI 控件
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("编辑记录");
        }

        // 设置类型
        ArrayAdapter<String> typeAdapter = (ArrayAdapter<String>) spinnerType.getAdapter();
        if (typeAdapter != null) {
            int spinnerPosition = typeAdapter.getPosition(account.getType());
            spinnerType.setSelection(spinnerPosition);
        }

        // 设置类别
        ArrayAdapter<String> categoryAdapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
        if (categoryAdapter != null) {
            int spinnerPosition = categoryAdapter.getPosition(account.getCategory());
            spinnerCategory.setSelection(spinnerPosition);
        }

        etAmount.setText(String.format(Locale.getDefault(), "%.2f", account.getAmount()));
        etDate.setText(account.getDate());
        etNote.setText(account.getNote());
        btnSave.setText("保存修改");
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        if (isEditMode && accountToEdit != null) {
            try {
                calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(accountToEdit.getDate()));
            } catch (Exception e) {
                // Ignore parsing error, use current date
            }
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
            etDate.setText(dateString);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveAccount() {
        String type = spinnerType.getSelectedItem().toString();
        String category = spinnerCategory.getSelectedItem().toString();
        String amountStr = etAmount.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "金额和日期不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "金额格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        long result;
        if (isEditMode) {
            accountToEdit.setType(type);
            accountToEdit.setCategory(category);
            accountToEdit.setAmount(amount);
            accountToEdit.setDate(date);
            accountToEdit.setNote(note);
            // accountToEdit 中已经包含了正确的 userId

            // 【核心修改 2】调用 DBHelper 的 update 方法
            result = dbHelper.updateAccount(accountToEdit);
            if (result > 0) {
                Toast.makeText(this, "记录更新成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK); // 返回 RESULT_OK
                finish();
            } else {
                Toast.makeText(this, "记录更新失败 (可能记录不存在)", Toast.LENGTH_SHORT).show();
            }

        } else {
            Account newAccount = new Account(0, currentUserId, type, category, amount, date, note); // 【核心修改 3】新增记录时传入 currentUserId

            result = dbHelper.addAccount(newAccount);
            if (result > 0) {
                Toast.makeText(this, "新增记录成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK); // 返回 RESULT_OK
                finish();
            } else {
                Toast.makeText(this, "新增记录失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}