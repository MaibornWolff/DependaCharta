package model

import "fmt"

type HitPoints struct {
	current int
	maximum int
}

func NewHitPoints(maximum int) (*HitPoints, error) {
	if maximum <= 0 {
		return nil, fmt.Errorf("maximum hit points must be positive, got %d", maximum)
	}
	return &HitPoints{
		current: maximum,
		maximum: maximum,
	}, nil
}

func (hp *HitPoints) Current() int {
	return hp.current
}

func (hp *HitPoints) Maximum() int {
	return hp.maximum
}

func (hp *HitPoints) IsAlive() bool {
	return hp.current > 0
}

func (hp *HitPoints) TakeDamage(damage int) {
	hp.current -= damage
	if hp.current < 0 {
		hp.current = 0
	}
}

func (hp *HitPoints) Heal(healing int) {
	hp.current += healing
	if hp.current > hp.maximum {
		hp.current = hp.maximum
	}
}
