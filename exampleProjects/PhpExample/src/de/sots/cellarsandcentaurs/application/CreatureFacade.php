<?php
namespace de\sots\cellarsandcentaurs\application;

require '../domain/model/Creature.php';

use de\sots\cellarsandcentaurs\domain\model\{ArmorClass,
    Creature,
    CreatureId,
    CreatureType,
    HitPoints,
    Speed,
    SpeedType};
use de\sots\cellarsandcentaurs\domain\service\CreatureService;


class CreatureFacade
{
    const STANDARD_CREATURE_TYPE = CreatureType::MONSTROSITY;
    private CreatureService $creatureService;

    function __construct(CreatureService $creatureService)
    {
       $this->$creatureService = $creatureService;
    }

    public function create(
        CreatureType $type,
        Speed $walkSpeed,
        Speed $flySpeed,
        Speed $swimSpeed,
        Speed $burrowSpeed,
        Speed $climbSpeed,
        ArmorClass $armorClass,
        int $hitPointsValue
    ) : Creature
    {
        $creature = new Creature(new CreatureId($this->generateUUID()));
        $creature->setArmorClass($armorClass);
        $creature->setHitPoints(HitPoints::init($hitPointsValue));
        $creature->setType($type);
        $creature->setSpeeds([
            SpeedType::WALKING->value => $walkSpeed,
            SpeedType::FLYING->value => $flySpeed,
            SpeedType::SWIMMING->value => $swimSpeed,
            SpeedType::BURROWING->value => $burrowSpeed,
            SpeedType::CLIMBING->value => $climbSpeed
        ]);

        $this->creatureService->save($creature);
        return $creature;
    }

    private function generateUUID(): string
    {
        $data = random_bytes(16);
        $data[6] = chr(ord($data[6]) & 0x0f | 0x40);
        $data[8] = chr(ord($data[8]) & 0x3f | 0x80);
        return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
    }
}
