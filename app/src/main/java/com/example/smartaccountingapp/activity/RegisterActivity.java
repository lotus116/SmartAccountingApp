package com.example.smartaccountingapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartaccountingapp.R;
import com.example.smartaccountingapp.util.UserDBHelper; // 【新增】导入 UserDBHelper

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername;
    private EditText etPassword;
    private Button btnRegister;

    private UserDBHelper userDbHelper; // 【新增】用户数据库 helper

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 【修正】初始化 UserDBHelper
        userDbHelper = new UserDBHelper(this);

        etUsername = findViewById(R.id.et_username_reg);
        etPassword = findViewById(R.id.et_password_reg);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> handleRegister());
    }

    private void handleRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 【核心修正】使用 UserDBHelper 进行注册
        if (userDbHelper.registerUser(username, password)) {
            Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_LONG).show();

            // 注册成功后跳转回登录页面
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();

        } else {
            // 失败的原因通常是用户名已存在
            Toast.makeText(this, "注册失败：用户名已存在或数据库错误", Toast.LENGTH_LONG).show();
        }
    }
}