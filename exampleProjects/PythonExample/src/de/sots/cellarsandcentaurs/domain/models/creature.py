from de.sots.cellarsandcentaurs.application.creature_facade import CreatureFacade
from ..models import *

@fightable
class Creature:
    def __init__(self, creature_id: CreatureId, creature_type: CreatureType = CreatureFacade.STANDARD_CREATURE_TYPE):
        self.id = creature_id
        self.type = creature_type
        self.armor_class = None
        self.speeds = {}
        self.hit_points = None

    @property
    def id(self):
        return self._id

    @id.setter
    def id(self, creature_id: CreatureId):
        self._id = creature_id

    @property
    def type(self):
        return self._type

    @type.setter
    def type(self, creature_type: CreatureType):
        self._type = creature_type

    @property
    def armor_class(self):
        return self._armor_class

    @armor_class.setter
    def armor_class(self, armor_class: ArmorClass):
        self._armor_class = armor_class

    @property
    def speeds(self):
        return self._speeds

    @speeds.setter
    def speeds(self, speeds: dict[SpeedType, Speed]):
        self._speeds = speeds

    @property
    def hit_points(self):
        return self._hit_points

    @hit_points.setter
    def hit_points(self, hit_points: HitPoints):
        self._hit_points = hit_points