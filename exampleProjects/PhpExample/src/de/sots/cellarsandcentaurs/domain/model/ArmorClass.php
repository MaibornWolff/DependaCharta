<?php

namespace de\sots\cellarsandcentaurs\domain\model;

use de\sots\cellarsandcentaurs\application\CreatureUtil;

class ArmorClass
{
    private string $description;
    private int $base;
    private int $bonus;
    private int $total;

    public function __construct(int $base, int $bonus, ?string $description = null)
    {
        $this->description = $description ?? CreatureUtil::STANDARD_ARMOR_CLASS_DESCRIPTION;
        $this->base = $base;
        $this->bonus = $bonus;
        $this->total = $base + $bonus;
    }

    public function getBase(): int
    {
        return $this->base;
    }

    public function setBase(int $base): void
    {
        $this->base = $base;
        $this->updateTotal();
    }

    public function getBonus(): int
    {
        return $this->bonus;
    }

    public function setBonus(int $bonus): void
    {
        $this->bonus = $bonus;
        $this->updateTotal();
    }

    public function getTotal(): int
    {
        return $this->total;
    }

    public function getDescription(): string
    {
        return $this->description;
    }

    public function setDescription(string $description): void
    {
        $this->description = $description;
    }

    private function updateTotal(): void
    {
        $this->total = $this->base + $this->bonus;
    }
}
