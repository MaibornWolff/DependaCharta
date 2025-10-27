package application

import (
	"fmt"
	"github.com/sots/cellarsandcentaurs/src/de/sots/cellarsandcentaurs/domain/model"
	"github.com/stretchr/testify/assert"
	"strings"
	"testing"
)

type CreatureUtil struct{}

func NewCreatureUtil() *CreatureUtil {
	return &CreatureUtil{}
}

func (cu *CreatureUtil) FormatCreatureInfo(creature *model.Creature) string {
	var info []string

	info = append(info, fmt.Sprintf("ID: %s", creature.GetId().String()))
	info = append(info, fmt.Sprintf("Type: %s", creature.GetType().String()))

	if creature.GetArmorClass() != nil {
		info = append(info, fmt.Sprintf("Armor Class: %s", creature.GetArmorClass().String()))
	}

	if creature.GetHitPoints() != nil {
		info = append(info, fmt.Sprintf("Hit Points: %d/%d",
			creature.GetHitPoints().Current(),
			creature.GetHitPoints().Maximum()))
	}

	speeds := creature.GetSpeeds()
	if len(speeds) > 0 {
		var speedStrings []string
		for speedType, speed := range speeds {
			speedStrings = append(speedStrings, fmt.Sprintf("%s: %s", speedType.String(), speed.String()))
		}
		info = append(info, fmt.Sprintf("Speeds: %s", strings.Join(speedStrings, ", ")))
	}

	return strings.Join(info, "\n")
}

func (cu *CreatureUtil) CalculateChallengeRating(creature *model.Creature) float64 {
	baseRating := 0.5

	if creature.GetArmorClass() != nil {
		baseRating += float64(creature.GetArmorClass().Value()) * 0.1
	}

	if creature.GetHitPoints() != nil {
		baseRating += float64(creature.GetHitPoints().Maximum()) * 0.05
	}

	switch creature.GetType() {
	case model.Dragon:
		baseRating += 5.0
	case model.Fiend:
		baseRating += 3.0
	case model.Undead:
		baseRating += 2.0
	case model.Beast:
		baseRating += 1.0
	}

	return baseRating
}

func (cu *CreatureUtil) IsCreatureEqual(c1, c2 *model.Creature) bool {
	if c1 == nil || c2 == nil {
		return c1 == c2
	}

	return c1.GetId().Equals(c2.GetId()) && c1.GetType() == c2.GetType()
}

func (cu *CreatureUtil) AssertCreatureEquals(t *testing.T, expected, actual *model.Creature) {
	assert.NotNil(t, expected, "Expected creature should not be nil")
	assert.NotNil(t, actual, "Actual creature should not be nil")

	if expected != nil && actual != nil {
		assert.True(t, expected.GetId().Equals(actual.GetId()), "Creature IDs should match")
		assert.Equal(t, expected.GetType(), actual.GetType(), "Creature types should match")
	}
}

func (cu *CreatureUtil) CreateCreatureBuilder() *CreatureBuilder {
	return NewCreatureBuilder()
}

type CreatureBuilder struct {
	creature *model.Creature
}

func NewCreatureBuilder() *CreatureBuilder {
	id := model.NewCreatureId()
	return &CreatureBuilder{
		creature: model.NewCreature(id),
	}
}

func (cb *CreatureBuilder) WithType(creatureType model.CreatureType) *CreatureBuilder {
	cb.creature.SetType(creatureType)
	return cb
}

func (cb *CreatureBuilder) WithArmorClass(ac int) *CreatureBuilder {
	armorClass, _ := model.NewArmorClass(ac)
	cb.creature.SetArmorClass(armorClass)
	return cb
}

func (cb *CreatureBuilder) WithHitPoints(hp int) *CreatureBuilder {
	hitPoints, _ := model.NewHitPoints(hp)
	cb.creature.SetHitPoints(hitPoints)
	return cb
}

func (cb *CreatureBuilder) WithWalkingSpeed(speed int) *CreatureBuilder {
	walkingSpeed, _ := model.NewSpeed(speed)
	cb.creature.AddSpeed(model.Walking, walkingSpeed)
	return cb
}

func (cb *CreatureBuilder) Build() *model.Creature {
	return cb.creature
}
