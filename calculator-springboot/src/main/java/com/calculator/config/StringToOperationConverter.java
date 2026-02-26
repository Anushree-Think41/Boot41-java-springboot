package com.calculator.config;

import com.calculator.enums.Operation;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Converts the ?operation= query parameter string to the Operation enum,
 * accepting both lowercase ("add") and uppercase ("ADD") values.
 */
@Component
public class StringToOperationConverter implements Converter<String, Operation> {

    @Override
    public Operation convert(String value) {
        try {
            return Operation.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            String accepted = Arrays.stream(Operation.values())
                    .map(op -> op.name().toLowerCase())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    String.format("Invalid operation '%s'. Accepted values: %s", value, accepted));
        }
    }
}
