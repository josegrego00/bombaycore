package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "cierre_id", "producto_id" }),
        @UniqueConstraint(columnNames = { "cierre_id", "ingrediente_id" })
})
public class DetalleCierreInventarioDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cierre_id", nullable = false)
    private CierreInventarioDiario cierre;

    // PRODUCTOS SIN RECETA (Coca Cola, etc.)
    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = true)
    private Producto producto; // Si es producto, debe ser tieneReceta = false

    // INGREDIENTES (materia prima)
    @ManyToOne
    @JoinColumn(name = "ingrediente_id", nullable = true)
    private Ingrediente ingrediente;

    // Campos numéricos
    private double stockTeorico;
    private double stockReal;
    private double stockMerma;
    private double stockDesperdicio;
    private double diferencia;
    private double costoUnitario;
    private double valorDiferencia;

    // Validación
    private boolean isValido() {
        if (producto != null && ingrediente != null)
            return false;
        if (producto != null && producto.isTieneReceta())
            return false; // ¡IMPORTANTE!
        return producto != null || ingrediente != null;
    }

}
