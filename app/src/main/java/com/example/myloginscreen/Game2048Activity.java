package com.example.myloginscreen;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Game2048Activity extends AppCompatActivity {

    private static final int GRID_SIZE = 4;
    private static final int TEXT_SIZE_SMALL = 18;
    private static final int TEXT_SIZE_LARGE = 24;

    private int[][] board;
    private TextView[][] tiles;
    private TextView scoreView;
    private int score = 0;
    private ImageButton backButton2048;

    private Chronometer chronometer2048;
    private boolean isChronometerStarted = false;

    private FirebaseFirestore db;
    private String userId;

    private int[][] previousBoard;
    private GestureDetector gestureDetector;
    private Map<Integer, Integer> colorMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game2048);

        // Inicializar Firestore y obtener el ID del usuario
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        initializeColorMap();
        initializeBoard();

        addRandomTile();
        addRandomTile();
        updateBoard();

        initSwipeListener();
    }

    // Método para guardar el mejor puntaje
    private void saveBestScore(int score, long elapsedTime) {
        if (userId == null) return; // Verificar que el usuario esté autenticado

        DocumentReference scoreRef = db.collection("users").document(userId).collection("scores").document("best_score");

        // Formatear la fecha y hora
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Datos a guardar
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("score", score);
        scoreData.put("time", elapsedTime); // tiempo en milisegundos
        scoreData.put("date", date);

        // Obtener el mejor puntaje actual y actualizar si el nuevo puntaje es mayor
        scoreRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                int bestScore = document.getLong("score").intValue();
                if (score > bestScore) {
                    scoreRef.set(scoreData); // Actualizar con el nuevo mejor puntaje
                }
            } else {
                scoreRef.set(scoreData); // Guardar si no existe puntaje previo
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error al guardar puntaje", Toast.LENGTH_SHORT).show());
    }


    private void initializeViews() {
        backButton2048 = findViewById(R.id.button_back_2048);
        chronometer2048 = findViewById(R.id.chronometer_2048);
        scoreView = findViewById(R.id.score_view);

        backButton2048.setOnClickListener(v -> finish());

        Button undoButton = findViewById(R.id.button_undo);
        Button restartButton = findViewById(R.id.button_restart);
        undoButton.setOnClickListener(v -> undoMove());
        restartButton.setOnClickListener(v -> restartGame());
    }

    private void initializeColorMap() {
        colorMap = new HashMap<>();
        colorMap.put(0, R.color.color_0);
        colorMap.put(2, R.color.color_2);
        colorMap.put(4, R.color.color_4);
        colorMap.put(8, R.color.color_8);
        colorMap.put(16, R.color.color_16);
        colorMap.put(32, R.color.color_32);
        colorMap.put(64, R.color.color_64);
        colorMap.put(128, R.color.color_128);
        colorMap.put(256, R.color.color_256);
        colorMap.put(512, R.color.color_512);
        colorMap.put(1024, R.color.color_1024);
        colorMap.put(2048, R.color.color_2048);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeBoard() {
        GridLayout gridLayout = findViewById(R.id.grid_layout);
        board = new int[GRID_SIZE][GRID_SIZE];
        tiles = new TextView[GRID_SIZE][GRID_SIZE];

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                tiles[i][j] = (TextView) gridLayout.getChildAt(i * GRID_SIZE + j);
            }
        }

        gridLayout.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });
    }

    private void initSwipeListener() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (diffX > 0) moveBoard(Direction.RIGHT);
                    else moveBoard(Direction.LEFT);
                } else {
                    if (diffY > 0) moveBoard(Direction.DOWN);
                    else moveBoard(Direction.UP);
                }
                return true;
            }
        });
    }

    private void startChronometerIfNotStarted() {
        if (!isChronometerStarted) {
            chronometer2048.setBase(SystemClock.elapsedRealtime());
            chronometer2048.start();
            isChronometerStarted = true;
        }
    }

    private void moveBoard(Direction direction) {
        startChronometerIfNotStarted();
        savePreviousState();

        boolean moved = false;
        for (int i = 0; i < GRID_SIZE; i++) {
            int[] line = extractLine(i, direction);
            int[] newLine = compressAndMergeLine(line);
            if (!moved && !compareLines(line, newLine)) moved = true;
            insertLine(i, newLine, direction);
        }

        if (moved) {
            addRandomTile();
            updateBoard();

            if (!hasMovesAvailable()) {
                endGame();
            }
        }
    }

    private int[] extractLine(int index, Direction direction) {
        int[] line = new int[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            switch (direction) {
                case LEFT:
                    line[i] = board[index][i];
                    break;
                case RIGHT:
                    line[i] = board[index][GRID_SIZE - 1 - i];
                    break;
                case UP:
                    line[i] = board[i][index];
                    break;
                case DOWN:
                    line[i] = board[GRID_SIZE - 1 - i][index];
                    break;
            }
        }
        return line;
    }

    private void insertLine(int index, int[] line, Direction direction) {
        for (int i = 0; i < GRID_SIZE; i++) {
            switch (direction) {
                case LEFT:
                    board[index][i] = line[i];
                    break;
                case RIGHT:
                    board[index][GRID_SIZE - 1 - i] = line[i];
                    break;
                case UP:
                    board[i][index] = line[i];
                    break;
                case DOWN:
                    board[GRID_SIZE - 1 - i][index] = line[i];
                    break;
            }
        }
    }

    private int[] compressAndMergeLine(int[] line) {
        int[] compressedLine = new int[GRID_SIZE];
        int position = 0;
        for (int num : line) {
            if (num != 0) {
                if (position > 0 && compressedLine[position - 1] == num) {
                    compressedLine[position - 1] *= 2;
                    score += compressedLine[position - 1];
                } else {
                    compressedLine[position++] = num;
                }
            }
        }
        return compressedLine;
    }

    private boolean compareLines(int[] line1, int[] line2) {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (line1[i] != line2[i]) return false;
        }
        return true;
    }

    private void addRandomTile() {
        Random random = new Random();
        int row, col;
        do {
            row = random.nextInt(GRID_SIZE);
            col = random.nextInt(GRID_SIZE);
        } while (board[row][col] != 0);
        board[row][col] = random.nextInt(10) < 9 ? 2 : 4;
    }

    private void updateBoard() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                int value = board[i][j];
                TextView tile = tiles[i][j];

                tile.setText(value == 0 ? "" : String.valueOf(value));
                tile.setTextSize(value > 99 ? TEXT_SIZE_SMALL : TEXT_SIZE_LARGE);

                int colorRes = colorMap.getOrDefault(value, R.color.color_default);
                Drawable background = ContextCompat.getDrawable(this, R.drawable.title_background);
                if (background != null) {
                    background.setTint(ContextCompat.getColor(this, colorRes));
                    tile.setBackground(background);
                }
            }
        }
        scoreView.setText("Score: " + score);
    }

    private boolean hasMovesAvailable() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (board[i][j] == 0) return true;
                if (j < GRID_SIZE - 1 && board[i][j] == board[i][j + 1]) return true;
                if (i < GRID_SIZE - 1 && board[i][j] == board[i + 1][j]) return true;
            }
        }
        return false;
    }

    // Llamar a saveBestScore cuando el juego termina
    private void endGame() {
        chronometer2048.stop();
        long elapsedTime = SystemClock.elapsedRealtime() - chronometer2048.getBase();

        // Guardar el puntaje si es el mejor
        saveBestScore(score, elapsedTime);

        // Mostrar mensaje al usuario
        Toast.makeText(this, "¡GAME OVER! No more moves.", Toast.LENGTH_LONG).show();
    }

    private void savePreviousState() {
        if (previousBoard == null) {
            previousBoard = new int[GRID_SIZE][GRID_SIZE];
        }
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(board[i], 0, previousBoard[i], 0, GRID_SIZE);
        }
    }

    private void undoMove() {
        if (previousBoard != null) {
            board = previousBoard;
            updateBoard();
        } else {
            Toast.makeText(this, "No movements for undo", Toast.LENGTH_SHORT).show();
        }
    }

    private void restartGame() {
        score = 0;
        board = new int[GRID_SIZE][GRID_SIZE];
        previousBoard = null;

        chronometer2048.setBase(SystemClock.elapsedRealtime());
        isChronometerStarted = true;

        addRandomTile();
        addRandomTile();
        updateBoard();
    }

    private enum Direction {
        LEFT, RIGHT, UP, DOWN
    }
}
