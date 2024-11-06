package com.example.myloginscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ScoreFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private LinearLayout scoreContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_score, container, false);
        scoreContainer = view.findViewById(R.id.score_container);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadScores();

        return view;
    }

    private void loadScores() {
        // Accede a la colección 'scores' del usuario autenticado
        db.collection("users").document(userId).collection("scores")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        // Para cada documento en 'scores', llama a displayScore para mostrarlo
                        displayScore(document);
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace());
    }

    private void displayScore(DocumentSnapshot document) {
        // Inflar el layout del cuadro de puntaje
        View scoreItem = getLayoutInflater().inflate(R.layout.score_item, scoreContainer, false);

        // Configurar el título del juego basado en el nombre del documento
        String gameTitle = document.getId(); // Usar el ID del documento como título
        TextView gameTitleView = scoreItem.findViewById(R.id.game_title);
        gameTitleView.setText(gameTitle);

        // Configurar los valores de puntaje, fecha y tiempo
        TextView scoreView = scoreItem.findViewById(R.id.score_value);
        TextView dateView = scoreItem.findViewById(R.id.date_value);
        TextView timeView = scoreItem.findViewById(R.id.time_value);

        // Verifica si el documento contiene datos de puntaje, fecha y tiempo
        if (document.exists()) {
            scoreView.setText(String.valueOf(document.getLong("score")));
            dateView.setText(document.getString("date"));
            timeView.setText(String.valueOf(document.getLong("time")) + " ms");
        } else {
            scoreView.setText("N/A");
            dateView.setText("N/A");
            timeView.setText("N/A");
        }

        // Agrega la vista del cuadro al contenedor principal
        scoreContainer.addView(scoreItem);
    }
}

