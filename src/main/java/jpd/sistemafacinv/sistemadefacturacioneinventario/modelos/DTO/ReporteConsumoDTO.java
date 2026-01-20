package jpd.sistemafacinv.sistemadefacturacioneinventario.modelos.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReporteConsumoDTO {
    private LocalDate fechaInicio; // Fecha inicial del reporte
    private LocalDate fechaFin; // Fecha final del reporte
    private List<ItemConsumoDTO> items; // Lista de ingredientes con sus métricas

    // Métricas resumen del reporte completo
    private Double totalValorDiferencia; // Suma de todas las diferencias en dinero
    private Double porcentajeDiferenciaTotal; // % de diferencia vs consumo total
    private int totalIngredientesAnalizados;
}
