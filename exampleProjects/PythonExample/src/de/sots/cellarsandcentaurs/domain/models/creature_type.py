from enum import Enum

class CreatureType(Enum):
    HUMANOID = "humanoid"
    BEAST = "beast"
    UNDEAD = "undead"
    DRAGON = "dragon"
    GIANT = "giant"
    FIEND = "fiend"
    CELESTIAL = "celestial"
    ELEMENTAL = "elemental"
    ABERRATION = "aberration"
    CONSTRUCT = "construct"
    OOZE = "ooze"
    PLANT = "plant"
    MONSTROSITY = "monstrosity"
    FEY = "fey"

__all__ = ['CreatureType']