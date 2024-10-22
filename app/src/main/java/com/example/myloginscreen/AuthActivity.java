package com.example.myloginscreen;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {

    private Button loginButton;
    private Button registerButton;
    private EditText username, password; // Declare username and password

    // ProviderType enum declaration
    public enum ProviderType {
        BASIC
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);

        // Verificar si el usuario ya está autenticado
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Si hay un usuario autenticado, redirigir a HomeActivity
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            showHome(email, ProviderType.BASIC);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setTitle("Login");

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.RegisterButton);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!username.getText().toString().isEmpty() && !password.getText().toString().isEmpty()) {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(username.getText().toString(),
                            password.getText().toString()).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String email = task.getResult().getUser() != null ? task.getResult().getUser().getEmail() : "";
                            showHome(email, ProviderType.BASIC);
                        } else {
                            showAlert();
                        }
                    });
                } else {
                    Toast.makeText(AuthActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!username.getText().toString().isEmpty() && !password.getText().toString().isEmpty()) {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(username.getText().toString(),
                            password.getText().toString()).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String email = task.getResult().getUser() != null ? task.getResult().getUser().getEmail() : "";
                            showHome(email, ProviderType.BASIC);
                        } else {
                            showAlert();
                        }
                    });
                } else {
                    Toast.makeText(AuthActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void showAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage("Se ha producido un error autenticando el usuario");
        builder.setPositiveButton("Aceptar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showHome(String email, ProviderType provider) {
        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.putExtra("email", email);
        homeIntent.putExtra("provider", provider.name());
        startActivity(homeIntent);
    }
}
