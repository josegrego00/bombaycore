package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jpd.sistemafacinv.sistemadefacturacioneinventario.servicios.UsuarioServicio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FacturaDetalle {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServicio.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "factura_id", nullable = false)
    @JsonIgnore
    private Factura factura;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    private int cantidad;

    private double precioUnitario;

    private double subtotal;

    // Calcular automáticamente antes de persistir
    @PrePersist
    @PreUpdate
    private void calcularSubtotal() {
        // AÑADE ESTO:
        String metodo = "";
        try {
            metodo = new Throwable().getStackTrace()[1].getMethodName();
        } catch (Exception e) {
        }

        log.warn("🚨 EJECUTANDO calcularSubtotal() - Desde: {}, FacturaDetalle ID: {}",
                metodo, this.id);

        if (this.precioUnitario > 0 && this.cantidad > 0) {
            this.subtotal = this.cantidad * this.precioUnitario;
            log.warn("Subtotal calculado: {} x {} = {}",
                    this.cantidad, this.precioUnitario, this.subtotal);
        }
    }
}