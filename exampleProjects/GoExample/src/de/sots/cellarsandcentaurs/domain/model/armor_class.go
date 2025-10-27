package model

import "fmt"

type ArmorClass struct {
	value int
}

func NewArmorClass(value int) (*ArmorClass, error) {
	if value < 0 || value > 30 {
		return nil, fmt.Errorf("armor class must be between 0 and 30, got %d", value)
	}
	return &ArmorClass{value: value}, nil
}

func (ac *ArmorClass) Value() int {
	return ac.value
}

func (ac *ArmorClass) String() string {
	return fmt.Sprintf("AC %d", ac.value)
}
