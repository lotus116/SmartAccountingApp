package com.example.smartaccountingapp.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    // 图片相关控件和变量
    private Button btnSelectImage;
    private Button btnTakePhoto;
    private ImageView ivPreview;
    private String currentImagePath = null;

    private boolean isEditMode = false;
    private Account accountToEdit;
    private String currentUserId;

    // 请求码常量
    private static final int REQUEST_CODE_CAMERA = 3;
    private static final int REQUEST_CODE_GALLERY = 4;
    private static final int PERMISSION_GALLERY_CODE = 101; // 相册权限码
    private static final int PERMISSION_CAMERA_CODE = 102;  // 相机权限码

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        currentUserId = PrefsManager.getCurrentUserId(this);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "用户未登录，无法记账", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        dbHelper = new DBHelper(this);

        initViews();
        setupSpinners();

        // 【已移除】: 移除了 onCreate 中调用 checkPermissions() 的代码，实现按需请求。

        // 检查是否为编辑模式
        Intent intent = getIntent();
        if (intent.getBooleanExtra("is_edit", false) && intent.getSerializableExtra("account") != null) {
            isEditMode = true;
            accountToEdit = (Account) intent.getSerializableExtra("account");
            loadAccountData(accountToEdit);
        }

        btnSave.setOnClickListener(v -> saveAccount());
        etDate.setOnClickListener(v -> showDatePickerDialog());

        // 【核心修改】点击事件触发时，再进行权限检查
        btnSelectImage.setOnClickListener(v -> checkAndOpenGallery());
        btnTakePhoto.setOnClickListener(v -> checkAndOpenCamera());
    }

    // --- 权限检查和启动逻辑 ---

    /**
     * 【新增】检查相册权限，如果已授予则打开相册，否则请求权限。
     */
    private void checkAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_GALLERY_CODE);
        }
    }

    /**
     * 【新增】检查相机权限，如果已授予则打开相机，否则请求权限。
     */
    private void checkAndOpenCamera() {
        // 拍照通常需要相机和存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_CAMERA_CODE);
        }
    }


    /**
     * 【修改】处理权限请求的结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_GALLERY_CODE) {
                openGallery(); // 权限通过，启动相册
            } else if (requestCode == PERMISSION_CAMERA_CODE) {
                openCamera(); // 权限通过，启动相机
            }
        } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "您拒绝了权限，无法完成此操作", Toast.LENGTH_SHORT).show();
        }
    }


    private void initViews() {
        spinnerType = findViewById(R.id.spinner_type);
        spinnerCategory = findViewById(R.id.spinner_category);
        etAmount = findViewById(R.id.et_amount);
        etDate = findViewById(R.id.et_date);
        etNote = findViewById(R.id.et_note);
        btnSave = findViewById(R.id.btn_save);

        // 图片相关控件
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        ivPreview = findViewById(R.id.iv_attachment_preview);

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("编辑记录");
        }

        ArrayAdapter<String> typeAdapter = (ArrayAdapter<String>) spinnerType.getAdapter();
        if (typeAdapter != null) {
            int spinnerPosition = typeAdapter.getPosition(account.getType());
            spinnerType.setSelection(spinnerPosition);
        }

        ArrayAdapter<String> categoryAdapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
        if (categoryAdapter != null) {
            int spinnerPosition = categoryAdapter.getPosition(account.getCategory());
            spinnerCategory.setSelection(spinnerPosition);
        }

        etAmount.setText(String.format(Locale.getDefault(), "%.2f", account.getAmount()));
        etDate.setText(account.getDate());
        etNote.setText(account.getNote());

        // 加载图片路径和显示预览
        currentImagePath = account.getImagePath();
        if (currentImagePath != null && !currentImagePath.isEmpty()) {
            displayImagePreview(Uri.parse(currentImagePath));
        }

        btnSave.setText("保存修改");
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
            etDate.setText(dateString);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * 打开相册 (Gallery) - 只有在权限通过时才调用
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    /**
     * 打开相机 (Camera) - 只有在权限通过时才调用
     */
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_CAMERA);
        } else {
            Toast.makeText(this, "无法打开相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示图片预览并记录路径 (URI)
     */
    private void displayImagePreview(Uri uri) {
        ivPreview.setImageURI(uri);
        ivPreview.setVisibility(View.VISIBLE);
        currentImagePath = uri.toString();
    }

    /**
     * 处理 Activity 返回结果 (包括相册和拍照)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_GALLERY && data != null) {
                // 处理相册选择结果
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    displayImagePreview(selectedImageUri);
                }
            } else if (requestCode == REQUEST_CODE_CAMERA) {
                // 处理拍照结果
                if (data != null && data.getData() != null) {
                    displayImagePreview(data.getData());
                } else if (data != null && data.getExtras() != null) {
                    // 通用处理：如果返回 Bitmap 缩略图，我们只记录一个占位路径
                    Toast.makeText(this, "拍照成功，图片路径已记录", Toast.LENGTH_SHORT).show();
                    currentImagePath = "camera_capture://" + System.currentTimeMillis();
                    ivPreview.setImageResource(android.R.drawable.ic_menu_camera);
                    ivPreview.setVisibility(View.VISIBLE);
                }
            }
        }
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
            accountToEdit.setImagePath(currentImagePath);

            result = dbHelper.updateAccount(accountToEdit);
            if (result > 0) {
                Toast.makeText(this, "记录更新成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "记录更新失败 (可能记录不存在)", Toast.LENGTH_SHORT).show();
            }

        } else {
            Account newAccount = new Account(0, currentUserId, type, category, amount, date, note, currentImagePath);

            result = dbHelper.addAccount(newAccount);
            if (result > 0) {
                Toast.makeText(this, "新增记录成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "新增记录失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}