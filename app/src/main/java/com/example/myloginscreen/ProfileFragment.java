package com.example.myloginscreen;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1; // Código de solicitud para el selector de imágenes
    private ImageView profileImageView; // ImageView para el logo
    private FirebaseAuth auth;
    private StorageReference storageReference;
    private TextView profileEmailTextView; // Declarar el TextView para el correo


    // Campos para cambiar la contraseña
    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Inicializar Firebase Auth y Storage
        auth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("profile_images/" + auth.getCurrentUser().getUid() + ".jpg");

        // Inicializar la vista del logo de perfil
        profileImageView = view.findViewById(R.id.profile_image);
        loadProfileImage(); // Cargar la imagen de perfil desde Firebase Storage

        // Inicializar el TextView para el correo
        profileEmailTextView = view.findViewById(R.id.profile_email); // Asegúrate de que este ID coincida con el de tu XML

        // Cargar el correo electrónico del usuario
        loadUserEmail();

        // Configurar el click listener para cambiar la imagen
        profileImageView.setOnClickListener(v -> openImageChooser());

        // Inicializar campos para cambiar la contraseña
        currentPasswordEditText = view.findViewById(R.id.current_password);
        newPasswordEditText = view.findViewById(R.id.new_password);
        confirmPasswordEditText = view.findViewById(R.id.confirm_new_password);

        // Botón para cambiar la contraseña
        Button changePasswordButton = view.findViewById(R.id.change_password_button);
        changePasswordButton.setOnClickListener(v -> changePassword());

        return view;
    }

    private void loadUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            profileEmailTextView.setText(email); // Establecer el texto del TextView al correo del usuario
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*"); // Selecciona cualquier tipo de imagen
        intent.setAction(Intent.ACTION_GET_CONTENT); // Acción para obtener contenido
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST); // Inicia el selector de imágenes
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageToFirebase(imageUri); // Subir la nueva imagen a Firebase Storage
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        try {
            // Cargar y redimensionar la imagen
            InputStream imageStream = getActivity().getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(imageStream);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 200, 200, false); // Redimensionar a 200x200

            // Convertir el Bitmap a ByteArray
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            // Subir la imagen a Firebase Storage
            UploadTask uploadTask = storageReference.putBytes(data);

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Cargar la imagen en el ImageView usando Glide
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    Glide.with(getActivity())
                            .load(uri)
                            .circleCrop() // Asegura que la imagen cargada sea circular
                            .into(profileImageView); // Aplica la imagen al ImageView

                    Toast.makeText(getActivity(), "Profile image updated", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(getActivity(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Image not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage() {
        // Cargar la imagen de Firebase Storage usando Glide
        storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri)
                    .circleCrop() // Asegura que la imagen cargada sea circular
                    .into(profileImageView);
        });
    }

    private void changePassword() {
        String currentPassword = currentPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Verificar si los campos están vacíos
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si la nueva contraseña y la confirmación son iguales
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getActivity(), "New password and confirmation do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el usuario actual
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Re-autenticar al usuario con la contraseña actual
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Cambiar la contraseña
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(getActivity(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Failed to change password: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), "Re-authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
