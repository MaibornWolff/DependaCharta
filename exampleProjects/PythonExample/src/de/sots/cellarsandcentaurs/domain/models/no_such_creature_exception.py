class NoSuchCreatureException(Exception):
    def __init__(self, creature_id):
        super().__init__(f"No creature found with id: {creature_id.id}")