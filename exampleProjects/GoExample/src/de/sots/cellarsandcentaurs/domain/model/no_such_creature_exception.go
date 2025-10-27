package model

import "fmt"

type NoSuchCreatureError struct {
	creatureId *CreatureId
}

func NewNoSuchCreatureError(creatureId *CreatureId) *NoSuchCreatureError {
	return &NoSuchCreatureError{
		creatureId: creatureId,
	}
}

func (e *NoSuchCreatureError) Error() string {
	return fmt.Sprintf("no creature found with id: %s", e.creatureId.String())
}

func (e *NoSuchCreatureError) CreatureId() *CreatureId {
	return e.creatureId
}
