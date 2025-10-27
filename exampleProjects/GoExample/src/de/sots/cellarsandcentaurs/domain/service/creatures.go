package service

import "github.com/sots/cellarsandcentaurs/src/de/sots/cellarsandcentaurs/domain/model"

type Creatures interface {
	Save(creature *model.Creature) error
	FindById(id *model.CreatureId) (*model.Creature, error)
	FindAll() ([]*model.Creature, error)
	Delete(id *model.CreatureId) error
}
