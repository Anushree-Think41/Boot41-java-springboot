package com.calculator.controller;

import com.calculator.dto.ApiResponse;
import com.calculator.dto.CalculationRequest;
import com.calculator.dto.CalculationResult;
import com.calculator.enums.Operation;
import com.calculator.service.CalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/calculator")
@RequiredArgsConstructor
public class CalculatorController {

    private final CalculatorService calculatorService;

    /**
     * Performs an arithmetic calculation.
     *
     * @param operation query param — one of: add, subtract, multiply, divide, modulo, power
     * @param request   request body containing operandA and operandB
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<CalculationResult>> calculate(
            @RequestParam Operation operation,
            @Valid @RequestBody CalculationRequest request) {

        log.info("Calculate request: operation={}, operandA={}, operandB={}",
                operation.name().toLowerCase(), request.getOperandA(), request.getOperandB());

        CalculationResult result = calculatorService.calculate(
                operation, request.getOperandA(), request.getOperandB());

        return ResponseEntity.ok(ApiResponse.<CalculationResult>builder()
                .success(true)
                .message(operation.name().toLowerCase() + " performed successfully")
                .data(result)
                .build());
    }
}
