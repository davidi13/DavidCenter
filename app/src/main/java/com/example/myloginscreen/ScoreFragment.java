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
        // Cargar los puntajes de '2048' desde la colección 'scores'
        db.collection("users").document(userId).collection("scores")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        displayScore(document, "2048");
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace());

        // Cargar los puntajes de 'Simon Says' desde la colección 'scores2'
        db.collection("users").document(userId).collection("scores2")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        displayScore(document, "Simon Says");
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace());

        // Cargar los puntajes de 'Lights Outside' desde la colección 'score3'
        db.collection("users").document(userId).collection("score3")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        displayScore(document, "Lights Outside");
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

        // Verifica si el documento contiene datos de puntaje, fecha y tiempo
        if (document.exists()) {
            if (gameTitle.equals("2048")) {
                // Mostrar puntaje para 2048
                scoreView.setText(String.valueOf(document.getLong("score")));
            } else if (gameTitle.equals("Simon Says")) {
                // Mostrar rondas para Simon Says
                scoreView.setText(String.valueOf(document.getLong("rounds")));
            } else if (gameTitle.equals("Lights Outside")) {
                // Mostrar rondas para Lights Outside
                scoreView.setText(String.valueOf(document.getLong("rounds")));
            }

            // Formatear la fecha para mostrar solo la fecha sin la hora
            String fullDate = document.getString("date");
            if (fullDate != null && fullDate.contains(" ")) {
                String dateOnly = fullDate.split(" ")[0];
                dateView.setText(dateOnly);
            } else {
                dateView.setText(fullDate != null ? fullDate : "N/A");
            }

            // Leer el tiempo directamente en milisegundos y convertir a segundos
            Long timeInMillis = document.getLong("time");
            if (timeInMillis != null) {
                long timeInSeconds = timeInMillis / 1000;
                timeView.setText(timeInSeconds + " s");
            } else {
                timeView.setText("N/A");
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
        }

        // Agrega la vista del cuadro al contenedor principal
        scoreContainer.addView(scoreItem);
    }
}
