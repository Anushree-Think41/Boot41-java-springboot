package com.calculator.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CalculationRequest {

    @NotNull(message = "operandA is required")
    private Double operandA;

    @NotNull(message = "operandB is required")
    private Double operandB;
}
