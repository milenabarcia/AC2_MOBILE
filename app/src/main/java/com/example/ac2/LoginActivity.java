package com.example.ac2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    EditText edtEmail, edtSenha;
    Button btnCadastrar, btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        btnLogin = findViewById(R.id.btnLogin);

        btnCadastrar.setOnClickListener(v ->
                mAuth.createUserWithEmailAndPassword(
                        edtEmail.getText().toString(),
                        edtSenha.getText().toString()
                ).addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Toast.makeText(this, "Conta criada", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Erro: " + t.getException(), Toast.LENGTH_LONG).show();
                    }
                })
        );

        btnLogin.setOnClickListener(v ->
                mAuth.signInWithEmailAndPassword(
                        edtEmail.getText().toString(),
                        edtSenha.getText().toString()
                ).addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Toast.makeText(this, "Login OK", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Erro: " + t.getException(), Toast.LENGTH_LONG).show();
                    }
                })
        );
    }
}
