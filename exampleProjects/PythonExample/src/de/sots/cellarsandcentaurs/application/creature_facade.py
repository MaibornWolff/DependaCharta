import uuid
import de.sots.cellarsandcentaurs.domain.models as models
from de.sots.cellarsandcentaurs.domain.services.creature_service import CreatureService


class CreatureFacade:
    STANDARD_CREATURE_TYPE = models.CreatureType.MONSTROSITY

    def __init__(self, creature_service: CreatureService):
        self.creature_service = creature_service

    def create(self, type: models.CreatureType, walk_speed: models.Speed, fly_speed: models.Speed,
               swim_speed: models.Speed, burrow_speed: models.Speed, climb_speed: models.Speed,
               armor_class: models.ArmorClass, hit_points_value: int):
        creature_id = models.CreatureId(uuid.uuid4())
        creature = models.Creature(creature_id)

        # Set properties using the assumed methods
        creature.armor_class = armor_class
        creature.hit_points = models.HitPoints.init(hit_points_value)
        creature.type = type
        creature.speeds = {
            models.SpeedType.WALKING: walk_speed,
            models.SpeedType.FLYING: fly_speed,
            models.SpeedType.SWIMMING: swim_speed,
            models.SpeedType.BURROWING: burrow_speed,
            models.SpeedType.CLIMBING: climb_speed
        }

        self.creature_service.save(creature)