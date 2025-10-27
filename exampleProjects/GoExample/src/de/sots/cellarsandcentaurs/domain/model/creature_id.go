package model

import "github.com/google/uuid"

type CreatureId struct {
	id uuid.UUID
}

func NewCreatureId() *CreatureId {
	return &CreatureId{
		id: uuid.New(),
	}
}

func NewCreatureIdFromString(idStr string) (*CreatureId, error) {
	id, err := uuid.Parse(idStr)
	if err != nil {
		return nil, err
	}
	return &CreatureId{id: id}, nil
}

func (c *CreatureId) String() string {
	return c.id.String()
}

func (c *CreatureId) Equals(other *CreatureId) bool {
	return c.id == other.id
}
