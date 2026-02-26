from typing import Generic, Optional, TypeVar

from pydantic import BaseModel

T = TypeVar("T")


class CalculationResult(BaseModel):
    operand_a: float
    operand_b: float
    operation: str
    result: float


class ApiResponse(BaseModel, Generic[T]):
    success: bool
    message: str
    data: Optional[T] = None
