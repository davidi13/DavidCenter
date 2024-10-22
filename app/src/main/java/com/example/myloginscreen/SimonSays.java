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

import java.util.ArrayList;
import java.util.List;
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

    @Override
    protected void onPause() {
        super.onPause();
        // Pausa todos los sonidos cuando la app se pone en segundo plano
        if (soundPool != null) {
            soundPool.autoPause(); // Detiene todos los sonidos en curso
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si quieres que los sonidos se reanuden al regresar a la app,
        // puedes llamar a soundPool.autoResume(); si es necesario.
        soundPool.autoResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simon_says);

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
            illuminateButton(0); // Ilumina el botón rojo
            handleUserInput(0);
            playSound(0); // Reproducir sonido al presionar el botón rojo
        });
        buttonBlue.setOnClickListener(v -> {
            illuminateButton(1); // Ilumina el botón azul
            handleUserInput(1);
            playSound(1); // Reproducir sonido al presionar el botón azul
        });
        buttonGreen.setOnClickListener(v -> {
            illuminateButton(2); // Ilumina el botón verde
            handleUserInput(2);
            playSound(2); // Reproducir sonido al presionar el botón verde
        });
        buttonYellow.setOnClickListener(v -> {
            illuminateButton(3); // Ilumina el botón amarillo
            handleUserInput(3);
            playSound(3); // Reproducir sonido al presionar el botón amarillo
        });

        // Configura el botón de ir hacia atrás
        backButton.setOnClickListener(v -> {
            if (soundPool != null) {
                soundPool.autoPause(); // Detener sonidos al regresar al home
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
        isPlayingSequence = true; // Indica que la secuencia está sonando
        Handler handler = new Handler();
        for (int i = 0; i < sequence.size(); i++) {
            int colorIndex = sequence.get(i);
            handler.postDelayed(() -> {
                if (gameActive) { // Solo ilumina y reproduce sonidos si el juego está activo
                    illuminateButton(colorIndex);
                    playSound(colorIndex);
                }
            }, i * 1000); // Retraso de 1 segundo entre cada color
        }

        // Reiniciar isPlayingSequence después de que la secuencia se haya mostrado
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
            default: return;
        }

        // Revertir el color del botón después de medio segundo
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
            // Error en la entrada del usuario
            gameActive = false; // Detener el juego
            if (isPlayingSequence) { // Si la secuencia está sonando, puedes cancelarla aquí
                // Detenemos cualquier sonido en curso
                soundPool.autoPause(); // Pausa todos los sonidos
            }
            soundPool.play(loseSoundId, 1, 1, 0, 0, 1); // Reproducir sonido de derrota
            chronometer.stop(); // Detiene el cronómetro
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
}
