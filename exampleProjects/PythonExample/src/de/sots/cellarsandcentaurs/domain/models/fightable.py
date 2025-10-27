def fightable(cls):
    """Decorator to mark a class as fightable."""
    cls._is_fightable = True
    return cls