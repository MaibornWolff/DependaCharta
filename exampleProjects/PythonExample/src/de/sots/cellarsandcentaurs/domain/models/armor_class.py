from de.sots.cellarsandcentaurs.application.creature_util import CreatureUtil

class ArmorClass:
    def __init__(self, base, bonus, description=CreatureUtil.STANDARD_ARMOR_CLASS_DESCRIPTION):
        self.description = description
        self.base = base
        self.bonus = bonus

    @property
    def total(self):
        return self.base + self.bonus

    @property
    def base(self):
        return self._base

    @base.setter
    def base(self, value):
        self._base = value

    @property
    def bonus(self):
        return self._bonus

    @bonus.setter
    def bonus(self, value):
        self._bonus = value

    @property
    def description(self):
        return self._description

    @description.setter
    def description(self, value):
        self._description = value
