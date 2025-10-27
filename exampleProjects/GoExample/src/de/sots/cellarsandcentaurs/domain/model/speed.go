package model

import "fmt"

type Speed struct {
	value int
	unit  string
}

func NewSpeed(value int) (*Speed, error) {
	if value < 0 {
		return nil, fmt.Errorf("speed cannot be negative, got %d", value)
	}
	return &Speed{
		value: value,
		unit:  "feet",
	}, nil
}

func (s *Speed) Value() int {
	return s.value
}

func (s *Speed) Unit() string {
	return s.unit
}

func (s *Speed) String() string {
	return fmt.Sprintf("%d %s", s.value, s.unit)
}
