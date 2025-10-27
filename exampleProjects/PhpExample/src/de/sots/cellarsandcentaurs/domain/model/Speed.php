<?php
namespace de\sots\cellarsandcentaurs\domain\model;

class Speed
{
    private int $speed;

    public function __construct(int $speed)
    {
        $this->speed = $speed;
    }

    public function getSpeed(): int
    {
        return $this->speed;
    }

    public function setSpeed(int $speed): void
    {
        $this->speed = $speed;
    }
}
