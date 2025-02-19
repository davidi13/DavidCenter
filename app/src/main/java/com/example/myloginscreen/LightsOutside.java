package com.example.myloginscreen;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LightsOutside extends AppCompatActivity {

    private GridLayout gridLayout;
    private Button playButton;
    private ImageButton backButton;
    private TextView roundCounter;
    private TextView timer; // TextView para el cronómetro
    private List<Integer> lightedPositions;
    private int currentRound;
    private Handler handler;
    private boolean gameOver;
    private Runnable timerRunnable;
    private long elapsedTime; // Variable para el tiempo transcurrido en milisegundos

    // Para SoundPool
    private SoundPool soundPool;
    private int soundStart;
    private int soundLose;
    private int soundButtonClick;
    private int soundNextRound;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lights_outside);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser().getUid();

        gridLayout = findViewById(R.id.grid_layout);
        playButton = findViewById(R.id.play_button);
        backButton = findViewById(R.id.back_button);
        roundCounter = findViewById(R.id.round_counter);
        timer = findViewById(R.id.chronometer); // Inicializar TextView para el cronómetro
        handler = new Handler();
        lightedPositions = new ArrayList<>();

        // Inicializar SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build();

        soundStart = soundPool.load(this, R.raw.inicio, 1);
        soundLose = soundPool.load(this, R.raw.end, 1);
        soundButtonClick = soundPool.load(this, R.raw.pop, 1);
        soundNextRound = soundPool.load(this, R.raw.lvl_up, 1);

        initializeGrid();
        enableGridButtons(false);

        playButton.setOnClickListener(v -> startGame());

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(LightsOutside.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void initializeGrid() {
        for (int i = 0; i < 25; i++) {
            final int position = i;
            Button button = new Button(this);
            button.setBackgroundColor(Color.LTGRAY);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 130;
            params.height = 130;
            params.setMargins(5, 5, 5, 5);

            button.setLayoutParams(params);
            button.setOnClickListener(v -> {
                soundPool.play(soundButtonClick, 1, 1, 0, 0, 1);
                onButtonClick(position, button);
            });
            gridLayout.addView(button);
        }
    }

    private void startGame() {
        stopTimer(); // Detener el cronómetro anterior antes de iniciar uno nuevo

        currentRound = 1;
        lightedPositions.clear();
        gameOver = false;
        elapsedTime = 0; // Reiniciar el tiempo a 0
        roundCounter.setText("Rounds: " + currentRound);
        timer.setText(formatTime(elapsedTime)); // Mostrar el tiempo inicial
        playButton.setText("Restart");
        enableGridButtons(true);
        showRandomLights();
        soundPool.play(soundStart, 1, 1, 0, 0, 1);
        startTimer(); // Iniciar el cronómetro
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                elapsedTime += 100; // Aumentar el tiempo en 100 milisegundos (0.1 segundos)
                timer.setText(formatTime(elapsedTime)); // Actualizar el texto del cronómetro
                handler.postDelayed(this, 100); // Ejecutar cada 100 milisegundos
            }
        };
        handler.postDelayed(timerRunnable, 100);
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable); // Detener el cronómetro
    }

    private String formatTime(long timeInMillis) {
        int minutes = (int) (timeInMillis / 60000);
        int seconds = (int) ((timeInMillis % 60000) / 1000);
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void showRandomLights() {
        int numLights = 5 + currentRound - 1;
        lightedPositions.clear();
        List<Integer> allPositions = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            allPositions.add(i);
        }
        Collections.shuffle(allPositions);
        lightedPositions.addAll(allPositions.subList(0, numLights));

        enableGridButtons(false);

        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            Button button = (Button) gridLayout.getChildAt(i);
            if (lightedPositions.contains(i)) {
                button.setBackgroundColor(getResources().getColor(R.color.blue));
            } else {
                button.setBackgroundColor(Color.LTGRAY);
            }
        }

        handler.postDelayed(() -> {
            for (int i = 0; i < gridLayout.getChildCount(); i++) {
                Button button = (Button) gridLayout.getChildAt(i);
                button.setBackgroundColor(Color.LTGRAY);
            }
            enableGridButtons(true);
            roundCounter.setText("Rounds: " + currentRound);
        }, 1000);
    }

    private void onButtonClick(int position, Button button) {
        if (gameOver) return;

        if (lightedPositions.contains(position)) {
            if (button.getCurrentTextColor() == getResources().getColor(R.color.blue2)) {
                return;
            }

            button.setBackgroundColor(getResources().getColor(R.color.blue2));
            lightedPositions.remove((Integer) position);

            if (lightedPositions.isEmpty()) {
                currentRound++;
                roundCounter.setText("Rounds: " + currentRound);
                soundPool.play(soundNextRound, 1, 1, 0, 0, 1);

                handler.postDelayed(this::showRandomLights, 500);
            }
        } else {
            Toast.makeText(this, "¡You lost! Rounds played: " + currentRound, Toast.LENGTH_SHORT).show();
            soundPool.play(soundLose, 1, 1, 0, 0, 1);
            gameOver = true;

            for (int i = 0; i < gridLayout.getChildCount(); i++) {
                Button btn = (Button) gridLayout.getChildAt(i);
                if (lightedPositions.contains(i)) {
                    btn.setBackgroundColor(Color.RED);
                }
            }

            enableGridButtons(false);
            playButton.setText("Play");
            stopTimer(); // Detener el cronómetro al perder
            saveBestScore(currentRound, elapsedTime); // Guardar el puntaje en Firestore
        }
    }

    // Método para guardar todas las partidas en la subcolección 'score3'
    private void saveBestScore(int rounds, long elapsedTime) {
        if (userId == null) return;

        // Crear un nuevo documento para cada partida con un ID único
        DocumentReference scoreRef = db.collection("users").document(userId)
                .collection("score3").document();

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("rounds", rounds);
        scoreData.put("time", elapsedTime);
        scoreData.put("date", date);

        scoreRef.set(scoreData)
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar puntaje: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }



    private void enableGridButtons(boolean enabled) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            Button button = (Button) gridLayout.getChildAt(i);
            button.setEnabled(enabled);
        }
    }
}
