package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos;

import java.util.List;

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
        @UniqueConstraint(columnNames = { "empresa_id", "nombre" }),
        @UniqueConstraint(columnNames = { "empresa_id", "identificacion" }),
        @UniqueConstraint(columnNames = { "empresa_id", "correo" }),
})
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    private String nombre;

    private String identificacion;

    private String telefono;

    private String correo;

    private Boolean activo;

    @OneToMany(mappedBy = "proveedor")
    private List<FacturaProveedor> facturas;

}
