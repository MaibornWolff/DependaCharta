<?php
namespace de\sots\cellarsandcentaurs\adapter\persistence;

require_once '../../domain/model/Creature.php';
include_once '../../domain/model/CreatureId.php';
include '../../domain/service/Creatures.php';

use de\sots\cellarsandcentaurs\domain\model\{Creature, CreatureId, NoSuchCreatureException};

class PersistedCreature implements Creatures
{
    private CreatureRepository $repository;

    function __construct(CreatureRepository $repository)
    {
        $this->repository = $repository;
    }

    public function save(Creature $creature): void
    {
        $this->repository->save(new CreatureEntity($creature->getId()->id));
    }

    public function find(CreatureId $id): Creature
    {
        $creatureEntity = $this->repository->findOne($id->id);
        if ($creatureEntity === null) {
            throw new NoSuchCreatureException($id);
        }
        $id = $creatureEntity->getId();
        $creatureId = new CreatureId($id);
        return new Creature($creatureId);
    }
}
