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

    private String currentUserId; // 当前登录用户的ID

    private static final int REQUEST_CODE_ADD = 1;
    private static final int REQUEST_CODE_EDIT = 2;

    // 筛选相关成员变量
    private String currentTypeFilter = null;
    private String currentCategoryFilter = null;
    private String currentStartDate = null;
    private String currentEndDate = null;
    private String currentOrderBy = DBHelper.COLUMN_DATE + " DESC, " + DBHelper.COLUMN_ID + " DESC"; // 默认排序

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取当前登录用户ID
        currentUserId = PrefsManager.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "用户未登录，请先登录", Toast.LENGTH_LONG).show();
            // 确保未登录时跳转到登录界面
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // 初始化 DBHelper
        dbHelper = new DBHelper(this);

        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAccountActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD);
        });

        // 初始化 RecyclerView 和 Adapter
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 首次加载时，先获取数据
        List<Account> initialAccounts = dbHelper.getFilteredAccounts(currentUserId, null, null, null, null, currentOrderBy);
        adapter = new AccountAdapter(this, initialAccounts);
        recyclerView.setAdapter(adapter);

        // 设置 Adapter 的点击监听器
        adapter.setOnItemClickListener(new AccountAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(int accountId) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("删除确认")
                        .setMessage("确定删除此条记录吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            // 传入 currentUserId
                            dbHelper.deleteAccount(accountId, currentUserId);
                            loadAccounts();
                            Toast.makeText(MainActivity.this, "记录已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }

            @Override
            public void onItemClick(Account account) {
                // 跳转到编辑页面
                Intent intent = new Intent(MainActivity.this, AddAccountActivity.class);
                intent.putExtra("is_edit", true);
                intent.putExtra("account", account);
                startActivityForResult(intent, REQUEST_CODE_EDIT);
            }
        });

        // 初始化筛选器
        initFilterSpinner();

        // 首次启动时，默认筛选最近 30 天
        if (currentStartDate == null) {
            setFilterToLast30Days();
        }

        loadAccounts(); // 确保加载最新的数据
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_ADD || requestCode == REQUEST_CODE_EDIT) {
                // 刷新数据
                loadAccounts();
            }
        }
    }

    private void loadAccounts() {
        // 传入 currentUserId 进行筛选
        List<Account> accounts = dbHelper.getFilteredAccounts(currentUserId, currentTypeFilter, currentCategoryFilter, currentStartDate, currentEndDate, currentOrderBy);
        adapter.updateData(accounts);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_filter) {
            showFilterDialog();
            return true;
        } else if (id == R.id.action_export) {
            showExportDialog();
            return true;
        } else if (id == R.id.action_import) {
            showImportDialog();
            return true;
        } else if (id == R.id.action_chart) {
            // 跳转到 ChartActivity 时，传递当前用户ID
            Intent intent = new Intent(this, ChartActivity.class);
            intent.putExtra("user_id", currentUserId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            // 执行退出登录
            PrefsManager.logout(this);
            // 跳转到登录界面
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 【新增】根据当前用户ID生成专属备份文件名
     */
    private String getBackupFileName() {
        // 文件名格式：accounts_backup_[user_id].json
        return "accounts_backup_" + currentUserId + ".json";
    }

    // 导出确认对话框
    private void showExportDialog() {
        // 获取当前用户的所有记录
        List<Account> accounts = dbHelper.getFilteredAccounts(currentUserId, null, null, null, null, null);

        if (accounts.isEmpty()) {
            Toast.makeText(this, "当前用户没有记账记录可导出", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = getBackupFileName(); // 使用用户专属文件名

        new AlertDialog.Builder(this)
                .setTitle("数据导出")
                // 【修正 1】更新提示信息，显示用户专属文件名
                .setMessage("确定将当前 " + accounts.size() + " 条记录导出到 " + fileName + " 文件吗？(此操作会覆盖旧备份)")
                .setPositiveButton("确定", (dialog, which) -> {
                    String json = FileUtil.convertAccountsToJson(accounts);

                    // 【修正 2】使用用户专属文件名进行保存
                    if (FileUtil.saveToFile(MainActivity.this, fileName, json)) {
                        Toast.makeText(MainActivity.this, "数据导出成功 (" + accounts.size() + "条)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "数据导出失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }


    // 导入确认对话框 (Dialog)
    private void showImportDialog() {
        String fileName = getBackupFileName(); // 使用用户专属文件名

        // 【修正 3】读取用户专属备份文件
        String json = FileUtil.readFromFile(this, fileName);
        if (json == null) {
            // 更新提示信息，明确告诉用户找不到谁的备份文件
            Toast.makeText(this, "找不到当前用户 (" + currentUserId + ") 的备份文件: " + fileName, Toast.LENGTH_LONG).show();
            return;
        }

        List<Account> accounts = FileUtil.convertJsonToAccounts(json);
        if (accounts.isEmpty()) {
            Toast.makeText(this, "备份文件数据为空或格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前用户的所有记录的总数
        int currentAccountCount = dbHelper.getFilteredAccounts(currentUserId, null, null, null, null, currentOrderBy).size();

        new AlertDialog.Builder(this)
                .setTitle("数据导入")
                .setMessage("导入将清空并覆盖现有 " + currentAccountCount + " 条数据，确定导入备份文件中的 " + accounts.size() + " 条数据吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    dbHelper.deleteAllAccounts(currentUserId); // 清空当前用户的记录
                    for (Account account : accounts) {
                        account.setId(0);
                        account.setUserId(currentUserId); // 这一步是关键，确保导入的记录属于当前用户
                        dbHelper.addAccount(account);
                    }
                    // 重置筛选器并加载新数据
                    currentTypeFilter = null;
                    currentCategoryFilter = null;
                    setFilterToLast30Days();
                    loadAccounts();
                    Toast.makeText(MainActivity.this, "数据导入成功 (" + accounts.size() + "条)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }


    // --- 筛选/排序逻辑 (与之前保持一致) ---

    private void initFilterSpinner() {
        Spinner sortSpinner = findViewById(R.id.spinner_sort_mode);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.contains("最新优先")) {
                    currentOrderBy = DBHelper.COLUMN_DATE + " DESC, " + DBHelper.COLUMN_ID + " DESC";
                } else if (selected.contains("最早优先")) {
                    currentOrderBy = DBHelper.COLUMN_DATE + " ASC, " + DBHelper.COLUMN_ID + " ASC";
                } else if (selected.contains("金额最高")) {
                    currentOrderBy = DBHelper.COLUMN_AMOUNT + " DESC";
                } else if (selected.contains("金额最低")) {
                    currentOrderBy = DBHelper.COLUMN_AMOUNT + " ASC";
                }
                loadAccounts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentOrderBy = DBHelper.COLUMN_DATE + " DESC, " + DBHelper.COLUMN_ID + " DESC";
                loadAccounts();
            }
        });
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.filter_dialog, null);
        builder.setView(dialogView);

        Spinner typeSpinner = dialogView.findViewById(R.id.filter_spinner_type);
        Spinner categorySpinner = dialogView.findViewById(R.id.filter_spinner_category);
        EditText etStartDate = dialogView.findViewById(R.id.et_filter_start_date);
        EditText etEndDate = dialogView.findViewById(R.id.et_filter_end_date);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.account_types_with_all, android.R.layout.simple_spinner_item);
        typeSpinner.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories_with_all, android.R.layout.simple_spinner_item);
        categorySpinner.setAdapter(categoryAdapter);

        // 设置当前筛选值
        if (currentTypeFilter != null) {
            typeSpinner.setSelection(typeAdapter.getPosition(currentTypeFilter));
        }
        if (currentCategoryFilter != null) {
            categorySpinner.setSelection(categoryAdapter.getPosition(currentCategoryFilter));
        }
        if (currentStartDate != null) {
            etStartDate.setText(currentStartDate);
        }
        if (currentEndDate != null) {
            etEndDate.setText(currentEndDate);
        }

        // 日期选择器
        etStartDate.setOnClickListener(v -> showDatePickerDialog(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePickerDialog(etEndDate));

        builder.setTitle("筛选记录")
                .setPositiveButton("应用筛选", (dialog, which) -> {
                    currentTypeFilter = typeSpinner.getSelectedItem().toString();
                    currentCategoryFilter = categorySpinner.getSelectedItem().toString();
                    currentStartDate = etStartDate.getText().toString();
                    currentEndDate = etEndDate.getText().toString();

                    if ("全部".equals(currentTypeFilter)) currentTypeFilter = null;
                    if ("全部".equals(currentCategoryFilter)) currentCategoryFilter = null;
                    if (currentStartDate.isEmpty() || currentEndDate.isEmpty()) {
                        currentStartDate = null;
                        currentEndDate = null;
                    }

                    loadAccounts();
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("清除筛选", (dialog, which) -> {
                    currentTypeFilter = null;
                    currentCategoryFilter = null;
                    currentStartDate = null;
                    currentEndDate = null;
                    // 默认筛选最近 30 天
                    setFilterToLast30Days();
                    loadAccounts();
                })
                .show();
    }

    private void showDatePickerDialog(EditText dateEditText) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // 尝试解析 EditText 中的当前日期
        String currentText = dateEditText.getText().toString();
        if (!currentText.isEmpty()) {
            try {
                Date date = sdf.parse(currentText);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception e) {
                // 解析失败，使用当前日期
            }
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            String dateString = sdf.format(selectedDate.getTime());
            dateEditText.setText(dateString);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // 设置默认筛选范围为最近 30 天
    private void setFilterToLast30Days() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // 设置结束日期为今天
        currentEndDate = sdf.format(calendar.getTime());

        // 设置开始日期为 30 天前
        calendar.add(Calendar.DAY_OF_YEAR, -29);
        currentStartDate = sdf.format(calendar.getTime());
    }
}