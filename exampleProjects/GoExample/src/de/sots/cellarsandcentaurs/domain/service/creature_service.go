package service

import (
	"github.com/sots/cellarsandcentaurs/src/de/sots/cellarsandcentaurs/domain/model"
	"golang.org/x/crypto/bcrypt"
)

type CreatureService struct {
	creatures Creatures
}

func NewCreatureService(creatures Creatures) *CreatureService {
	return &CreatureService{
		creatures: creatures,
	}
}

func (cs *CreatureService) Save(creature *model.Creature) error {
	return cs.creatures.Save(creature)
}

func (cs *CreatureService) FindById(id *model.CreatureId) (*model.Creature, error) {
	return cs.creatures.FindById(id)
}

func (cs *CreatureService) FindAll() ([]*model.Creature, error) {
	return cs.creatures.FindAll()
}

func (cs *CreatureService) Delete(id *model.CreatureId) error {
	return cs.creatures.Delete(id)
}

func (cs *CreatureService) HashCreatureData(data string) (string, error) {
	hashedBytes, err := bcrypt.GenerateFromPassword([]byte(data), bcrypt.DefaultCost)
	if err != nil {
		return "", err
	}
	return string(hashedBytes), nil
}

func (cs *CreatureService) ValidateCreature(creature *model.Creature) error {
	if creature.GetId() == nil {
		return &model.NoSuchCreatureError{}
	}

	if !creature.GetType().IsValid() {
		return &model.NoSuchCreatureError{}
	}

	return nil
}
