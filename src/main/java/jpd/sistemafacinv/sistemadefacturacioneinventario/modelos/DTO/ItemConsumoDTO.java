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
public class ItemConsumoDTO {
    // Datos básicos del ingrediente
    private Long ingredienteId;
    private String nombreIngrediente;
    private String unidadMedida;

    // MÉTRICAS CALCULADAS (lo importante del reporte)
    private Double stockInicial; // Stock al inicio del período
    private Double comprasPeriodo; // Compras recibidas en el período
    private Double consumoVentas; // Consumo por ventas en el período
    private Double stockTeoricoFinal; // = stockInicial + compras - consumo
    private Double stockRealFinal; // Stock físico al final (del cierre diario)
    private Double diferencia; // = stockReal - stockTeorico (puede ser + o -)
    private Double costoUnitarioPromedio; // Costo promedio del ingrediente
    private Double valorDiferencia;
}
