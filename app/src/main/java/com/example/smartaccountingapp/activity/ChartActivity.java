package com.example.smartaccountingapp.activity;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.smartaccountingapp.R;
import com.example.smartaccountingapp.util.DBHelper;
import com.example.smartaccountingapp.util.PrefsManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChartActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private PieChart pieChart;
    private LineChart lineChart;
    private TextView tvDateRange;
    private TextView tvTotalExpense;
    private TextView tvTotalIncome;
    private Spinner timePresetSpinner;
    private ImageView ivSelectDate;

    private String currentUserId; // 【新增】当前登录用户的ID

    private String startDate;
    private String endDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        // 【核心修改 1】获取当前登录用户ID
        currentUserId = getIntent().getStringExtra("user_id"); // 优先从 Intent 获取
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = PrefsManager.getCurrentUserId(this); // 容错获取
        }
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "用户ID丢失，无法加载图表数据", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 初始化
        dbHelper = new DBHelper(this);
        pieChart = findViewById(R.id.pie_chart);
        lineChart = findViewById(R.id.line_chart);
        tvDateRange = findViewById(R.id.tv_date_range);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvTotalIncome = findViewById(R.id.tv_total_income);
        timePresetSpinner = findViewById(R.id.spinner_time_preset);
        ivSelectDate = findViewById(R.id.iv_select_date);

        // 初始化日期范围（默认为当月）
        initDateRangeToCurrentMonth();

        // 初始化 Spinner
        initTimePresetSpinner();

        // 初始化图表数据
        loadAllCharts();

        ivSelectDate.setOnClickListener(v -> showDatePickerDialog());
    }

    // 统一加载所有图表数据的方法
    private void loadAllCharts() {
        if (startDate == null || endDate == null) return;
        tvDateRange.setText(String.format(Locale.getDefault(), "%s 至 %s", startDate, endDate));
        loadSummaryData();
        loadPieChartData();
        loadLineChartData();
    }


    private void initTimePresetSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.time_presets, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timePresetSpinner.setAdapter(adapter);

        timePresetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // 当月
                        initDateRangeToCurrentMonth();
                        break;
                    case 1: // 最近 7 天
                        setFilterToLastNDays(7);
                        break;
                    case 2: // 最近 30 天
                        setFilterToLastNDays(30);
                        break;
                    case 3:
                        // 自定义日期，不触发数据加载
                        return;
                }
                loadAllCharts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 默认当月
                initDateRangeToCurrentMonth();
                loadAllCharts();
            }
        });
    }

    private void initDateRangeToCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        startDate = dateFormat.format(calendar.getTime());

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate = dateFormat.format(calendar.getTime());
    }

    private void setFilterToLastNDays(int days) {
        Calendar calendar = Calendar.getInstance();
        endDate = dateFormat.format(calendar.getTime());

        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1)); // 包含今天，所以减去 days-1
        startDate = dateFormat.format(calendar.getTime());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(dateFormat.parse(endDate));
        } catch (ParseException e) {
            // 忽略
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedEndDate = Calendar.getInstance();
            selectedEndDate.set(year, month, dayOfMonth);
            endDate = dateFormat.format(selectedEndDate.getTime());

            // 再次弹窗选择开始日期
            Calendar startCalendar = Calendar.getInstance();
            try {
                startCalendar.setTime(dateFormat.parse(startDate));
            } catch (ParseException e) {
                // 忽略
            }

            new DatePickerDialog(this, (view2, startYear, startMonth, startDay) -> {
                Calendar selectedStartDate = Calendar.getInstance();
                selectedStartDate.set(startYear, startMonth, startDay);
                startDate = dateFormat.format(selectedStartDate.getTime());

                if (selectedStartDate.after(selectedEndDate)) {
                    Toast.makeText(ChartActivity.this, "开始日期不能晚于结束日期", Toast.LENGTH_SHORT).show();
                    initDateRangeToCurrentMonth(); // 恢复默认
                }
                // 手动选择日期后，将 Spinner 设置为 “当月” 以外的值，防止循环触发
                timePresetSpinner.setSelection(3); // 假设自定义在最后一个位置
                loadAllCharts();

            }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH)).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // 【修改】 loadSummaryData 方法，计算当前用户收支总和
    private void loadSummaryData() {
        if (startDate == null || endDate == null) return;

        double totalIncome = 0;
        double totalExpense = 0;

        // 【核心修改 2】传入 currentUserId
        Cursor cursor = dbHelper.getAccountSummary(currentUserId, startDate, endDate);
        if (cursor.moveToFirst()) {
            totalIncome = cursor.getDouble(cursor.getColumnIndexOrThrow("total_income"));
            totalExpense = cursor.getDouble(cursor.getColumnIndexOrThrow("total_expense"));
        }
        cursor.close();

        tvTotalIncome.setText(String.format(Locale.getDefault(), "总收入: %.2f", totalIncome));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "总支出: %.2f", totalExpense));
    }


    // 【修改】 loadPieChartData 方法，用于加载当前用户的支出饼图数据
    private void loadPieChartData() {
        // 配置 PieChart 基础样式
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawEntryLabels(false);

        // 1. 从数据库获取数据
        List<PieEntry> entries = new ArrayList<>();
        // 【核心修改 3】传入 currentUserId
        Cursor cursor = dbHelper.getPieChartData(currentUserId, startDate, endDate);

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CATEGORY));
                float amount = (float) cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount"));
                entries.add(new PieEntry(amount, category));
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("所选时间段暂无支出记录");
            pieChart.invalidate();
            return;
        }

        // 2. 设置数据集
        PieDataSet dataSet = new PieDataSet(entries, "支出类别");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // 设置颜色
        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        dataSet.setColors(colors);

        // 3. 创建 PieData
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.BLACK);
        pieChart.setData(data);
        pieChart.invalidate();

        // 4. 配置 Legend
        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(5f);
    }

    // 【修改】 loadLineChartData 方法，用于加载当前用户的收支趋势折线图数据
    private void loadLineChartData() {
        // 配置 LineChart 基础样式
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.getAxisRight().setEnabled(false); // 禁用右侧 Y 轴

        // 1. 从数据库获取数据
        // 【核心修改 4】传入 currentUserId
        Cursor cursor = dbHelper.getTrendDataByRange(currentUserId, startDate, endDate);

        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        List<String> xValues = new ArrayList<>(); // X 轴标签

        if (cursor.moveToFirst()) {
            int i = 0;
            do {
                String timeKey = cursor.getString(cursor.getColumnIndexOrThrow("time_key"));
                float income = (float) cursor.getDouble(cursor.getColumnIndexOrThrow("total_income"));
                float expense = (float) cursor.getDouble(cursor.getColumnIndexOrThrow("total_expense"));

                incomeEntries.add(new Entry(i, income));
                expenseEntries.add(new Entry(i, expense));
                xValues.add(timeKey.substring(timeKey.lastIndexOf('-') + 1) + "日/月"); // 简化标签
                i++;
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (incomeEntries.isEmpty() && expenseEntries.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("所选时间段暂无收支趋势数据");
            lineChart.invalidate();
            return;
        }

        // 2. 设置数据集
        LineDataSet incomeSet = new LineDataSet(incomeEntries, "收入");
        incomeSet.setColor(Color.rgb(50, 205, 50)); // 亮绿色
        incomeSet.setCircleColor(Color.rgb(50, 205, 50));
        incomeSet.setLineWidth(2f);
        incomeSet.setCircleRadius(4f);
        incomeSet.setDrawValues(false);
        incomeSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 曲线平滑

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "支出");
        expenseSet.setColor(Color.rgb(255, 69, 0)); // 橘红色
        expenseSet.setCircleColor(Color.rgb(255, 69, 0));
        expenseSet.setLineWidth(2f);
        expenseSet.setCircleRadius(4f);
        expenseSet.setDrawValues(false);
        expenseSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 曲线平滑

        LineData lineData = new LineData(incomeSet, expenseSet);
        lineChart.setData(lineData);

        // 3. X 轴配置
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xValues));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(30f); // 旋转标签
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(xValues.size(), false); // 确保所有标签显示

        lineChart.animateX(1500); // 添加动画
        lineChart.invalidate(); // 刷新图表
    }
}