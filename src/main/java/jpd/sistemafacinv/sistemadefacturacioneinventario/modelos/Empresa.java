package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(unique = true)
    private String subdominio;

    private boolean estado;
    private String emailContacto;

    private String telefono;
    private String plan; // "BASICO", "PREMIUM", "ENTERPRISE"
    @OneToMany(mappedBy = "empresa")
    private List<Sucursal> sucursales;

    private LocalDate fechaCreacion;

}
