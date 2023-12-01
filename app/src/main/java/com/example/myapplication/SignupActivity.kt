package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import org.w3c.dom.Text

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val signup = findViewById<Button>(R.id.signup)
        val email = findViewById<TextView>(R.id.email)
        val password = findViewById<TextView>(R.id.password)
        val nickname = findViewById<TextView>(R.id.nickname)

        signup.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val nicknameText = nickname.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty() || nicknameText.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호, 닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
            } else {
                createUser(emailText, passwordText, nicknameText)
            }
        }

        val cancelButton = findViewById<Button>(R.id.cancel)
        cancelButton.setOnClickListener {
            // LoginActivity로 이동하기 위한 인텐트 생성
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun createUser(emailText: String, passwordText: String, nickname: String) {
        auth.createUserWithEmailAndPassword(emailText, passwordText)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid ?: ""

                    val userData = hashMapOf(
                        "nickname" to nickname
                    )

                    firestore.collection("users")
                        .document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, HomeActivity::class.java)
                            intent.putExtra("NICKNAME", nickname)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "회원가입 실패: 닉네임 저장 실패", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
