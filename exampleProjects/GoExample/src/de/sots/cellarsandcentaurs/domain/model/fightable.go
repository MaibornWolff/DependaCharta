package model

type Fightable interface {
	GetArmorClass() *ArmorClass
	GetHitPoints() *HitPoints
	TakeDamage(damage int)
	IsAlive() bool
}
