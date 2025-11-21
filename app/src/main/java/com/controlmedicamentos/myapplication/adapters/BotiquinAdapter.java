package com.controlmedicamentos.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.controlmedicamentos.myapplication.R;
import com.controlmedicamentos.myapplication.models.Medicamento;
import java.util.List;

public class BotiquinAdapter extends RecyclerView.Adapter<BotiquinAdapter.BotiquinViewHolder> {

    private Context context;
    private List<Medicamento> medicamentos;
    private OnMedicamentoClickListener listener;

    public interface OnMedicamentoClickListener {
        void onEditarClick(Medicamento medicamento);
        void onEliminarClick(Medicamento medicamento);
        void onAgregarStockClick(Medicamento medicamento);
        void onRestarStockClick(Medicamento medicamento); // Para medicamentos ocasionales
    }

    public BotiquinAdapter(Context context, List<Medicamento> medicamentos) {
        this.context = context;
        this.medicamentos = medicamentos;
    }

    public void setOnMedicamentoClickListener(OnMedicamentoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BotiquinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicamento_botiquin, parent, false);
        return new BotiquinViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BotiquinViewHolder holder, int position) {
        Medicamento medicamento = medicamentos.get(position);
        holder.bind(medicamento);
    }

    @Override
    public int getItemCount() {
        return medicamentos.size();
    }

    public void actualizarMedicamentos(List<Medicamento> nuevosMedicamentos) {
        this.medicamentos = nuevosMedicamentos;
        notifyDataSetChanged();
    }

    class BotiquinViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardMedicamento;
        private ImageView ivIcono;
        private TextView tvNombre;
        private TextView tvPresentacion;
        private TextView tvStock;
        private TextView tvEstado;
        private MaterialButton btnEditar;
        private MaterialButton btnEliminar;
        private MaterialButton btnAgregarStock;
        private MaterialButton btnRestarStock;

        public BotiquinViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMedicamento = itemView.findViewById(R.id.cardMedicamento);
            ivIcono = itemView.findViewById(R.id.ivIcono);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPresentacion = itemView.findViewById(R.id.tvPresentacion);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
            btnAgregarStock = itemView.findViewById(R.id.btnAgregarStock);
            btnRestarStock = itemView.findViewById(R.id.btnRestarStock);
        }

        public void bind(Medicamento medicamento) {
            // Configurar ícono
            ivIcono.setImageResource(medicamento.getIconoPresentacion());

            // Configurar nombre
            tvNombre.setText(medicamento.getNombre());

            // Configurar presentación
            tvPresentacion.setText(medicamento.getPresentacion());

            // Configurar stock
            String stockText = "Stock: " + medicamento.getStockActual() + "/" + medicamento.getStockInicial();
            tvStock.setText(stockText);

            // Configurar estado
            if (medicamento.estaVencido()) {
                tvEstado.setText("Vencido");
                tvEstado.setTextColor(context.getColor(R.color.error));
                btnEliminar.setVisibility(View.VISIBLE);
                btnEditar.setVisibility(View.GONE);
                btnAgregarStock.setVisibility(View.GONE);
                btnRestarStock.setVisibility(View.GONE);
            } else if (medicamento.isPausado()) {
                tvEstado.setText("Pausado");
                tvEstado.setTextColor(context.getColor(R.color.warning));
                btnEditar.setVisibility(View.VISIBLE);
                btnEliminar.setVisibility(View.VISIBLE);
                btnAgregarStock.setVisibility(View.GONE);
                btnRestarStock.setVisibility(View.GONE);
            } else {
                tvEstado.setText("Activo");
                tvEstado.setTextColor(context.getColor(R.color.success));
                btnEditar.setVisibility(View.VISIBLE);
                btnEliminar.setVisibility(View.VISIBLE);
                btnAgregarStock.setVisibility(View.VISIBLE);
                
                // Mostrar botón de restar stock solo para medicamentos ocasionales con stock > 0
                // Consistente con React: BotiquinScreen.jsx líneas 144-160
                if (medicamento.getTomasDiarias() == 0 && medicamento.getStockActual() > 0) {
                    btnRestarStock.setVisibility(View.VISIBLE);
                } else {
                    btnRestarStock.setVisibility(View.GONE);
                }
            }

            // Configurar color de fondo
            cardMedicamento.setCardBackgroundColor(medicamento.getColor());

            // Configurar listeners
            btnEditar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditarClick(medicamento);
                }
            });

            btnEliminar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEliminarClick(medicamento);
                }
            });

            btnAgregarStock.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAgregarStockClick(medicamento);
                }
            });

            btnRestarStock.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRestarStockClick(medicamento);
                }
            });
        }
    }
}