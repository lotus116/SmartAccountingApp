package com.example.smartaccountingapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartaccountingapp.R;
import com.example.smartaccountingapp.util.PrefsManager;
import com.example.smartaccountingapp.util.UserDBHelper; // 【新增】导入 UserDBHelper

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    private UserDBHelper userDbHelper; // 【新增】用户数据库 helper

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 【新增】初始化 UserDBHelper
        userDbHelper = new UserDBHelper(this);

        // 检查是否已登录，如果已登录则直接跳转到 MainActivity
        if (PrefsManager.isLoggedIn(this)) {
            navigateToMain();
            return;
        }

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);

        btnLogin.setOnClickListener(v -> handleLogin());
        tvRegister.setOnClickListener(v -> navigateToRegister());
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 【核心修正】使用 UserDBHelper 验证登录
        if (userDbHelper.checkCredentials(username, password)) {
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();

            // 保存当前登录用户ID
            PrefsManager.setCurrentUserId(this, username);

            navigateToMain();
        } else {
            Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // 清除任务栈，防止按返回键回到登录页
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}