<?php

namespace de\sots\cellarsandcentaurs\adapter\persistence;

use de\sots\cellarsandcentaurs\adapter\persistence\CreatureEntity as Entity;

class CreatureRepository
{
    private array $creatures = [];

    public function save($creature): void
    {
        $this->creatures[$creature->getId()] = $creature;
    }

    public function findOne(string $id): ?Entity
    {
        return $this->creatures[$id];
    }

    public function findAll(): array
    {
        return $this->creatures;
    }
}
