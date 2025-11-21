package com.controlmedicamentos.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.controlmedicamentos.myapplication.adapters.BotiquinAdapter;
import com.controlmedicamentos.myapplication.models.Medicamento;
import com.controlmedicamentos.myapplication.services.AuthService;
import com.controlmedicamentos.myapplication.services.FirebaseService;
import com.controlmedicamentos.myapplication.utils.NetworkUtils;
import java.util.List;
import android.content.Intent;

public class BotiquinActivity extends AppCompatActivity implements BotiquinAdapter.OnMedicamentoClickListener {

    private RecyclerView rvMedicamentos;
    private FloatingActionButton fabNuevaMedicina;
    private MaterialButton btnVolver;
    private BotiquinAdapter adapter;
    private List<Medicamento> medicamentos;
    private AuthService authService;
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_botiquin);

        // Inicializar servicios
        authService = new AuthService();
        firebaseService = new FirebaseService();

        // Verificar autenticación
        if (!authService.isUserLoggedIn()) {
            finish();
            return;
        }

        inicializarVistas();
        configurarRecyclerView();
        cargarMedicamentos();
        configurarListeners();
    }

    private void inicializarVistas() {
        rvMedicamentos = findViewById(R.id.rvMedicamentos);
        fabNuevaMedicina = findViewById(R.id.fabNuevaMedicina);
        btnVolver = findViewById(R.id.btnVolver);
    }

    private void configurarRecyclerView() {
        adapter = new BotiquinAdapter(this, medicamentos);
        adapter.setOnMedicamentoClickListener(this);
        rvMedicamentos.setLayoutManager(new LinearLayoutManager(this));
        rvMedicamentos.setAdapter(adapter);
    }

    private void cargarMedicamentos() {
        // Verificar conexión a internet
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        // Cargar todos los medicamentos desde Firebase
        firebaseService.obtenerMedicamentos(new FirebaseService.FirestoreListCallback() {
            @Override
            public void onSuccess(List<?> result) {
                medicamentos = (List<Medicamento>) result;
                adapter.actualizarMedicamentos(medicamentos);
                
                if (medicamentos.isEmpty()) {
                    Toast.makeText(BotiquinActivity.this, "No tienes medicamentos registrados", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(BotiquinActivity.this, 
                    "Error al cargar medicamentos: " + 
                    (exception != null ? exception.getMessage() : "Error desconocido"), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void configurarListeners() {
        fabNuevaMedicina.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BotiquinActivity.this, NuevaMedicinaActivity.class);
                startActivity(intent);
            }
        });

        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onEditarClick(Medicamento medicamento) {
        mostrarDialogoEditar(medicamento);
    }

    @Override
    public void onEliminarClick(Medicamento medicamento) {
        mostrarDialogoEliminar(medicamento);
    }

    @Override
    public void onAgregarStockClick(Medicamento medicamento) {
        mostrarDialogoAgregarStock(medicamento);
    }

    @Override
    public void onRestarStockClick(Medicamento medicamento) {
        // Restar stock para medicamento ocasional
        // Consistente con React: BotiquinScreen.jsx líneas 148-155
        if (medicamento.getTomasDiarias() == 0 && medicamento.getStockActual() > 0) {
            // Verificar conexión a internet
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
                return;
            }

            firebaseService.restarStockMedicamento(medicamento.getId(), new FirebaseService.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    // El listener en tiempo real actualizará la lista automáticamente
                    if (result instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> resultado = (java.util.Map<String, Object>) result;
                        Object stockActualObj = resultado.get("stockActual");
                        int nuevoStock = stockActualObj instanceof Number ? ((Number) stockActualObj).intValue() : 0;
                        Toast.makeText(BotiquinActivity.this, 
                            "Toma registrada. Stock actualizado: " + nuevoStock + " unidades restantes", 
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BotiquinActivity.this, "Toma registrada exitosamente", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception exception) {
                    Toast.makeText(BotiquinActivity.this, 
                        "Error al registrar toma: " + 
                        (exception != null ? exception.getMessage() : "Error desconocido"), 
                        Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void mostrarDialogoEditar(Medicamento medicamento) {
        // Abrir NuevaMedicinaActivity en modo edición
        Intent intent = new Intent(BotiquinActivity.this, NuevaMedicinaActivity.class);
        intent.putExtra("medicamento_id", medicamento.getId());
        startActivity(intent);
    }

    private void mostrarDialogoEliminar(Medicamento medicamento) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Medicamento")
                .setMessage("¿Estás seguro de que quieres eliminar " + medicamento.getNombre() + "?")
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Verificar conexión a internet
                        if (!NetworkUtils.isNetworkAvailable(BotiquinActivity.this)) {
                            Toast.makeText(BotiquinActivity.this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Eliminar de Firebase
                        firebaseService.eliminarMedicamento(medicamento.getId(), new FirebaseService.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                Toast.makeText(BotiquinActivity.this, "Medicamento eliminado", Toast.LENGTH_SHORT).show();
                                cargarMedicamentos();
                            }

                            @Override
                            public void onError(Exception exception) {
                                Toast.makeText(BotiquinActivity.this, 
                                    "Error al eliminar medicamento: " + 
                                    (exception != null ? exception.getMessage() : "Error desconocido"), 
                                    Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoAgregarStock(Medicamento medicamento) {
        Toast.makeText(this, "Agregar stock a " + medicamento.getNombre() + " - Próximamente", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarMedicamentos();
    }
}