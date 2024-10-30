package com.example.myloginscreen;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class Game2048Activity extends AppCompatActivity {

    private int[][] board;
    private TextView[][] tiles;
    private TextView scoreView;
    private int score = 0;
    private final int GRID_SIZE = 4;

    private int[][] previousBoard; // Para guardar el estado anterior del tablero

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game2048);

        board = new int[GRID_SIZE][GRID_SIZE];
        tiles = new TextView[GRID_SIZE][GRID_SIZE];
        scoreView = findViewById(R.id.score_view);

        initBoard();
        initSwipeListener();
        addRandomTile();
        addRandomTile();

        // Configurar los botones
        Button undoButton = findViewById(R.id.button_undo);
        Button restartButton = findViewById(R.id.button_restart);

        undoButton.setOnClickListener(v -> undoMove());
        restartButton.setOnClickListener(v -> restartGame());
    }

    private void initBoard() {
        GridLayout gridLayout = findViewById(R.id.grid_layout);

        if (gridLayout.getChildCount() != GRID_SIZE * GRID_SIZE) {
            Toast.makeText(this, "Error: GridLayout debe tener 16 hijos", Toast.LENGTH_LONG).show();
            return;
        }

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                tiles[i][j] = (TextView) gridLayout.getChildAt(i * GRID_SIZE + j);
            }
        }
        updateBoard();
    }

    private void initSwipeListener() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (diffX > 0) swipeRight();
                    else swipeLeft();
                } else {
                    if (diffY > 0) swipeDown();
                    else swipeUp();
                }
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private void swipeLeft() {
        savePreviousState(); // Guardar el estado antes de mover
        boolean moved = false;
        for (int i = 0; i < GRID_SIZE; i++) {
            int[] newRow = new int[GRID_SIZE];
            int position = 0;

            for (int j = 0; j < GRID_SIZE; j++) {
                if (board[i][j] != 0) {
                    if (position > 0 && newRow[position - 1] == board[i][j]) {
                        newRow[position - 1] *= 2;
                        score += newRow[position - 1];
                        moved = true;
                    } else {
                        newRow[position] = board[i][j];
                        if (position != j) moved = true;
                        position++;
                    }
                }
            }
            board[i] = newRow;
        }
        if (moved) {
            addRandomTile();
            updateBoard();
        }
    }

    private void swipeRight() {
        savePreviousState(); // Guardar el estado antes de mover
        boolean moved = false;
        for (int i = 0; i < GRID_SIZE; i++) {
            int[] newRow = new int[GRID_SIZE];
            int position = GRID_SIZE - 1;

            for (int j = GRID_SIZE - 1; j >= 0; j--) {
                if (board[i][j] != 0) {
                    if (position < GRID_SIZE - 1 && newRow[position + 1] == board[i][j]) {
                        newRow[position + 1] *= 2;
                        score += newRow[position + 1];
                        moved = true;
                    } else {
                        newRow[position] = board[i][j];
                        if (position != j) moved = true;
                        position--;
                    }
                }
            }
            board[i] = newRow;
        }
        if (moved) {
            addRandomTile();
            updateBoard();
        }
    }

    private void swipeUp() {
        savePreviousState(); // Guardar el estado antes de mover
        boolean moved = false;
        for (int j = 0; j < GRID_SIZE; j++) {
            int[] newCol = new int[GRID_SIZE];
            int position = 0;

            for (int i = 0; i < GRID_SIZE; i++) {
                if (board[i][j] != 0) {
                    if (position > 0 && newCol[position - 1] == board[i][j]) {
                        newCol[position - 1] *= 2;
                        score += newCol[position - 1];
                        moved = true;
                    } else {
                        newCol[position] = board[i][j];
                        if (position != i) moved = true;
                        position++;
                    }
                }
            }
            for (int i = 0; i < GRID_SIZE; i++) {
                board[i][j] = newCol[i];
            }
        }
        if (moved) {
            addRandomTile();
            updateBoard();
        }
    }

    private void swipeDown() {
        savePreviousState(); // Guardar el estado antes de mover
        boolean moved = false;
        for (int j = 0; j < GRID_SIZE; j++) {
            int[] newCol = new int[GRID_SIZE];
            int position = GRID_SIZE - 1;

            for (int i = GRID_SIZE - 1; i >= 0; i--) {
                if (board[i][j] != 0) {
                    if (position < GRID_SIZE - 1 && newCol[position + 1] == board[i][j]) {
                        newCol[position + 1] *= 2;
                        score += newCol[position + 1];
                        moved = true;
                    } else {
                        newCol[position] = board[i][j];
                        if (position != i) moved = true;
                        position--;
                    }
                }
            }
            for (int i = 0; i < GRID_SIZE; i++) {
                board[i][j] = newCol[i];
            }
        }
        if (moved) {
            addRandomTile();
            updateBoard();
        }
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
                tiles[i][j].setText(value == 0 ? "" : String.valueOf(value));

                // Cambiar el tamaño de texto basado en el valor
                if (value > 99) {
                    tiles[i][j].setTextSize(18); // Tamaño de texto para números de 3 dígitos o más
                } else {
                    tiles[i][j].setTextSize(24); // Tamaño de texto para números de 2 dígitos o menos
                }

                // Actualizar el color de fondo según el valor
                int color;
                switch (value) {
                    case 0:
                        color = ContextCompat.getColor(this, R.color.color_0);
                        break;
                    case 2:
                        color = ContextCompat.getColor(this, R.color.color_2);
                        break;
                    case 4:
                        color = ContextCompat.getColor(this, R.color.color_4);
                        break;
                    case 8:
                        color = ContextCompat.getColor(this, R.color.color_8);
                        break;
                    case 16:
                        color = ContextCompat.getColor(this, R.color.color_16);
                        break;
                    case 32:
                        color = ContextCompat.getColor(this, R.color.color_32);
                        break;
                    case 64:
                        color = ContextCompat.getColor(this, R.color.color_64);
                        break;
                    case 128:
                        color = ContextCompat.getColor(this, R.color.color_128);
                        break;
                    case 256:
                        color = ContextCompat.getColor(this, R.color.color_256);
                        break;
                    case 512:
                        color = ContextCompat.getColor(this, R.color.color_512);
                        break;
                    case 1024:
                        color = ContextCompat.getColor(this, R.color.color_1024);
                        break;
                    case 2048:
                        color = ContextCompat.getColor(this, R.color.color_2048);
                        break;
                    default:
                        color = ContextCompat.getColor(this, R.color.color_default);
                        break;
                }
                tiles[i][j].setBackgroundColor(color);
            }
        }
        scoreView.setText("Score: " + score);
    }


    private void savePreviousState() {
        previousBoard = new int[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(board[i], 0, previousBoard[i], 0, GRID_SIZE);
        }
    }

    private void undoMove() {
        if (previousBoard != null) {
            board = previousBoard;
            updateBoard();
        } else {
            Toast.makeText(this, "No hay movimientos para deshacer", Toast.LENGTH_SHORT).show();
        }
    }

    private void restartGame() {
        score = 0;
        board = new int[GRID_SIZE][GRID_SIZE];
        previousBoard = null; // Reiniciar el estado anterior
        addRandomTile();
        addRandomTile();
        updateBoard();
    }
}
