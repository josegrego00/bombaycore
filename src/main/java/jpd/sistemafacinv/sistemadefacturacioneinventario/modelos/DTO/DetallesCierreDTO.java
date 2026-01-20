package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO;

import java.util.List;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
public class DetallesCierreDTO {
    private List<DetalleRequest> detalles = new ArrayList<>();

    @Data
    public static class DetalleRequest {
        private Long id;
        private Double stockReal;
        private Double merma;
        private Double desperdicio;
    }
}