package persistence

import (
	"database/sql/driver"
	"github.com/google/uuid"
	"time"
)

type CreatureEntity struct {
	ID           uuid.UUID `db:"id"`
	CreatureType string    `db:"creature_type"`
	ArmorClass   int       `db:"armor_class"`
	CurrentHP    int       `db:"current_hp"`
	MaximumHP    int       `db:"maximum_hp"`
	WalkingSpeed int       `db:"walking_speed"`
	FlyingSpeed  *int      `db:"flying_speed"`
	CreatedAt    time.Time `db:"created_at"`
	UpdatedAt    time.Time `db:"updated_at"`
}

func NewCreatureEntity(id uuid.UUID) *CreatureEntity {
	now := time.Now()
	return &CreatureEntity{
		ID:        id,
		CreatedAt: now,
		UpdatedAt: now,
	}
}

func (ce *CreatureEntity) Value() (driver.Value, error) {
	return ce.ID.String(), nil
}

func (ce *CreatureEntity) GetID() uuid.UUID {
	return ce.ID
}

func (ce *CreatureEntity) SetID(id uuid.UUID) {
	ce.ID = id
	ce.UpdatedAt = time.Now()
}
