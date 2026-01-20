package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
        @UniqueConstraint(columnNames = { "empresa_id", "nombre" })
})
public class Ingrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    private String nombre;
    private String unidadMedida;
    private double stockActual;
    private double stockMinimo;
    private double precio;
    private boolean activo = true;

    @OneToMany(mappedBy = "ingrediente", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<RecetaDetalle> recetaDetalles;

    @OneToMany(mappedBy = "ingrediente")
    @JsonIgnore
    private List<FacturaDetalleProveedor> comprasDetalles;

}
