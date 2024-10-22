package com.example.myloginscreen;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LightsOutside extends AppCompatActivity {

    private GridLayout gridLayout;
    private Button playButton;
    private ImageButton backButton;
    private TextView roundCounter;
    private TextView timer; // Añadir TextView para el cronómetro
    private List<Integer> lightedPositions;
    private int currentRound;
    private Handler handler;
    private boolean gameOver;
    private Runnable timerRunnable;
    private int elapsedTime; // Variable para el tiempo

    // Para SoundPool
    private SoundPool soundPool;
    private int soundStart;
    private int soundLose;
    private int soundButtonClick;
    private int soundNextRound; // Sonido para el cambio de ronda

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lights_outside);

        auth = FirebaseAuth.getInstance();

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
        soundNextRound = soundPool.load(this, R.raw.lvl_up, 1); // Cargar el sonido para el cambio de ronda

        // Inicializar la cuadrícula de botones
        initializeGrid();

        // Deshabilitar todos los botones al inicio
        enableGridButtons(false);

        playButton.setOnClickListener(v -> {
            startGame();
        });

        // Configurar el botón de regreso
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
        elapsedTime = 0; // Comenzar en 0:00
        roundCounter.setText("Rounds: " + currentRound);
        timer.setText(formatTime(elapsedTime)); // Mostrar el tiempo inicial
        playButton.setText("Restart"); // Cambiar el texto del botón
        enableGridButtons(true);
        showRandomLights();
        soundPool.play(soundStart, 1, 1, 0, 0, 1);
        startTimer(); // Iniciar el cronómetro
    }


    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                elapsedTime++; // Aumentar el tiempo
                timer.setText(formatTime(elapsedTime)); // Actualizar el texto del cronómetro
                handler.postDelayed(this, 1000); // Ejecutar cada segundo
            }
        };
        handler.postDelayed(timerRunnable, 1000); // Iniciar el cronómetro después de 1 segundo
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable); // Detener el cronómetro
    }

    private String formatTime(int timeInSeconds) {
        int minutes = timeInSeconds / 60;
        int seconds = timeInSeconds % 60;
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

        // Verificar si el botón está en la lista de posiciones iluminadas
        if (lightedPositions.contains(position)) {
            // Si el botón ya fue presionado (color azul), ignorar el clic
            if (button.getCurrentTextColor() == getResources().getColor(R.color.blue2)) {
                return; // No hacer nada
            }

            // Marcar el botón como presionado correctamente
            button.setBackgroundColor(getResources().getColor(R.color.blue2));
            lightedPositions.remove((Integer) position); // Remover la posición iluminada

            // Comprobar si todos los botones iluminados han sido pulsados
            if (lightedPositions.isEmpty()) {
                currentRound++;
                roundCounter.setText("Rounds: " + currentRound);

                // Reproducir sonido al pasar de ronda
                soundPool.play(soundNextRound, 1, 1, 0, 0, 1);

                handler.postDelayed(this::showRandomLights, 500);
            }
        } else {
            // Si el botón no estaba iluminado, el jugador pierde
            Toast.makeText(this, "¡You lost! Rounds played: " + currentRound, Toast.LENGTH_SHORT).show();
            soundPool.play(soundLose, 1, 1, 0, 0, 1);
            gameOver = true;

            // Marcar los botones iluminados en rojo
            for (int i = 0; i < gridLayout.getChildCount(); i++) {
                Button btn = (Button) gridLayout.getChildAt(i);
                if (lightedPositions.contains(i)) {
                    btn.setBackgroundColor(Color.RED);
                }
            }

            enableGridButtons(false);
            playButton.setText("Play");
            stopTimer(); // Detener el cronómetro al perder
        }
    }

    private void enableGridButtons(boolean enabled) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            Button button = (Button) gridLayout.getChildAt(i);
            button.setEnabled(enabled);
        }
    }
}
