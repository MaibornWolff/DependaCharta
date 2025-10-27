package persistence

import (
	"database/sql"
	"github.com/sots/cellarsandcentaurs/src/de/sots/cellarsandcentaurs/domain/model"
	"github.com/sots/cellarsandcentaurs/src/de/sots/cellarsandcentaurs/domain/service"
)

type PersistedCreatures struct {
	repository *CreatureRepository
}

func NewPersistedCreatures(db *sql.DB) service.Creatures {
	return &PersistedCreatures{
		repository: NewCreatureRepository(db),
	}
}

func (pc *PersistedCreatures) Save(creature *model.Creature) error {
	return pc.repository.Save(creature)
}

func (pc *PersistedCreatures) FindById(id *model.CreatureId) (*model.Creature, error) {
	return pc.repository.FindById(id)
}

func (pc *PersistedCreatures) FindAll() ([]*model.Creature, error) {
	return pc.repository.FindAll()
}

func (pc *PersistedCreatures) Delete(id *model.CreatureId) error {
	return pc.repository.Delete(id)
}
