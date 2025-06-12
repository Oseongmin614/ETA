package com.example.eta.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eta.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignupActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etNickname, etEmail, etPassword;
    private Button btnSignup, btnSelectImage;
    private ImageView ivProfile;
    private Uri selectedImageUri = null;

    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etNickname = findViewById(R.id.et_nickname);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignup = findViewById(R.id.btn_signup);
        btnSelectImage = findViewById(R.id.btn_select_image);
        ivProfile = findViewById(R.id.iv_profile);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();

        btnSelectImage.setOnClickListener(v -> openImagePicker());
        btnSignup.setOnClickListener(v -> signupUser());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "프로필 사진 선택"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ivProfile.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void signupUser() {
        String nickname = etNickname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nickname.isEmpty()) {
            etNickname.setError("닉네임을 입력하세요.");
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("이메일을 입력하세요.");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("비밀번호를 입력하세요.");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("비밀번호는 6자 이상이어야 합니다.");
            return;
        }

        // 1. Firebase Auth로 회원가입
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (selectedImageUri != null) {
                            uploadProfileImageAndSaveUser(user, nickname, selectedImageUri);
                        } else {
                            saveUserToDatabase(user, nickname, null);
                        }
                    } else {
                        Toast.makeText(this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadProfileImageAndSaveUser(FirebaseUser user, String nickname, Uri imageUri) {
        // 고유 파일명 생성
        String fileName = "profile_images/" + user.getUid() + "_" + UUID.randomUUID().toString();

        StorageReference ref = storage.getReference().child(fileName);
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    saveUserToDatabase(user, nickname, imageUrl);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "프로필 사진 업로드 실패", Toast.LENGTH_SHORT).show();
                    saveUserToDatabase(user, nickname, null); // 사진 없이 저장
                });
    }

    private void saveUserToDatabase(FirebaseUser user, String nickname, String imageUrl) {
        // 사용자 정보 저장
        Map<String, Object> userData = new HashMap<>();
        userData.put("nickname", nickname);
        userData.put("email", user.getEmail());
        if (imageUrl != null) userData.put("profileImageUrl", imageUrl);

        database.getReference("users").child(user.getUid())
                .setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "회원가입 완료!", Toast.LENGTH_SHORT).show();
                        // 메인화면 등으로 이동
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "DB 저장 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
