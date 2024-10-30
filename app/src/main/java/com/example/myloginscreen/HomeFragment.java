package com.example.myloginscreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {

    private FirebaseAuth auth; // Inicializa FirebaseAuth
    private TextView profileEmailTextView; // TextView para mostrar el correo

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance(); // Obtener la instancia de FirebaseAuth
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserEmail(); // Cargar el correo electrónico del usuario al regresar a la actividad
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Encuentra el NavigationView para obtener el nav_header
        NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        profileEmailTextView = headerView.findViewById(R.id.email_header_text); // Asegúrate de que este ID esté correcto

        // Llamar a loadUserEmail para establecer el correo del usuario
        loadUserEmail();

        // Encuentra el botón SIMON DICE y establece el OnClickListener
        Button simonDiceButton = view.findViewById(R.id.rounded_button);
        simonDiceButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SimonSays.class);
            startActivity(intent);
        });

        // Encuentra el botón 2048 y establece el OnClickListener
        Button game2048Button = view.findViewById(R.id.second_rounded_button);
        game2048Button.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Game2048Activity.class);
            startActivity(intent);
        });

        // Encuentra el botón LIGHTS OUTSIDE y establece el OnClickListener
        Button lightsOutsideButton = view.findViewById(R.id.third_rounded_button);
        lightsOutsideButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LightsOutside.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            profileEmailTextView.setText(email); // Establecer el texto del TextView al correo del usuario
        } else {
            profileEmailTextView.setText("No user logged in"); // Opcional: mensaje si no hay usuario
        }
    }
}
