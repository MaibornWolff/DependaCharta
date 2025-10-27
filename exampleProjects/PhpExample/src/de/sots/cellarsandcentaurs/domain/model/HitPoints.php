<?php
namespace de\sots\cellarsandcentaurs\domain\model;

class HitPoints
{
    private int $current;
    private int $max;
    private int $temporary;

    public function __construct(int $current, int $max, int $temporary = 0)
    {
        $this->current = $current;
        $this->max = $max;
        $this->temporary = $temporary;
    }

    public static function init(int $max): HitPoints
    {
        return new self($max, $max, 0);
    }

    public function getCurrent(): int
    {
        return $this->current;
    }

    public function getMax(): int
    {
        return $this->max;
    }

    public function getTemporary(): int
    {
        return $this->temporary;
    }

    public function setCurrent(int $current): void
    {
        $this->current = $current;
    }

    public function setMax(int $max): void
    {
        $this->max = $max;
    }

    public function setTemporary(int $temporary): void
    {
        $this->temporary = $temporary;
    }
}
