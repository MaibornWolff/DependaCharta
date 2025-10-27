package model

type SpeedType string

const (
	Walking   SpeedType = "WALKING"
	Flying    SpeedType = "FLYING"
	Swimming  SpeedType = "SWIMMING"
	Climbing  SpeedType = "CLIMBING"
	Burrowing SpeedType = "BURROWING"
)

func (st SpeedType) String() string {
	return string(st)
}

func (st SpeedType) IsValid() bool {
	switch st {
	case Walking, Flying, Swimming, Climbing, Burrowing:
		return true
	default:
		return false
	}
}
