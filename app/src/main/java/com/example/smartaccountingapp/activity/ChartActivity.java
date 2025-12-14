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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class ChartActivity extends AppCompatActivity {
    private PieChart pieChart;
    private LineChart lineChart;
    private DBHelper dbHelper;
    private TextView tvDateRange, tvTotalExpense, tvTotalIncome;
    private Spinner spinnerTimePreset;

    private Calendar calendarStart, calendarEnd;
    private SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat sdfUi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final int[] EXPENSE_COLORS = {
            Color.rgb(255, 99, 132), Color.rgb(54, 162, 235), Color.rgb(255, 206, 86),
            Color.rgb(75, 192, 192), Color.rgb(153, 102, 255), Color.rgb(201, 203, 207)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        dbHelper = new DBHelper(this);
        pieChart = findViewById(R.id.pie_chart);
        lineChart = findViewById(R.id.line_chart);
        tvDateRange = findViewById(R.id.tv_date_range);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvTotalIncome = findViewById(R.id.tv_total_income);
        spinnerTimePreset = findViewById(R.id.spinner_time_preset);
        ImageView ivSelectDate = findViewById(R.id.iv_select_date);

        calendarStart = Calendar.getInstance();
        calendarEnd = Calendar.getInstance();

        // 初始化为当前月份
        setRangeToCurrentMonth();

        // Spinner 监听器
        spinnerTimePreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // 当前月份
                        setRangeToCurrentMonth();
                        break;
                    case 1: // 过去 30 天
                        setRangeToLast30Days();
                        break;
                    case 2: // 本年
                        setRangeToCurrentYear();
                        break;
                    case 3: // 所有时间
                        setRangeToAllTime();
                        break;
                }
                loadAllCharts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 日历选择监听器
        ivSelectDate.setOnClickListener(v -> showDateRangePickerDialog());

        // 第一次由 Spinner 监听器触发
    }

    // 设置日期范围到当前月份
    private void setRangeToCurrentMonth() {
        calendarStart.setTime(new Date());
        calendarStart.set(Calendar.DAY_OF_MONTH, 1); // 月初

        calendarEnd.setTime(new Date());
        calendarEnd.set(Calendar.DAY_OF_MONTH, calendarEnd.getActualMaximum(Calendar.DAY_OF_MONTH)); // 月末
    }

    // 设置日期范围到过去 30 天
    private void setRangeToLast30Days() {
        calendarEnd.setTime(new Date());
        calendarStart.setTime(new Date());
        calendarStart.add(Calendar.DAY_OF_MONTH, -29);
    }

    // 设置日期范围到本年
    private void setRangeToCurrentYear() {
        calendarStart.setTime(new Date());
        calendarStart.set(Calendar.DAY_OF_YEAR, 1); // 年初

        calendarEnd.setTime(new Date());
        calendarEnd.set(Calendar.MONTH, 11);
        calendarEnd.set(Calendar.DAY_OF_MONTH, calendarEnd.getActualMaximum(Calendar.DAY_OF_MONTH)); // 年末
    }

    // 设置日期范围到所有时间
    private void setRangeToAllTime() {
        // 由于 SQLite 的日期比较，这里用一个极早的日期
        calendarStart.set(2000, 0, 1);
        calendarEnd.setTime(new Date());
    }

    // 显示日期范围选择对话框
    private void showDateRangePickerDialog() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendarStart.set(year, month, dayOfMonth);
            showEndDateDialog();
        }, calendarStart.get(Calendar.YEAR), calendarStart.get(Calendar.MONTH), calendarStart.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showEndDateDialog() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendarEnd.set(year, month, dayOfMonth);

            // 确保结束日期不早于开始日期
            if (calendarEnd.getTimeInMillis() < calendarStart.getTimeInMillis()) {
                Toast.makeText(ChartActivity.this, "结束日期不能早于开始日期", Toast.LENGTH_SHORT).show();
                calendarEnd.setTime(calendarStart.getTime()); // 重设为开始日期
            }
            // 这里将 Spinner 选中位置设置为 3 (所有时间)，表示用户选择了自定义范围
            // 如果您在 strings.xml 中添加了“自定义”，则应选中自定义
            spinnerTimePreset.setSelection(3);
            loadAllCharts();
        }, calendarEnd.get(Calendar.YEAR), calendarEnd.get(Calendar.MONTH), calendarEnd.get(Calendar.DAY_OF_MONTH)).show();
    }

    // -------------------
    // 图表加载主控
    // -------------------
    private void loadAllCharts() {
        String startDate = sdfDb.format(calendarStart.getTime());
        String endDate = sdfDb.format(calendarEnd.getTime());

        String uiRange = sdfUi.format(calendarStart.getTime()) + " 至 " + sdfUi.format(calendarEnd.getTime());
        tvDateRange.setText(uiRange);

        // 1. 加载概览数据 (此方法内部通过 loadTotalExpense 间接加载了饼图)
        loadOverviewData(startDate, endDate);

        // 2. 加载折线图
        loadLineChartData(startDate, endDate);
    }

    // -------------------
    // 概览数据加载
    // -------------------
    private void loadOverviewData(String startDate, String endDate) {
        // 总收入
        double totalIncome = dbHelper.getTotalIncomeByRange(startDate, endDate);
        tvTotalIncome.setText(String.format(Locale.getDefault(), "总收入: +%.2f", totalIncome));

        // 总支出 (从饼图逻辑中获取)
        double totalExpense = loadTotalExpense(startDate, endDate);
        tvTotalExpense.setText(String.format(Locale.getDefault(), "总支出: -%.2f", totalExpense));
    }

    // -------------------
    // 饼图数据加载
    // -------------------
    private double loadTotalExpense(String startDate, String endDate) {
        Cursor cursor = dbHelper.getExpenseSummaryByRange(startDate, endDate);

        ArrayList<PieEntry> entries = new ArrayList<>();
        float totalExpense = 0;

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CATEGORY));
                float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("total_amount"));
                entries.add(new PieEntry(amount, category));
                totalExpense += amount;
            } while (cursor.moveToNext());
        }
        cursor.close();

        // 设置饼图数据
        setupPieChart(totalExpense, entries);
        return totalExpense;
    }

    private void setupPieChart(float totalExpense, ArrayList<PieEntry> entries) {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawEntryLabels(false);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        // 【已修正】将 Legend.HorizontalAlignment 修正为 Legend.LegendHorizontalAlignment
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("所选时间段暂无支出数据");
            pieChart.setCenterText("总支出\n0.00");
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(EXPENSE_COLORS);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(15f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);
        pieChart.invalidate();

        // 中间文本显示总支出
        pieChart.setCenterText("总支出\n" + String.format(Locale.getDefault(), "%.2f", totalExpense));
    }

    // -------------------
    // 折线图数据加载
    // -------------------
    private void loadLineChartData(String startDate, String endDate) {
        Cursor cursor = dbHelper.getTrendDataByRange(startDate, endDate);

        ArrayList<Entry> incomeEntries = new ArrayList<>();
        ArrayList<Entry> expenseEntries = new ArrayList<>();
        List<String> xValues = new ArrayList<>();

        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                String timeKey = cursor.getString(cursor.getColumnIndexOrThrow("time_key"));
                float income = cursor.getFloat(cursor.getColumnIndexOrThrow("total_income"));
                float expense = cursor.getFloat(cursor.getColumnIndexOrThrow("total_expense"));

                incomeEntries.add(new Entry(i, income));
                expenseEntries.add(new Entry(i, expense));
                xValues.add(timeKey);
                i++;
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (incomeEntries.isEmpty() && expenseEntries.isEmpty()) {
            lineChart.setNoDataText("所选时间段暂无收支趋势数据");
            return;
        }

        // 1. 设置数据集
        LineDataSet incomeSet = new LineDataSet(incomeEntries, "收入");
        incomeSet.setColor(Color.rgb(50, 205, 50)); // 亮绿色
        incomeSet.setCircleColor(Color.rgb(50, 205, 50));
        incomeSet.setLineWidth(2f);
        incomeSet.setCircleRadius(4f);
        incomeSet.setDrawValues(false);

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "支出");
        expenseSet.setColor(Color.rgb(255, 69, 0)); // 橘红色
        expenseSet.setCircleColor(Color.rgb(255, 69, 0));
        expenseSet.setLineWidth(2f);
        expenseSet.setCircleRadius(4f);
        expenseSet.setDrawValues(false);

        LineData lineData = new LineData(incomeSet, expenseSet);
        lineChart.setData(lineData);

        // 2. X 轴配置
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xValues));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(30f); // 旋转标签，避免重叠

        // 3. Y 轴配置
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisRight().setEnabled(false);

        // 4. 图例配置
        Legend legend = lineChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);

        lineChart.getDescription().setEnabled(false);
        lineChart.invalidate();
    }
}