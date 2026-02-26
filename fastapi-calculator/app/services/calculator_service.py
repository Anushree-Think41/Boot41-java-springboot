import logging

from app.exceptions.calculator_exceptions import DivisionByZeroError
from app.models.enums import Operation
from app.models.response import CalculationResult

logger = logging.getLogger(__name__)


class CalculatorService:
    """Handles all calculator operations and returns structured results."""

    def calculate(self, operation: Operation, operand_a: float, operand_b: float) -> CalculationResult:
        """Dispatch to the correct operation based on the Operation enum value."""
        dispatch = {
            Operation.ADD: self._add,
            Operation.SUBTRACT: self._subtract,
            Operation.MULTIPLY: self._multiply,
            Operation.DIVIDE: self._divide,
            Operation.MODULO: self._modulo,
            Operation.POWER: self._power,
        }
        return dispatch[operation](operand_a, operand_b)

    # ------------------------------------------------------------------ #
    # Private operation methods                                            #
    # ------------------------------------------------------------------ #

    def _add(self, operand_a: float, operand_b: float) -> CalculationResult:
        logger.debug("add: %s + %s", operand_a, operand_b)
        return self._build_result(operand_a, operand_b, "addition", operand_a + operand_b)

    def _subtract(self, operand_a: float, operand_b: float) -> CalculationResult:
        logger.debug("subtract: %s - %s", operand_a, operand_b)
        return self._build_result(operand_a, operand_b, "subtraction", operand_a - operand_b)

    def _multiply(self, operand_a: float, operand_b: float) -> CalculationResult:
        logger.debug("multiply: %s * %s", operand_a, operand_b)
        return self._build_result(operand_a, operand_b, "multiplication", operand_a * operand_b)

    def _divide(self, operand_a: float, operand_b: float) -> CalculationResult:
        logger.debug("divide: %s / %s", operand_a, operand_b)
        if operand_b == 0:
            raise DivisionByZeroError()
        return self._build_result(operand_a, operand_b, "division", operand_a / operand_b)

    def _modulo(self, operand_a: float, operand_b: float) -> CalculationResult:
        logger.debug("modulo: %s %% %s", operand_a, operand_b)
        if operand_b == 0:
            raise DivisionByZeroError()
        return self._build_result(operand_a, operand_b, "modulo", operand_a % operand_b)

    def _power(self, operand_a: float, operand_b: float) -> CalculationResult:
        logger.debug("power: %s ** %s", operand_a, operand_b)
        return self._build_result(operand_a, operand_b, "power", operand_a ** operand_b)

    # ------------------------------------------------------------------ #
    # Private helpers                                                      #
    # ------------------------------------------------------------------ #

    @staticmethod
    def _build_result(
        operand_a: float,
        operand_b: float,
        operation: str,
        result: float,
    ) -> CalculationResult:
        return CalculationResult(
            operand_a=operand_a,
            operand_b=operand_b,
            operation=operation,
            result=result,
        )
