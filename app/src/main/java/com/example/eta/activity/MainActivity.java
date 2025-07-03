package com.example.eta.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.eta.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnRegister, btnLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);

        btnRegister.setOnClickListener(v -> registerUser());
        btnLogin.setOnClickListener(v -> loginUser());
    }

    // 회원가입 화면으로 이동
    private void registerUser() {
        Intent intent = new Intent(MainActivity.this, SignupActivity.class);
        startActivity(intent);
    }

    // 로그인
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("이메일과 비밀번호를 입력하세요.");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        showToast("로그인 성공!");
                        FirebaseUser user = mAuth.getCurrentUser();
                        fetchUserDataAndMove(user);
                    } else {
                        showToast("로그인 실패: " + task.getException().getMessage());
                    }
                });
    }

    // 사용자 정보 DB에서 읽어서 넘기기
    private void fetchUserDataAndMove(FirebaseUser user) {
        if (user == null) {
            showToast("유저 정보가 없습니다.");
            return;
        }

        FirebaseDatabase.getInstance().getReference("users")
                .child(user.getUid())
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        String nickname = dataSnapshot.child("nickname").getValue(String.class);
                        String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);
                        moveToAppointmentList(user, nickname, profileImageUrl);
                    } else {
                        showToast("사용자 정보가 없습니다. 회원가입을 다시 해주세요.");
                    }
                })
                .addOnFailureListener(e -> showToast("사용자 정보 불러오기 실패: " + e.getMessage()));
    }

    private void moveToAppointmentList(FirebaseUser user, String nickname, String profileImageUrl) {
        Intent intent = new Intent(this, AppointmentListActivity.class);
        intent.putExtra("userId", user.getUid());
        intent.putExtra("email", user.getEmail());
        intent.putExtra("nickname", nickname);
        intent.putExtra("profileImageUrl", profileImageUrl);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
