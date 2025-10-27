from dataclasses import dataclass

@dataclass(frozen=True)
class HitPoints:
    current: int
    max: int
    temporary: int = 0  # Default value for temporary

    @classmethod
    def init(cls, max_value):
        """Class method to create initial HitPoints with max value."""
        return cls(max_value, max_value, 0)