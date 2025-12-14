package com.example.smartaccountingapp.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartaccountingapp.R;
import com.example.smartaccountingapp.adapter.AccountAdapter;
import com.example.smartaccountingapp.model.Account;
import com.example.smartaccountingapp.util.DBHelper;
import com.example.smartaccountingapp.util.FileUtil;
import com.example.smartaccountingapp.util.PrefsManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AccountAdapter adapter;
    private DBHelper dbHelper;

    private static final int REQUEST_CODE_ADD = 1;
    private static final int REQUEST_CODE_EDIT = 2;

    // 筛选相关成员变量
    private String currentTypeFilter = null;
    private String currentCategoryFilter = null;
    private String startDateFilter = null;
    private String endDateFilter = null;
    private Calendar calendarStart, calendarEnd;
    private SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // 【回退】排序相关成员变量
    private Spinner spinnerSortMode;
    private String currentOrderBy = DBHelper.COLUMN_DATE + " DESC, " + DBHelper.COLUMN_ID + " DESC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        calendarStart = Calendar.getInstance();
        calendarEnd = Calendar.getInstance();

        // 默认设置为最近 30 天的筛选范围
        setFilterToLast30Days();

        // FloatingActionButton
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAccountActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD);
        });

        // 【回退】初始化排序 Spinner (通过 findViewById)
        spinnerSortMode = findViewById(R.id.spinner_sort_mode);
        setupSortSpinner();

        // RecycleView 初始化
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadAccounts(); // 首次加载数据
    }

    // 【回退】设置排序 Spinner 的方法 (逻辑不变)
    private void setupSortSpinner() {
        if (spinnerSortMode == null) return; // 安全检查

        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.sort_modes, android.R.layout.simple_spinner_dropdown_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSortMode.setAdapter(sortAdapter);

        spinnerSortMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 根据用户选择的 position 设置 SQL 的 ORDER BY 子句
                switch (position) {
                    case 0: // 最新优先 (时间倒序)
                        currentOrderBy = DBHelper.COLUMN_DATE + " DESC, " + DBHelper.COLUMN_ID + " DESC";
                        break;
                    case 1: // 最早优先 (时间正序)
                        currentOrderBy = DBHelper.COLUMN_DATE + " ASC, " + DBHelper.COLUMN_ID + " ASC";
                        break;
                    case 2: // 金额最高优先
                        currentOrderBy = DBHelper.COLUMN_AMOUNT + " DESC";
                        break;
                    case 3: // 金额最低优先
                        currentOrderBy = DBHelper.COLUMN_AMOUNT + " ASC";
                        break;
                }
                loadAccounts(); // 刷新列表
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ---------------------------------
    // 数据加载与列表配置
    // ---------------------------------

    /**
     * 根据当前的筛选和排序条件从数据库加载记账记录并刷新列表
     */
    private void loadAccounts() {
        List<Account> accounts = dbHelper.getFilteredAccounts(
                currentTypeFilter,
                currentCategoryFilter,
                startDateFilter,
                endDateFilter,
                currentOrderBy
        );

        if (adapter == null) {
            adapter = new AccountAdapter(this, accounts);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(accounts);
        }

        adapter.setOnItemClickListener(new AccountAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(int accountId) {
                showDeleteDialog(accountId);
            }

            @Override
            public void onItemClick(Account account) {
                Intent intent = new Intent(MainActivity.this, AddAccountActivity.class);
                intent.putExtra(AddAccountActivity.EXTRA_ACCOUNT, account);
                startActivityForResult(intent, REQUEST_CODE_EDIT);
            }
        });
    }

    // Activity 返回结果时刷新列表
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CODE_ADD || requestCode == REQUEST_CODE_EDIT) && resultCode == RESULT_OK) {
            loadAccounts(); // 刷新列表
        }
    }

    // ---------------------------------
    // Menu 菜单配置 (保持 ActionView 修改前的简洁状态)
    // ---------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_chart) {
            startActivity(new Intent(this, ChartActivity.class));
            return true;
        } else if (id == R.id.action_filter) { // 筛选菜单项处理
            showFilterDialog();
            return true;
        } else if (id == R.id.action_export) {
            exportData();
            return true;
        } else if (id == R.id.action_import) {
            showImportDialog();
            return true;
        } else if (id == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------
    // 筛选对话框和逻辑 (保持不变)
    // ---------------------------------

    /**
     * 将筛选日期范围设置为最近 30 天
     */
    private void setFilterToLast30Days() {
        calendarEnd.setTime(new Date());
        calendarStart.setTime(new Date());
        calendarStart.add(Calendar.DAY_OF_MONTH, -29);

        startDateFilter = sdfDb.format(calendarStart.getTime());
        endDateFilter = sdfDb.format(calendarEnd.getTime());
    }

    /**
     * 显示筛选对话框
     */
    private void showFilterDialog() {
        final View dialogView = getLayoutInflater().inflate(R.layout.filter_dialog, null);

        final Spinner spinnerType = dialogView.findViewById(R.id.filter_spinner_type);
        final Spinner spinnerCategory = dialogView.findViewById(R.id.filter_spinner_category);
        final EditText etStartDate = dialogView.findViewById(R.id.et_filter_start_date);
        final EditText etEndDate = dialogView.findViewById(R.id.et_filter_end_date);


        // 1. 类型筛选初始化
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.account_types_with_all, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);
        if (currentTypeFilter == null) spinnerType.setSelection(0);
        else spinnerType.setSelection(typeAdapter.getPosition(currentTypeFilter));

        // 2. 类别筛选初始化
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories_with_all, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        if (currentCategoryFilter == null) spinnerCategory.setSelection(0);
        else spinnerCategory.setSelection(categoryAdapter.getPosition(currentCategoryFilter));

        // 3. 时间筛选预填充
        etStartDate.setText(startDateFilter);
        etEndDate.setText(endDateFilter);

        // 设置日期选择监听
        etStartDate.setOnClickListener(v -> showDatePickerDialog(etStartDate, true));
        etEndDate.setOnClickListener(v -> showDatePickerDialog(etEndDate, false));


        new AlertDialog.Builder(this)
                .setTitle("筛选记账记录")
                .setView(dialogView)
                .setPositiveButton("应用筛选", (dialog, which) -> {
                    // 1. 获取类型筛选值
                    String selectedType = spinnerType.getSelectedItem().toString();
                    currentTypeFilter = selectedType.equals("全部") ? null : selectedType;

                    // 2. 获取类别筛选值
                    String selectedCategory = spinnerCategory.getSelectedItem().toString();
                    currentCategoryFilter = selectedCategory.equals("全部") ? null : selectedCategory;

                    // 3. 获取时间筛选值
                    startDateFilter = etStartDate.getText().toString().isEmpty() ? null : etStartDate.getText().toString();
                    endDateFilter = etEndDate.getText().toString().isEmpty() ? null : etEndDate.getText().toString();

                    // 4. 执行筛选并刷新列表
                    loadAccounts();
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("清除筛选", (dialog, which) -> {
                    currentTypeFilter = null;
                    currentCategoryFilter = null;
                    // 重置为默认时间段：最近30天
                    setFilterToLast30Days();
                    loadAccounts();
                })
                .show();
    }

    /**
     * 显示日期选择对话框并更新筛选日期
     */
    private void showDatePickerDialog(EditText editText, boolean isStartDate) {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(Calendar.YEAR, year);
            selectedCalendar.set(Calendar.MONTH, monthOfYear);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            String selectedDate = sdfDb.format(selectedCalendar.getTime());
            editText.setText(selectedDate);

            if (isStartDate) {
                startDateFilter = selectedDate;
            } else {
                endDateFilter = selectedDate;
            }
        };

        Calendar initialCalendar = Calendar.getInstance();
        String dateText = editText.getText().toString();
        if (!dateText.isEmpty()) {
            try {
                Date date = sdfDb.parse(dateText);
                if (date != null) {
                    initialCalendar.setTime(date);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        new DatePickerDialog(this, dateSetListener,
                initialCalendar.get(Calendar.YEAR),
                initialCalendar.get(Calendar.MONTH),
                initialCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ---------------------------------
    // 导入/导出/删除/登出逻辑 (保持不变)
    // ---------------------------------

    // 删除确认对话框 (Dialog)
    private void showDeleteDialog(int accountId) {
        new AlertDialog.Builder(this)
                .setTitle("删除记录")
                .setMessage("确定要删除这条记账记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    dbHelper.deleteAccount(accountId);
                    loadAccounts(); // 刷新列表
                    Toast.makeText(MainActivity.this, "记录已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 登出确认对话框
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    PrefsManager prefsManager = new PrefsManager(this);
                    prefsManager.logout();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 数据导出 (文件存储)
    private void exportData() {
        List<Account> accounts = dbHelper.getFilteredAccounts(null, null, null, null, currentOrderBy);
        String json = FileUtil.convertAccountsToJson(accounts);
        if (FileUtil.saveToFile(this, "accounts_backup.json", json)) {
            Toast.makeText(this, "导出成功！文件名为: accounts_backup.json", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "数据导出失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 导入确认对话框 (Dialog)
    private void showImportDialog() {
        String json = FileUtil.readFromFile(this, "accounts_backup.json");
        if (json == null) {
            Toast.makeText(this, "找不到accounts_backup.json备份文件", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Account> accounts = FileUtil.convertJsonToAccounts(json);
        if (accounts.isEmpty()) {
            Toast.makeText(this, "备份文件数据为空或格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentAccountCount = dbHelper.getFilteredAccounts(null, null, null, null, currentOrderBy).size();

        new AlertDialog.Builder(this)
                .setTitle("数据导入")
                .setMessage("导入将清空并覆盖现有 " + currentAccountCount + " 条数据，确定导入备份文件中的 " + accounts.size() + " 条数据吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    dbHelper.deleteAllAccounts();
                    for (Account account : accounts) {
                        account.setId(0);
                        dbHelper.addAccount(account);
                    }
                    currentTypeFilter = null;
                    currentCategoryFilter = null;
                    setFilterToLast30Days();
                    loadAccounts();
                    Toast.makeText(MainActivity.this, "数据导入成功 (" + accounts.size() + "条)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}