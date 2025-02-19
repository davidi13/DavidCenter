package com.example.myloginscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
        // Cargar el historial de partidas de cada juego
        loadGameHistory("scores", "2048");
        loadGameHistory("scores2", "Simon Says");
        loadGameHistory("score3", "Lights Outside");
        loadGameHistory("score4", "Arkanoid Game");
    }

    private void loadGameHistory(String collectionName, String gameTitle) {
        db.collection("users").document(userId).collection(collectionName)
                .orderBy("date", Query.Direction.DESCENDING) // Ordenar por fecha (más recientes primero)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        displayScore(document, gameTitle);
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace());
    }

    private void displayScore(DocumentSnapshot document, String gameTitle) {
        // Inflar el layout del cuadro de puntaje
        View scoreItem = getLayoutInflater().inflate(R.layout.score_item, scoreContainer, false);

        // Configurar el título del juego basado en el parámetro gameTitle
        TextView gameTitleView = scoreItem.findViewById(R.id.game_title);
        gameTitleView.setText(gameTitle);

        // Configurar los valores de puntaje, fecha y tiempo
        TextView scoreView = scoreItem.findViewById(R.id.score_value);
        TextView dateView = scoreItem.findViewById(R.id.date_value);
        TextView timeView = scoreItem.findViewById(R.id.time_value);

        if (document.exists()) {
            if (gameTitle.equals("2048") || gameTitle.equals("Arkanoid Game")) {
                scoreView.setText("Puntaje: " + document.getLong("score"));
            } else {
                scoreView.setText("Rondas: " + document.getLong("rounds"));
            }

            // Formatear la fecha para mostrar solo la fecha sin la hora
            String fullDate = document.getString("date");
            if (fullDate != null && fullDate.contains(" ")) {
                String dateOnly = fullDate.split(" ")[0];
                dateView.setText("Fecha: " + dateOnly);
            } else {
                dateView.setText("Fecha: " + (fullDate != null ? fullDate : "N/A"));
            }

            // Convertir el tiempo de milisegundos a segundos y mostrarlo
            Long timeInMillis = document.getLong("time");
            if (timeInMillis != null) {
                long timeInSeconds = timeInMillis / 1000;
                timeView.setText("Tiempo: " + timeInSeconds + " s");
            } else {
                timeView.setText("Tiempo: N/A");
            }
        } else {
            scoreView.setText("N/A");
            dateView.setText("N/A");
            timeView.setText("N/A");
        }

        // Configurar el fondo del contenedor según el juego
        if (gameTitle.equals("2048")) {
            scoreItem.setBackgroundResource(R.drawable.second_button);
        } else if (gameTitle.equals("Simon Says")) {
            scoreItem.setBackgroundResource(R.drawable.rounded_button_background);
        } else if (gameTitle.equals("Lights Outside")) {
            scoreItem.setBackgroundResource(R.drawable.third_button);
        } else if (gameTitle.equals("Arkanoid Game")) {
            scoreItem.setBackgroundResource(R.drawable.fourth_button);
        }

        // Agregar la vista del cuadro al contenedor principal
        scoreContainer.addView(scoreItem);
    }
}
