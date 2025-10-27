<?php
namespace de\sots\cellarsandcentaurs\domain\service;

use de\sots\cellarsandcentaurs\domain\model\Creature;
use de\sots\cellarsandcentaurs\domain\model\CreatureId;

interface Creatures
{
    public function save(Creature $creature);
    public function find(CreatureId $id): Creature;
}
