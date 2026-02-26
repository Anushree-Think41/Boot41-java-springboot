package com.calculator.service;

import com.calculator.dto.CalculationResult;
import com.calculator.enums.Operation;
import com.calculator.exception.DivisionByZeroException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CalculatorService {

    public CalculationResult calculate(Operation operation, Double operandA, Double operandB) {
        log.info("Calculating: {} {} {}", operandA, operation.name().toLowerCase(), operandB);
        return switch (operation) {
            case ADD      -> buildResult(operandA, operandB, "addition",       operandA + operandB);
            case SUBTRACT -> buildResult(operandA, operandB, "subtraction",    operandA - operandB);
            case MULTIPLY -> buildResult(operandA, operandB, "multiplication", operandA * operandB);
            case DIVIDE   -> {
                if (operandB == 0) throw new DivisionByZeroException();
                yield buildResult(operandA, operandB, "division", operandA / operandB);
            }
            case MODULO   -> {
                if (operandB == 0) throw new DivisionByZeroException();
                yield buildResult(operandA, operandB, "modulo", operandA % operandB);
            }
            case POWER    -> buildResult(operandA, operandB, "power", Math.pow(operandA, operandB));
        };
    }

    private CalculationResult buildResult(
            Double operandA, Double operandB, String operation, Double result) {
        log.debug("{}: {} {} = {}", operation, operandA, operandB, result);
        return CalculationResult.builder()
                .operandA(operandA)
                .operandB(operandB)
                .operation(operation)
                .result(result)
                .build();
    }
}
