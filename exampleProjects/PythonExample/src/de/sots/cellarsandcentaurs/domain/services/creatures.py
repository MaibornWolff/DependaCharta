from abc import ABC, abstractmethod

import de.sots.cellarsandcentaurs.domain.models
import de.sots.cellarsandcentaurs.domain.models.creature as crt
from ..models.no_such_creature_exception import NoSuchCreatureException

class Creatures(ABC):

    @abstractmethod
    def save(self, creature: crt.Creature):
        """Save a creature to the persistence layer."""
        pass

    @abstractmethod
    def find(self, creature_id: de.sots.cellarsandcentaurs.domain.models.CreatureId) -> Creature:
        """Find a creature by its ID.

        Raises:
            NoSuchCreatureException: If the creature is not found.
        """
        pass