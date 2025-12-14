package com.example.smartaccountingapp.activity;

import android.app.DatePickerDialog;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddAccountActivity extends AppCompatActivity {
    // 定义用于 Intent 传参的 Key
    public static final String EXTRA_ACCOUNT = "extra_account";

    private EditText etAmount;
    private EditText etDate;
    private EditText etNote;
    private Spinner spinnerType;
    private Spinner spinnerCategory;
    private Button btnSave;
    private Calendar calendar;
    private Account currentAccount = null; // 用于存储当前编辑的记录

    // 用于数据库操作和 UI 显示的日期格式
    private SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        // 控件初始化
        etAmount = findViewById(R.id.et_amount);
        etDate = findViewById(R.id.et_date);
        etNote = findViewById(R.id.et_note);
        spinnerType = findViewById(R.id.spinner_type);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSave = findViewById(R.id.btn_save);

        setupSpinners(); // 设置下拉列表

        calendar = Calendar.getInstance();

        // 检查 Intent 中是否有 Account 对象 (判断是编辑还是新增)
        if (getIntent().hasExtra(EXTRA_ACCOUNT)) {
            currentAccount = (Account) getIntent().getSerializableExtra(EXTRA_ACCOUNT);
            if (currentAccount != null) {
                // 编辑模式：预填充数据
                prefillData(currentAccount);
                getSupportActionBar().setTitle("编辑记账记录");
            }
        } else {
            // 新增模式：默认显示当前日期
            updateDateEditText(calendar.getTime());
            getSupportActionBar().setTitle("新增记账记录");
        }

        etDate.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> saveOrUpdateAccount());
    }

    private void setupSpinners() {
        // Spinner 类型设置 (收入/支出)
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.account_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // Spinner 类别设置
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    /**
     * 编辑模式：根据传入的 Account 对象预填充界面数据
     */
    private void prefillData(Account account) {
        // 1. 设置金额、备注和日期
        etAmount.setText(String.format(Locale.getDefault(), "%.2f", account.getAmount()));
        etNote.setText(account.getNote());
        etDate.setText(account.getDate());

        // 解析日期并设置给 Calendar，以便 DatePicker 弹出时定位正确
        try {
            Date date = sdfDb.parse(account.getDate());
            if (date != null) {
                calendar.setTime(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // 2. 预选类型 (收入/支出)
        ArrayAdapter<CharSequence> typeAdapter = (ArrayAdapter<CharSequence>) spinnerType.getAdapter();
        if (typeAdapter != null) {
            int position = typeAdapter.getPosition(account.getType());
            spinnerType.setSelection(position);
        }

        // 3. 预选类别
        ArrayAdapter<CharSequence> categoryAdapter = (ArrayAdapter<CharSequence>) spinnerCategory.getAdapter();
        if (categoryAdapter != null) {
            int position = categoryAdapter.getPosition(account.getCategory());
            spinnerCategory.setSelection(position);
        }

        btnSave.setText("保存修改");
    }

    /**
     * 保存（新增）或更新（编辑）记账记录
     */
    private void saveOrUpdateAccount() {
        String type = spinnerType.getSelectedItem().toString();
        String category = spinnerCategory.getSelectedItem().toString();
        String amountStr = etAmount.getText().toString();
        String date = etDate.getText().toString();
        String note = etNote.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            DBHelper dbHelper = new DBHelper(this);
            long result;

            if (currentAccount == null) {
                // 新增模式
                Account newAccount = new Account(0, type, category, amount, date, note);
                result = dbHelper.addAccount(newAccount);
                if (result > 0) {
                    Toast.makeText(this, "记账成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "记账失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 编辑模式
                currentAccount.setType(type);
                currentAccount.setCategory(category);
                currentAccount.setAmount(amount);
                currentAccount.setDate(date);
                currentAccount.setNote(note);
                result = dbHelper.updateAccount(currentAccount); // 调用 DBHelper 中的更新方法
                if (result > 0) {
                    Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "修改失败，未作更改或记录不存在", Toast.LENGTH_SHORT).show();
                }
            }

            setResult(RESULT_OK); // 通知主页刷新
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "金额格式错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateEditText(calendar.getTime());
        };

        // DatePickerDialog (DatePicker)
        new DatePickerDialog(this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateEditText(Date date) {
        etDate.setText(sdfDb.format(date));
    }
}