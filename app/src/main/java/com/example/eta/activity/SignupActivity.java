package com.example.eta.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eta.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etNickname, etEmail, etPassword;
    private Button btnSignup, btnSelectImage;
    private ImageView ivProfile;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private DatabaseReference databaseReference;

    private Uri selectedImageUri = null;

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
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        btnSelectImage.setOnClickListener(v -> openImagePicker());
        btnSignup.setOnClickListener(v -> signupUser());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ivProfile.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void signupUser() {
        String nickname = etNickname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nickname.isEmpty() || email.isEmpty() || password.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "입력 정보를 확인해주세요. 비밀번호는 6자 이상입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (selectedImageUri != null) {
                                uploadProfileImage(user, nickname);
                            } else {
                                updateUserProfileAndDatabase(user, nickname, null);
                            }
                        }
                    } else {
                        Toast.makeText(SignupActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        setLoading(false);
                    }
                });
    }

    private void uploadProfileImage(FirebaseUser user, String nickname) {
        StorageReference profileImageRef = storage.getReference().child("profile_images/" + user.getUid());

        profileImageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    updateUserProfileAndDatabase(user, nickname, imageUrl);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "사진 업로드 실패. 정보만 저장됩니다.", Toast.LENGTH_SHORT).show();
                    updateUserProfileAndDatabase(user, nickname, null);
                });
    }

    private void updateUserProfileAndDatabase(FirebaseUser user, String nickname, @Nullable String imageUrl) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(nickname)
                .setPhotoUri(imageUrl != null ? Uri.parse(imageUrl) : null)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            // Realtime Database에도 정보 저장
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", user.getUid());
            userData.put("email", user.getEmail());
            userData.put("nickname", nickname);
            if (imageUrl != null) {
                userData.put("profileImageUrl", imageUrl);
            }

            databaseReference.child(user.getUid()).setValue(userData)
                    .addOnCompleteListener(dbTask -> {
                        setLoading(false);
                        if (dbTask.isSuccessful()) {
                            Toast.makeText(this, "회원가입이 완료되었습니다!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "데이터베이스 저장 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!isLoading);
        btnSelectImage.setEnabled(!isLoading);
    }
}
