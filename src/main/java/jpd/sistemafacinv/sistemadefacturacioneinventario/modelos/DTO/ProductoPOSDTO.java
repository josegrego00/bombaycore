package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductoPOSDTO {
    private Long id;
    private String nombre;
    private Double precio;
    private boolean tieneReceta;
    private Double stockPosible;
}
