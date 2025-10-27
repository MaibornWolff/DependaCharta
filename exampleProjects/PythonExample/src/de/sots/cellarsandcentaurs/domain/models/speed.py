from dataclasses import dataclass

@dataclass
class Speed:
    speed: int

    def get_speed(self):
        return self.speed

    def set_speed(self, speed):
        self.speed = speed
