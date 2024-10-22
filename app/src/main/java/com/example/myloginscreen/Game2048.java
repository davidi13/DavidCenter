package com.example.myloginscreen;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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

        // Ajustar insets para el layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ImageView que será arrastrada
        ImageView draggableImage = findViewById(R.id.draggable_image);

        // Área de drop (cuadrado de 200dp x 200dp)
        View dropArea = findViewById(R.id.drop_area);

        // Inicializar GestureDetector
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                Log.d("Gestures", "onDown: Presionado en la pantalla");
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d("Gestures", "onFling: Fling detectado, velocidad X: " + velocityX + " velocidad Y: " + velocityY);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Log.d("Gestures", "onLongPress: Presionado largo en la pantalla");
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                Log.d("Gestures", "onScroll: Desplazamiento detectado, distancia X: " + distanceX + " distancia Y: " + distanceY);
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.d("Gestures", "onSingleTapUp: Toque detectado");
                return true;
            }
        });

        // Listener para iniciar el drag cuando se toca la imagen
        draggableImage.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);  // Detectar gestos también al tocar la imagen
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
                String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                ClipData dragData = new ClipData(v.getTag().toString(), mimeTypes, item);

                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(dragData, shadowBuilder, v, 0);
                v.setVisibility(View.INVISIBLE);  // Hacer invisible la imagen arrastrada
                return true;
            } else {
                return false;
            }
        });

        // Listener para manejar el evento de drop
        dropArea.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        return true;
                    }
                    return false;
                case DragEvent.ACTION_DRAG_ENTERED:
                    Log.d("DragDrop", "Imagen entró en el área de drop");
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    // Se podría obtener la posición del evento si es necesario
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    Log.d("DragDrop", "Imagen salió del área de drop");
                    return true;
                case DragEvent.ACTION_DROP:
                    Log.d("DragDrop", "Imagen soltada en el área de drop");

                    // Reubicar la imagen en la posición exacta donde fue soltada
                    View view = (View) event.getLocalState();
                    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) view.getLayoutParams();

                    // Coordenadas relativas al área de drop
                    float dropX = event.getX() - (view.getWidth() / 2);
                    float dropY = event.getY() - (view.getHeight() / 2);

                    // Restringir las coordenadas dentro del área de drop
                    int dropAreaWidth = dropArea.getWidth();
                    int dropAreaHeight = dropArea.getHeight();

                    if (dropX < 0) dropX = 0;
                    if (dropY < 0) dropY = 0;
                    if (dropX + view.getWidth() > dropAreaWidth) dropX = dropAreaWidth - view.getWidth();
                    if (dropY + view.getHeight() > dropAreaHeight) dropY = dropAreaHeight - view.getHeight();

                    // Actualizar la posición de la imagen dentro del área de drop
                    layoutParams.leftMargin = (int) dropX + dropArea.getLeft();
                    layoutParams.topMargin = (int) dropY + dropArea.getTop();
                    view.setLayoutParams(layoutParams);

                    view.setVisibility(View.VISIBLE);
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (!event.getResult()) {
                        View viewEnded = (View) event.getLocalState();
                        viewEnded.setVisibility(View.VISIBLE);  // Si el drop falla, la imagen reaparece
                    }
                    return true;
                default:
                    break;
            }
            return false;
        });

        // Listener general para gestos en la pantalla
        findViewById(R.id.main).setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }
}
