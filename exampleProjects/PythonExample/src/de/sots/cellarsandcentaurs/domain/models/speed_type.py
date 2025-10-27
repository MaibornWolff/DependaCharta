from enum import Enum

class SpeedType(Enum):
    WALKING = "walking"
    FLYING = "flying"
    SWIMMING = "swimming"
    CLIMBING = "climbing"
    BURROWING = "burrowing"

__all__ = ['SpeedType']