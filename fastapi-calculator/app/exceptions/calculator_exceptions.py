class CalculatorError(Exception):
    """Base exception for all calculator errors."""

    def __init__(self, detail: str) -> None:
        self.detail = detail
        super().__init__(detail)


class DivisionByZeroError(CalculatorError):
    """Raised when division or modulo by zero is attempted."""

    def __init__(self) -> None:
        super().__init__("Division by zero is not allowed")


class InvalidOperandError(CalculatorError):
    """Raised when an operand value is mathematically invalid for an operation."""

    def __init__(self, detail: str = "Invalid operand provided") -> None:
        super().__init__(detail)
