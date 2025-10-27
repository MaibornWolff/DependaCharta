from dataclasses import dataclass
import uuid

@dataclass(frozen=True)
class CreatureId:
    id: uuid.UUID