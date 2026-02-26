import logging

from fastapi import APIRouter, Depends, Query

from app.models.enums import Operation
from app.models.request import CalculationRequest
from app.models.response import ApiResponse, CalculationResult
from app.services.calculator_service import CalculatorService

router = APIRouter(prefix="/calculator", tags=["Calculator"])

logger = logging.getLogger(__name__)


def get_calculator_service() -> CalculatorService:
    return CalculatorService()


@router.post(
    "/calculate",
    response_model=ApiResponse[CalculationResult],
    summary="Perform a calculation",
    description=(
        "Accepts two operands in the request body and an `operation` query parameter "
        "that specifies which arithmetic operation to perform."
    ),
)
def calculate(
    operation: Operation = Query(..., description="Arithmetic operation to perform"),
    request: CalculationRequest = ...,
    service: CalculatorService = Depends(get_calculator_service),
) -> ApiResponse[CalculationResult]:
    logger.info(
        "calculate request: %s %s %s",
        request.operand_a,
        operation.value,
        request.operand_b,
    )
    result = service.calculate(operation, request.operand_a, request.operand_b)
    return ApiResponse(
        success=True,
        message=f"{operation.value.capitalize()} performed successfully",
        data=result,
    )
