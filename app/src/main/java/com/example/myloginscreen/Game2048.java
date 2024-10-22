package com.example.myloginscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Game2048 extends AppCompatActivity {

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game2048);

        // Configuración de insets para ajustar los paddings
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicialización del GestureDetector
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                Log.d("GestureDetector", "Pantalla presionada");
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d("GestureDetector", "Desplazamiento detectado: Velocidad X = " + velocityX + ", Velocidad Y = " + velocityY);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                Log.d("GestureDetector", "Desplazamiento en progreso: Distancia X = " + distanceX + ", Distancia Y = " + distanceY);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Log.d("GestureDetector", "Long press detectado");
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.d("GestureDetector", "Tap simple detectado");
                return true;
            }
        });

        // Configuración del listener de gestos en la vista principal
        findViewById(R.id.main).setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true; // Para asegurarse de que los gestos sean detectados
        });
    }
}
