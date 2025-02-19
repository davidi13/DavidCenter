package com.example.myloginscreen;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

public class ArkanoidGame extends AppCompatActivity {
    private ArkanoidSurfaceView arkanoidSurfaceView;
    private Chronometer chronometer;
    private TextView scoreText;
    private int score = 0;
    private int lives = 3;
    private final int maxLives = 3;

    private ImageView heart1, heart2, heart3;
    private boolean gameOver = false;
    private boolean isFirstTouch = true; // Variable para verificar si es el primer toque

    // Firestore variables
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arkanoid);

        // Initialize Firestore and get user ID
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> finish());

        chronometer = findViewById(R.id.chronometer);
        scoreText = findViewById(R.id.score_text);

        FrameLayout gameArea = findViewById(R.id.game_area);
        arkanoidSurfaceView = new ArkanoidSurfaceView(this);
        gameArea.addView(arkanoidSurfaceView);

        heart1 = findViewById(R.id.heart1);
        heart2 = findViewById(R.id.heart2);
        heart3 = findViewById(R.id.heart3);

        updateLivesDisplay();
    }

    private void updateLivesDisplay() {
        heart1.setVisibility(lives >= 1 ? View.VISIBLE : View.INVISIBLE);
        heart2.setVisibility(lives >= 2 ? View.VISIBLE : View.INVISIBLE);
        heart3.setVisibility(lives >= 3 ? View.VISIBLE : View.INVISIBLE);
    }

    private void gainLife() {
        if (lives < maxLives) {
            lives++;
            runOnUiThread(this::updateLivesDisplay);
        }
    }

    private void loseLife() {
        if (lives > 0) {
            lives--;
            runOnUiThread(this::updateLivesDisplay);
            if (lives == 0) {
                gameOver();
            } else {
                arkanoidSurfaceView.resetBallWithInitialVelocity();
            }
        }
    }

    private void gameOver() {
        gameOver = true;
        runOnUiThread(() -> {
            chronometer.stop();

            // Obtener el tiempo transcurrido
            long elapsedTime = SystemClock.elapsedRealtime() - chronometer.getBase();

            // Guardar el mejor puntaje en Firestore
            saveBestScore(userId, score, elapsedTime);

            Toast.makeText(ArkanoidGame.this, "Game over!", Toast.LENGTH_LONG).show();

        });
    }

    private void resetGame() {
        score = 0;  // Reinicia el puntaje solo al inicio del nuevo juego
        lives = maxLives;
        gameOver = false;
        updateLivesDisplay();
        scoreText.setText("Score: 0");  // Actualiza el puntaje en pantalla
        chronometer.setBase(SystemClock.elapsedRealtime());
        isFirstTouch = true; // Reiniciar la variable para permitir que el cronómetro inicie en el primer toque
        arkanoidSurfaceView.resetBallAndBlocks();
    }

    private void saveBestScore(String userId, int score, long elapsedTime) {
        if (userId == null) return;

        // Crear un nuevo documento para cada partida con un ID único
        DocumentReference scoreRef = db.collection("users").document(userId)
                .collection("score4").document();

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("score", score);
        scoreData.put("time", elapsedTime);
        scoreData.put("date", date);

        scoreRef.set(scoreData)
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar puntaje: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onPause() {
        super.onPause();
        arkanoidSurfaceView.pause();
        chronometer.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        arkanoidSurfaceView.resume();
    }

    class ArkanoidSurfaceView extends SurfaceView implements Runnable {
        private Thread gameThread;
        private boolean isPlaying;
        private Paint paint;
        private SurfaceHolder surfaceHolder;

        private float paddleX, paddleY;
        private float paddleWidth = 120, paddleHeight = 20;
        private float screenWidth, screenHeight;

        private float ballX, ballY;
        private float ballRadius = 15;
        private float initialVelocityX = 5, initialVelocityY = 5;
        private float ballVelocityX = initialVelocityX, ballVelocityY = initialVelocityY;
        private float speedIncrement = 0.5f;
        private float maxVelocity = 25f;
        private boolean ballMoving = false;

        private List<Block> blocks;
        private int[] blockColors;

        private int levelsCompleted = 0;

        public ArkanoidSurfaceView(Context context) {
            super(context);
            surfaceHolder = getHolder();
            paint = new Paint();

            blockColors = new int[]{
                    ContextCompat.getColor(context, R.color.grey_black),
                    ContextCompat.getColor(context, R.color.red),
                    ContextCompat.getColor(context, R.color.yellow),
                    ContextCompat.getColor(context, R.color.purple_700),
                    ContextCompat.getColor(context, R.color.green2)
            };
        }

        private void initializeBlocks() {
            blocks = new ArrayList<>();
            int rows = 5;
            int padding = 2;
            int topPadding = 50;

            int columns = 6;
            float blockWidth = (screenWidth - (columns + 1) * padding) / columns;
            float blockHeight = 40;

            for (int row = 0; row < rows; row++) {
                int color = blockColors[row % blockColors.length];
                int points = Math.max(10, 80 - (row * 20));
                for (int col = 0; col < columns; col++) {
                    float left = col * (blockWidth + padding) + padding;
                    float top = row * (blockHeight + padding) + padding + topPadding;
                    blocks.add(new Block(left, top, left + blockWidth, top + blockHeight, color, points));
                }
            }
        }

        @Override
        public void run() {
            while (isPlaying) {
                update();
                draw();
                control();
            }
        }

        private void initializeGame() {
            screenWidth = getWidth();
            screenHeight = getHeight();

            paddleY = screenHeight - paddleHeight - 50;
            paddleX = (screenWidth - paddleWidth) / 2;

            resetBall();
            initializeBlocks();
        }

        public void resetBallWithInitialVelocity() {
            resetBall();
            ballVelocityX = initialVelocityX;
            ballVelocityY = initialVelocityY;
        }

        public void resetBallAndBlocks() {
            resetBallWithInitialVelocity();
            initializeBlocks();
            score = 0;
            runOnUiThread(() -> scoreText.setText("Score: 0"));
        }

        public void resetGameState() {
            ballMoving = false;
            resetBallAndBlocks();
        }

        private void resetBall() {
            ballX = screenWidth / 2;
            ballY = screenHeight - (screenHeight / 4);
            ballMoving = false;
        }

        private void update() {
            if (ballMoving) {
                ballX += ballVelocityX;
                ballY += ballVelocityY;

                if (ballX < ballRadius || ballX > screenWidth - ballRadius) {
                    ballVelocityX = -ballVelocityX;
                }

                if (ballY < ballRadius) {
                    ballVelocityY = -ballVelocityY;
                }

                if (ballY + ballRadius >= paddleY && ballX >= paddleX && ballX <= paddleX + paddleWidth) {
                    ballVelocityY = -Math.abs(ballVelocityY);
                }

                if (ballY + ballRadius > screenHeight) {
                    loseLife();
                    resetBallWithInitialVelocity();
                }

                for (Block block : blocks) {
                    if (block.isVisible && ballX + ballRadius > block.left && ballX - ballRadius < block.right &&
                            ballY + ballRadius > block.top && ballY - ballRadius < block.bottom) {
                        ballVelocityY = -ballVelocityY;
                        block.isVisible = false;
                        score += block.points;

                        runOnUiThread(() -> scoreText.setText("Score: " + score));

                        if (Math.abs(ballVelocityX) < maxVelocity) {
                            ballVelocityX += (ballVelocityX > 0 ? speedIncrement : -speedIncrement);
                        }
                        if (Math.abs(ballVelocityY) < maxVelocity) {
                            ballVelocityY += (ballVelocityY > 0 ? speedIncrement : -speedIncrement);
                        }

                        break;
                    }
                }

                boolean allBlocksDestroyed = true;
                for (Block block : blocks) {
                    if (block.isVisible) {
                        allBlocksDestroyed = false;
                        break;
                    }
                }

                if (allBlocksDestroyed) {
                    levelsCompleted++;
                    if (levelsCompleted % 3 == 0) {
                        runOnUiThread(() -> gainLife());
                    }

                    ballX = screenWidth / 2;
                    ballY = screenHeight - (screenHeight / 4);
                    ballVelocityY = -Math.abs(ballVelocityY);
                    initializeBlocks();
                }
            }
        }

        private void draw() {
            if (surfaceHolder.getSurface().isValid()) {
                Canvas canvas = surfaceHolder.lockCanvas();

                if (screenWidth == 0 || screenHeight == 0) {
                    initializeGame();
                }

                canvas.drawColor(Color.BLACK);

                paint.setColor(Color.BLUE);
                canvas.drawRect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight, paint);

                paint.setColor(Color.WHITE);
                canvas.drawCircle(ballX, ballY, ballRadius, paint);

                for (Block block : blocks) {
                    if (block.isVisible) {
                        paint.setColor(block.color);
                        canvas.drawRect(block.left, block.top, block.right, block.bottom, paint);
                    }
                }

                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

        private void control() {
            try {
                Thread.sleep(17);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (gameOver) {
                    resetGame();
                } else if (!ballMoving && lives > 0) {
                    ballMoving = true;

                    // Iniciar el cronómetro solo en el primer toque
                    if (isFirstTouch) {
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.start();
                        isFirstTouch = false;
                    }
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float touchX = event.getX();
                paddleX = touchX - paddleWidth / 2;

                if (paddleX < 0) {
                    paddleX = 0;
                } else if (paddleX + paddleWidth > screenWidth) {
                    paddleX = screenWidth - paddleWidth;
                }
            }
            return true;
        }

        public void resume() {
            isPlaying = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        public void pause() {
            try {
                isPlaying = false;
                gameThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class Block {
        float left, top, right, bottom;
        int color;
        int points;
        boolean isVisible = true;

        Block(float left, float top, float right, float bottom, int color, int points) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.color = color;
            this.points = points;
        }
    }
}
