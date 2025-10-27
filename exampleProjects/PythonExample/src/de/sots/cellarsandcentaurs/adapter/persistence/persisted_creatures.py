from de.sots.cellarsandcentaurs.domain.models import Creature, CreatureId as CrId, NoSuchCreatureException

from de.sots.cellarsandcentaurs.domain.services.creatures import Creatures as Crs

from ..persistence.creature_entity import CreatureEntity

class PersistedCreatures(Crs):
    def __init__(self, repository):
        self.repository = repository

    def save(self, creature: Creature):
        # Assuming CreatureEntity is the persistence equivalent within the system
        entity = CreatureEntity(creature.id.id)
        self.repository.save(entity)

    def find(self, creature_id: CrId) -> Creature:
        entity = next(
            (entity for entity in self.repository.find_by_id(creature_id.id)
             if entity.get_id() == creature_id.id), None
        )

        if entity is None:
            raise NoSuchCreatureException(creature_id)

        return Creature(CrId(entity.get_id()))