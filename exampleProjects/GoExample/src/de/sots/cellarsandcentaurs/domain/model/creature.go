package model

const StandardCreatureType = Humanoid

type Creature struct {
	id           *CreatureId
	creatureType CreatureType
	armorClass   *ArmorClass
	speeds       map[SpeedType]*Speed
	hitPoints    *HitPoints
}

func NewCreature(id *CreatureId) *Creature {
	return NewCreatureWithType(id, StandardCreatureType)
}

func NewCreatureWithType(id *CreatureId, creatureType CreatureType) *Creature {
	return &Creature{
		id:           id,
		creatureType: creatureType,
		speeds:       make(map[SpeedType]*Speed),
	}
}

func (c *Creature) GetId() *CreatureId {
	return c.id
}

func (c *Creature) SetId(id *CreatureId) {
	c.id = id
}

func (c *Creature) GetType() CreatureType {
	return c.creatureType
}

func (c *Creature) SetType(creatureType CreatureType) {
	c.creatureType = creatureType
}

func (c *Creature) GetArmorClass() *ArmorClass {
	return c.armorClass
}

func (c *Creature) SetArmorClass(armorClass *ArmorClass) {
	c.armorClass = armorClass
}

func (c *Creature) GetSpeeds() map[SpeedType]*Speed {
	return c.speeds
}

func (c *Creature) SetSpeeds(speeds map[SpeedType]*Speed) {
	c.speeds = speeds
}

func (c *Creature) AddSpeed(speedType SpeedType, speed *Speed) {
	c.speeds[speedType] = speed
}

func (c *Creature) GetHitPoints() *HitPoints {
	return c.hitPoints
}

func (c *Creature) SetHitPoints(hitPoints *HitPoints) {
	c.hitPoints = hitPoints
}

func (c *Creature) TakeDamage(damage int) {
	if c.hitPoints != nil {
		c.hitPoints.TakeDamage(damage)
	}
}

func (c *Creature) IsAlive() bool {
	if c.hitPoints == nil {
		return false
	}
	return c.hitPoints.IsAlive()
}
