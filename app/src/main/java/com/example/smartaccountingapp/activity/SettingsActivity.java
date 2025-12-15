package com.example.smartaccountingapp.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartaccountingapp.R;

public class SettingsActivity extends AppCompatActivity {

    private ListView listView;

    // 设置项数据
    private final String[] settingsItems = new String[]{
            "账户与安全",
            "数据备份与恢复",
            "关于应用",
            "帮助与反馈",
            "版本信息"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 设置 ActionBar 标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("设置");
        }

        listView = findViewById(R.id.list_settings);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1, // 使用 Android 内建的简单布局
                settingsItems
        );

        listView.setAdapter(adapter);

        // 【核心修改】设置点击监听器，根据 position 弹出定制对话框
        listView.setOnItemClickListener((parent, view, position, id) -> {
            handleSettingsItemClick(position);
        });
    }

    /**
     * 根据点击的位置处理设置项，并显示 AlertDialog。
     * @param position 被点击的列表项索引。
     */
    private void handleSettingsItemClick(int position) {
        String title = settingsItems[position];
        String message;

        // 根据 position 定制显示内容
        switch (position) {
            case 0: // 账户与安全
                message = "账户功能已启用多用户数据隔离。\n\n如需修改密码，请联系管理员。";
                showCustomDialog(title, message);
                break;

            case 1: // 数据备份与恢复
                message = "数据备份功能位于主界面右上角菜单中。\n\n您的备份文件是专属的，其他用户无法导入。";
                showCustomDialog(title, message);
                break;

            case 2: // 关于应用
                message = "开发者: cclear116 \n\n本应用为《移动系统课程设计》期末作品。\n\n版权所有 © 2025 SmartAccountingApp";
                showCustomDialog(title, message);
                break;

            case 3: // 帮助与反馈
                message = "如有任何问题或建议，请联系开发者邮箱: cclear116@163.com。\n\n我们很乐意为您提供帮助！";
                showCustomDialog(title, message);
                break;

            case 4: // 版本信息
                message = "版本号: v1.0.0 (稳定版)\n\n发布日期: 2025年12月15日";
                showCustomDialog(title, message);
                break;

            default:
                Toast.makeText(SettingsActivity.this, "未知设置项: " + title, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * 显示通用的 AlertDialog
     * @param title 对话框标题
     * @param message 对话框内容
     */
    private void showCustomDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", (dialog, which) -> {
                    // 用户点击确定，关闭对话框
                    dialog.dismiss();
                })
                .show();
    }
}