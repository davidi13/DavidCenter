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
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ArkanoidGame extends AppCompatActivity {
    private ArkanoidSurfaceView arkanoidSurfaceView;
    private Chronometer chronometer;
    private TextView scoreText;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arkanoid);

        // Configurar el botón de retroceso
        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> finish());

        // Configurar el cronómetro y el puntaje
        chronometer = findViewById(R.id.chronometer);
        scoreText = findViewById(R.id.score_text);

        // Configurar el área de juego en el FrameLayout
        FrameLayout gameArea = findViewById(R.id.game_area);
        arkanoidSurfaceView = new ArkanoidSurfaceView(this);
        gameArea.addView(arkanoidSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        arkanoidSurfaceView.pause();
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

        // Configuración de la plataforma
        private float paddleX, paddleY;
        private float paddleWidth = 150, paddleHeight = 20; // Tamaño reducido de la plataforma
        private float screenWidth, screenHeight;

        // Configuración de la pelota
        private float ballX, ballY;
        private float ballRadius = 15; // Tamaño reducido de la pelota
        private float ballVelocityX = 5, ballVelocityY = 5;
        private float speedIncrement = 0.2f; // Incremento de velocidad más significativo
        private float maxVelocity = 20f; // Velocidad máxima de la pelota
        private boolean ballMoving = false;

        // Configuración de bloques
        private List<Block> blocks;
        private int[] blockColors;

        public ArkanoidSurfaceView(Context context) {
            super(context);
            surfaceHolder = getHolder();
            paint = new Paint();

            // Colores para los bloques
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
            int padding = 2; // Espaciado de 2dp entre bloques
            int topPadding = 50; // Espacio adicional desde la parte superior

            // Calcular el ancho de los bloques basado en el ancho total de la pantalla y el padding
            int columns = 6; // Número de columnas de bloques
            float blockWidth = (screenWidth - (columns + 1) * padding) / columns;
            float blockHeight = 40;

            // Crear los bloques en filas y columnas
            for (int row = 0; row < rows; row++) {
                int color = blockColors[row % blockColors.length];
                int points = Math.max(10, 80 - (row * 20)); // Puntos por fila, asegurando mínimo de 10 puntos
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

            // Posicionar la plataforma cerca del borde inferior
            paddleY = screenHeight - paddleHeight - 50;
            paddleX = (screenWidth - paddleWidth) / 2;

            // Posicionar la pelota en el centro del FrameLayout
            ballX = screenWidth / 2;
            ballY = screenHeight / 2;

            // Inicializar los bloques después de obtener las dimensiones de la pantalla
            initializeBlocks();
        }

        private void update() {
            if (ballMoving) {
                ballX += ballVelocityX;
                ballY += ballVelocityY;

                // Rebote en los bordes laterales
                if (ballX < ballRadius || ballX > screenWidth - ballRadius) {
                    ballVelocityX = -ballVelocityX;
                }

                // Rebote en el borde superior
                if (ballY < ballRadius) {
                    ballVelocityY = -ballVelocityY;
                }

                // Rebote en la plataforma
                if (ballY + ballRadius >= paddleY && ballX >= paddleX && ballX <= paddleX + paddleWidth) {
                    ballVelocityY = -ballVelocityY;
                }

                // Colisiones con los bloques
                for (Block block : blocks) {
                    if (block.isVisible && ballX + ballRadius > block.left && ballX - ballRadius < block.right &&
                            ballY + ballRadius > block.top && ballY - ballRadius < block.bottom) {
                        ballVelocityY = -ballVelocityY;  // Rebote en el bloque
                        block.isVisible = false;         // Ocultar el bloque
                        score += block.points;           // Incrementar puntaje

                        // Actualizar la puntuación en la interfaz
                        runOnUiThread(() -> scoreText.setText("Score: " + score));

                        // Incrementar la velocidad de la pelota
                        if (Math.abs(ballVelocityX) < maxVelocity) {
                            ballVelocityX += (ballVelocityX > 0 ? speedIncrement : -speedIncrement);
                        }
                        if (Math.abs(ballVelocityY) < maxVelocity) {
                            ballVelocityY += (ballVelocityY > 0 ? speedIncrement : -speedIncrement);
                        }

                        break;
                    }
                }

                // Verificar si todos los bloques han sido destruidos
                boolean allBlocksDestroyed = true;
                for (Block block : blocks) {
                    if (block.isVisible) {
                        allBlocksDestroyed = false;
                        break;
                    }
                }

                // Regenerar los bloques si todos fueron destruidos
                if (allBlocksDestroyed) {
                    // Mover la pelota debajo del área de bloques antes de generar nuevos bloques
                    ballY = screenHeight - (screenHeight / 4);
                    initializeBlocks();
                }

                // Reiniciar posición si la pelota cae debajo de la plataforma
                if (ballY > screenHeight) {
                    ballX = screenWidth / 2;
                    ballY = screenHeight / 2;
                    ballMoving = false;
                    chronometer.stop(); // Detener el cronómetro
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

                // Dibujar la plataforma
                paint.setColor(Color.BLUE);
                canvas.drawRect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight, paint);

                // Dibujar la pelota
                paint.setColor(Color.WHITE);
                canvas.drawCircle(ballX, ballY, ballRadius, paint);

                // Dibujar los bloques
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
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!ballMoving) {
                        ballMoving = true;
                        chronometer.setBase(SystemClock.elapsedRealtime()); // Reiniciar el cronómetro
                        chronometer.start(); // Iniciar el cronómetro
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    float touchX = event.getX();
                    paddleX = touchX - paddleWidth / 2;

                    if (paddleX < 0) {
                        paddleX = 0;
                    } else if (paddleX + paddleWidth > screenWidth) {
                        paddleX = screenWidth - paddleWidth;
                    }
                    break;
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
