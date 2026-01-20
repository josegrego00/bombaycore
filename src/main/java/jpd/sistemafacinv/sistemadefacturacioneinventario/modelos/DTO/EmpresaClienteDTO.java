package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaClienteDTO {

    private String nombre;
    private String subdominio;
    private String emailContacto;

    private String telefono;
    private String plan; // "BASICO", "PREMIUM", "ENTERPRISE"

    // Configuración inicial
    @Builder.Default
    private boolean crearUsuarios = true;

    @Builder.Default
    private boolean crearDatosIniciales = true;

}