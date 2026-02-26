import logging

import uvicorn
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.api.router import api_router
from app.core.config import settings
from app.core.logging_config import configure_logging
from app.exceptions.calculator_exceptions import CalculatorError
from app.models.response import ApiResponse

configure_logging()

logger = logging.getLogger(__name__)

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="A simple calculator REST API built with FastAPI.",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.include_router(api_router)


# ------------------------------------------------------------------ #
# Exception handlers                                                   #
# ------------------------------------------------------------------ #

@app.exception_handler(CalculatorError)
async def calculator_error_handler(request: Request, exc: CalculatorError) -> JSONResponse:
    logger.warning("Calculator error on %s: %s", request.url.path, exc.detail)
    return JSONResponse(
        status_code=400,
        content=ApiResponse(success=False, message=exc.detail, data=None).model_dump(),
    )


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.error("Unhandled error on %s: %s", request.url.path, str(exc), exc_info=True)
    return JSONResponse(
        status_code=500,
        content=ApiResponse(
            success=False, message="An unexpected error occurred", data=None
        ).model_dump(),
    )


# ------------------------------------------------------------------ #
# Health check                                                         #
# ------------------------------------------------------------------ #

@app.get("/health", tags=["Health"])
def health_check() -> dict:
    """Returns 200 OK when the service is running."""
    return {"status": "ok", "version": settings.app_version}


# ------------------------------------------------------------------ #
# Entry point                                                          #
# ------------------------------------------------------------------ #

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug,
        log_level=settings.log_level.lower(),
    )
