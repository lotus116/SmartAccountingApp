package com.example.smartaccountingapp.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartaccountingapp.R;
import com.example.smartaccountingapp.util.PrefsManager;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        PrefsManager prefsManager = new PrefsManager(this);

        etUsername = findViewById(R.id.et_username_reg);
        etPassword = findViewById(R.id.et_password_reg);
        Button btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查用户名是否已存在
            if (prefsManager.getSavedPassword(username) != null) {
                Toast.makeText(this, "用户名已存在，请直接登录", Toast.LENGTH_SHORT).show();
                return;
            }

            prefsManager.saveUser(username, password);
            Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
            finish(); // 注册成功后返回登录页
        });
    }
}