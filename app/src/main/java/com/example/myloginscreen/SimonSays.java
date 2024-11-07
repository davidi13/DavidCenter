package com.example.myloginscreen;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Chronometer;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class SimonSays extends AppCompatActivity {

    private Button buttonRed, buttonBlue, buttonGreen, buttonYellow, buttonStart;
    private ImageButton backButton;
    private SoundPool soundPool;
    private int playSoundId, redSoundId, blueSoundId, greenSoundId, yellowSoundId, loseSoundId;
    private List<Integer> sequence = new ArrayList<>();
    private int userInputIndex = 0;
    private int round = 0;
    private boolean gameActive = false;
    private Chronometer chronometer;
    private TextView roundCounter;

    // Variables para Firestore
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onPause() {
        super.onPause();
        if (soundPool != null) {
            soundPool.autoPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        soundPool.autoResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simon_says);

        // Inicializar Firestore y obtener el ID del usuario
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Inicializa los botones
        buttonRed = findViewById(R.id.button_red);
        buttonBlue = findViewById(R.id.button_blue);
        buttonGreen = findViewById(R.id.button_green);
        buttonYellow = findViewById(R.id.button_yellow);
        buttonStart = findViewById(R.id.button_start);
        backButton = findViewById(R.id.button_back);

        // Inicializa el Chronometer y el contador de rondas
        chronometer = findViewById(R.id.chronometer);
        roundCounter = findViewById(R.id.round_counter);

        // Configura SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build();

        // Cargar los sonidos
        playSoundId = soundPool.load(this, R.raw.inicio, 1);
        redSoundId = soundPool.load(this, R.raw.red, 1);
        blueSoundId = soundPool.load(this, R.raw.blue, 1);
        greenSoundId = soundPool.load(this, R.raw.green, 1);
        yellowSoundId = soundPool.load(this, R.raw.yellow, 1);
        loseSoundId = soundPool.load(this, R.raw.end, 1);

        // Configura el botón de inicio
        buttonStart.setOnClickListener(v -> startGame());

        // Configura los botones de colores
        buttonRed.setOnClickListener(v -> {
            illuminateButton(0);
            handleUserInput(0);
            playSound(0);
        });
        buttonBlue.setOnClickListener(v -> {
            illuminateButton(1);
            handleUserInput(1);
            playSound(1);
        });
        buttonGreen.setOnClickListener(v -> {
            illuminateButton(2);
            handleUserInput(2);
            playSound(2);
        });
        buttonYellow.setOnClickListener(v -> {
            illuminateButton(3);
            handleUserInput(3);
            playSound(3);
        });

        // Configura el botón de ir hacia atrás
        backButton.setOnClickListener(v -> {
            if (soundPool != null) {
                soundPool.autoPause();
            }
            finish();
        });
    }

    private void startGame() {
        soundPool.play(playSoundId, 1, 1, 0, 0, 1);
        sequence.clear();
        userInputIndex = 0;
        round = 0;
        gameActive = true;
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        roundCounter.setText("Rondas: 0");
        nextRound();
    }

    private void nextRound() {
        userInputIndex = 0;
        round++;
        roundCounter.setText("Rondas: " + round);
        sequence.add(new Random().nextInt(4));

        // Agrega un retraso de medio segundo antes de mostrar la secuencia
        new Handler().postDelayed(this::showSequence, 500);
    }

    private boolean isPlayingSequence = false;

    private void showSequence() {
        isPlayingSequence = true;
        Handler handler = new Handler();
        for (int i = 0; i < sequence.size(); i++) {
            int colorIndex = sequence.get(i);
            handler.postDelayed(() -> {
                if (gameActive) {
                    illuminateButton(colorIndex);
                    playSound(colorIndex);
                }
            }, i * 1000);
        }
        handler.postDelayed(() -> isPlayingSequence = false, sequence.size() * 1000);
    }

    private void illuminateButton(int colorIndex) {
        final Button button;
        switch (colorIndex) {
            case 0:
                button = buttonRed;
                button.setBackgroundResource(R.drawable.button_red_highlight);
                break;
            case 1:
                button = buttonBlue;
                button.setBackgroundResource(R.drawable.button_blue_highlight);
                break;
            case 2:
                button = buttonGreen;
                button.setBackgroundResource(R.drawable.button_green_highlight);
                break;
            case 3:
                button = buttonYellow;
                button.setBackgroundResource(R.drawable.button_yellow_highlight);
                break;
            default:
                return;
        }
        button.postDelayed(() -> {
            switch (colorIndex) {
                case 0: button.setBackgroundResource(R.drawable.button_red_normal); break;
                case 1: button.setBackgroundResource(R.drawable.button_blue_normal); break;
                case 2: button.setBackgroundResource(R.drawable.button_green_normal); break;
                case 3: button.setBackgroundResource(R.drawable.button_yellow_normal); break;
            }
        }, 500);
    }

    private void handleUserInput(int colorIndex) {
        if (!gameActive) return;

        if (colorIndex == sequence.get(userInputIndex)) {
            userInputIndex++;
            if (userInputIndex == sequence.size()) {
                nextRound();
            }
        } else {
            gameActive = false;
            soundPool.autoPause();
            soundPool.play(loseSoundId, 1, 1, 0, 0, 1);
            chronometer.stop();

            long elapsedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
            saveBestScore(round, elapsedTime);

            Toast.makeText(this, "¡You lost! Rounds played: " + round, Toast.LENGTH_SHORT).show();
        }
    }

    private void playSound(int colorIndex) {
        switch (colorIndex) {
            case 0: soundPool.play(redSoundId, 1, 1, 0, 0, 1); break;
            case 1: soundPool.play(blueSoundId, 1, 1, 0, 0, 1); break;
            case 2: soundPool.play(greenSoundId, 1, 1, 0, 0, 1); break;
            case 3: soundPool.play(yellowSoundId, 1, 1, 0, 0, 1); break;
        }
    }

    // Método para guardar el mejor puntaje en la subcolección 'scores2'
    private void saveBestScore(int rounds, long elapsedTime) {
        if (userId == null) return;

        DocumentReference scoreRef = db.collection("users").document(userId).collection("scores2").document("simon_says_score");

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("rounds", rounds);
        scoreData.put("time", elapsedTime);
        scoreData.put("date", date);

        scoreRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                int bestRounds = document.getLong("rounds").intValue();
                if (rounds > bestRounds) {
                    scoreRef.set(scoreData);
                }
            } else {
                scoreRef.set(scoreData);
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error al guardar puntaje: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
