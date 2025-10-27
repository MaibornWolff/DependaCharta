<?php

namespace de\sots\cellarsandcentaurs\domain\service;

use de\sots\cellarsandcentaurs\domain\model\Creature;
use de\sots\cellarsandcentaurs\domain\model\CreatureId;

class CreatureService
{
    private Creatures $creatures;

    public function __construct(Creatures $creatures)
    {
        $this->creatures = $creatures;
    }

    public function save(Creature $creature): void
    {
        $this->creatures->save($creature);
    }

    public function find(CreatureId $id): Creature
    {
        return $this->creatures->find($id);
    }
}
