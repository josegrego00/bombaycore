package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
        @UniqueConstraint(columnNames = { "empresa_id", "fecha" })
})
public class CierreInventarioDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    private LocalDate fecha;

    @ManyToOne // ← AÑADIR ESTA ANOTACIÓN
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    private String estado;
    private String observaciones;

    @OneToMany(mappedBy = "cierre", cascade = CascadeType.ALL)
    private List<DetalleCierreInventarioDiario> detalles = new ArrayList<>();

    private double totalVentas;
    private int cantidadFacturas;

}
