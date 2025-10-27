import uuid

class CreatureEntity:
    def __init__(self, id=None):
        self.id = id if id is not None else uuid.uuid4()

    def get_id(self):
        return self.id