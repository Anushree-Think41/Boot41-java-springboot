import logging
import sys

from app.core.config import settings


def configure_logging() -> None:
    """Configure application-wide logging format and level."""
    log_format = "%(asctime)s [%(levelname)s] %(name)s - %(message)s"
    date_format = "%Y-%m-%d %H:%M:%S"

    logging.basicConfig(
        level=settings.log_level.upper(),
        format=log_format,
        datefmt=date_format,
        stream=sys.stdout,
    )

    # Quiet down noisy third-party libraries
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
