package br.com.cloudtech.models;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record Producao(long id, long maquinaId, BigDecimal consumoKw, LocalTime horarioAcionamento,
                       int pecasProduzidas, LocalDateTime registradoEm) {
    public record Dados(@NotNull @Positive Long maquinaId,
                        @NotNull @DecimalMin("0") BigDecimal consumoKw,
                        @NotNull LocalTime horarioAcionamento,
                        @NotNull @PositiveOrZero Integer pecasProduzidas) {
    }
}
