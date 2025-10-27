package model

type CreatureType string

const (
	Beast     CreatureType = "BEAST"
	Humanoid  CreatureType = "HUMANOID"
	Dragon    CreatureType = "DRAGON"
	Undead    CreatureType = "UNDEAD"
	Fiend     CreatureType = "FIEND"
	Celestial CreatureType = "CELESTIAL"
)

func (ct CreatureType) String() string {
	return string(ct)
}

func (ct CreatureType) IsValid() bool {
	switch ct {
	case Beast, Humanoid, Dragon, Undead, Fiend, Celestial:
		return true
	default:
		return false
	}
}
