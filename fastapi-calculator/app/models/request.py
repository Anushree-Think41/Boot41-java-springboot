from pydantic import BaseModel, model_validator


class CalculationRequest(BaseModel):
    operand_a: float
    operand_b: float

    model_config = {
        "json_schema_extra": {
            "examples": [
                {"operand_a": 10, "operand_b": 5}
            ]
        }
    }

    @model_validator(mode="after")
    def check_finite_numbers(self) -> "CalculationRequest":
        import math
        for field_name, value in [("operand_a", self.operand_a), ("operand_b", self.operand_b)]:
            if not math.isfinite(value):
                raise ValueError(f"{field_name} must be a finite number")
        return self
