package persistence

import (
	"database/sql"
	"github.com/google/uuid"
	_ "github.com/lib/pq"
	"github.com/sots/cellarsandcentaurs/src/de/sots/cellarsandcentaurs/domain/model"
)

type CreatureRepository struct {
	db *sql.DB
}

func NewCreatureRepository(db *sql.DB) *CreatureRepository {
	return &CreatureRepository{
		db: db,
	}
}

func (cr *CreatureRepository) Save(creature *model.Creature) error {
	entity := cr.toEntity(creature)

	query := `
		INSERT INTO creatures (id, creature_type, armor_class, current_hp, maximum_hp, walking_speed, flying_speed, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
		ON CONFLICT (id) DO UPDATE SET
			creature_type = EXCLUDED.creature_type,
			armor_class = EXCLUDED.armor_class,
			current_hp = EXCLUDED.current_hp,
			maximum_hp = EXCLUDED.maximum_hp,
			walking_speed = EXCLUDED.walking_speed,
			flying_speed = EXCLUDED.flying_speed,
			updated_at = EXCLUDED.updated_at
	`

	_, err := cr.db.Exec(query,
		entity.ID,
		entity.CreatureType,
		entity.ArmorClass,
		entity.CurrentHP,
		entity.MaximumHP,
		entity.WalkingSpeed,
		entity.FlyingSpeed,
		entity.CreatedAt,
		entity.UpdatedAt,
	)

	return err
}

func (cr *CreatureRepository) FindById(id *model.CreatureId) (*model.Creature, error) {
	query := `SELECT id, creature_type, armor_class, current_hp, maximum_hp, walking_speed, flying_speed, created_at, updated_at FROM creatures WHERE id = $1`

	var entity CreatureEntity
	err := cr.db.QueryRow(query, uuid.MustParse(id.String())).Scan(
		&entity.ID,
		&entity.CreatureType,
		&entity.ArmorClass,
		&entity.CurrentHP,
		&entity.MaximumHP,
		&entity.WalkingSpeed,
		&entity.FlyingSpeed,
		&entity.CreatedAt,
		&entity.UpdatedAt,
	)

	if err != nil {
		if err == sql.ErrNoRows {
			return nil, model.NewNoSuchCreatureError(id)
		}
		return nil, err
	}

	return cr.toDomain(&entity), nil
}

func (cr *CreatureRepository) FindAll() ([]*model.Creature, error) {
	query := `SELECT id, creature_type, armor_class, current_hp, maximum_hp, walking_speed, flying_speed, created_at, updated_at FROM creatures`

	rows, err := cr.db.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var creatures []*model.Creature
	for rows.Next() {
		var entity CreatureEntity
		err := rows.Scan(
			&entity.ID,
			&entity.CreatureType,
			&entity.ArmorClass,
			&entity.CurrentHP,
			&entity.MaximumHP,
			&entity.WalkingSpeed,
			&entity.FlyingSpeed,
			&entity.CreatedAt,
			&entity.UpdatedAt,
		)
		if err != nil {
			return nil, err
		}

		creatures = append(creatures, cr.toDomain(&entity))
	}

	return creatures, nil
}

func (cr *CreatureRepository) Delete(id *model.CreatureId) error {
	query := `DELETE FROM creatures WHERE id = $1`
	_, err := cr.db.Exec(query, uuid.MustParse(id.String()))
	return err
}

func (cr *CreatureRepository) toEntity(creature *model.Creature) *CreatureEntity {
	entity := NewCreatureEntity(uuid.MustParse(creature.GetId().String()))
	entity.CreatureType = creature.GetType().String()

	if creature.GetArmorClass() != nil {
		entity.ArmorClass = creature.GetArmorClass().Value()
	}

	if creature.GetHitPoints() != nil {
		entity.CurrentHP = creature.GetHitPoints().Current()
		entity.MaximumHP = creature.GetHitPoints().Maximum()
	}

	speeds := creature.GetSpeeds()
	if walkingSpeed, exists := speeds[model.Walking]; exists {
		entity.WalkingSpeed = walkingSpeed.Value()
	}
	if flyingSpeed, exists := speeds[model.Flying]; exists {
		speed := flyingSpeed.Value()
		entity.FlyingSpeed = &speed
	}

	return entity
}

func (cr *CreatureRepository) toDomain(entity *CreatureEntity) *model.Creature {
	id, _ := model.NewCreatureIdFromString(entity.ID.String())
	creature := model.NewCreatureWithType(id, model.CreatureType(entity.CreatureType))

	if entity.ArmorClass > 0 {
		armorClass, _ := model.NewArmorClass(entity.ArmorClass)
		creature.SetArmorClass(armorClass)
	}

	if entity.MaximumHP > 0 {
		hitPoints, _ := model.NewHitPoints(entity.MaximumHP)
		if entity.CurrentHP != entity.MaximumHP {
			hitPoints.TakeDamage(entity.MaximumHP - entity.CurrentHP)
		}
		creature.SetHitPoints(hitPoints)
	}

	if entity.WalkingSpeed > 0 {
		walkingSpeed, _ := model.NewSpeed(entity.WalkingSpeed)
		creature.AddSpeed(model.Walking, walkingSpeed)
	}

	if entity.FlyingSpeed != nil && *entity.FlyingSpeed > 0 {
		flyingSpeed, _ := model.NewSpeed(*entity.FlyingSpeed)
		creature.AddSpeed(model.Flying, flyingSpeed)
	}

	return creature
}
