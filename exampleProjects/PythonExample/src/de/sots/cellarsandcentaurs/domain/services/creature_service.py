from ..models.creature import Creature
from .creatures import Creatures

class CreatureService:
    def __init__(self, creatures: Creatures):
        self.creatures = creatures

    def save(self, creature: Creature):
        self.creatures.save(creature)