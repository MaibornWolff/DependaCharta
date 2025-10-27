<?php
namespace de\sots\cellarsandcentaurs\domain\model;

use de\sots\cellarsandcentaurs\application\CreatureFacade;

class Creature implements Fightable
{
    private CreatureId $id;
    private CreatureType $type;
    private ?ArmorClass $armorClass = null;
    private array $speeds = [];
    private ?HitPoints $hitPoints = null;

    public function __construct(CreatureId $id, CreatureType $type = CreatureFacade::STANDARD_CREATURE_TYPE)
    {
        $this->id = $id;
        $this->type = $type;
    }

    public function getId(): CreatureId
    {
        return $this->id;
    }

    public function setId(CreatureId $id): void
    {
        $this->id = $id;
    }

    public function getType(): CreatureType
    {
        return $this->type;
    }

    public function setType(CreatureType $type): void
    {
        $this->type = $type;
    }

    public function getArmorClass(): ?ArmorClass
    {
        return $this->armorClass;
    }

    public function setArmorClass(ArmorClass $armorClass): void
    {
        $this->armorClass = $armorClass;
    }

    public function getSpeeds(): ?array
    {
        return $this->speeds;
    }

    public function setSpeeds(array $speeds): void
    {
        $this->speeds = [];

        foreach ($speeds as $speedKey => $speed) {
            $this->setSpeed(SpeedType::from($speedKey), $speed);
        }
    }

    private function setSpeed(SpeedType $speedType, Speed $speed ):void
    {
        $this->speeds[] = $speedType -> $speed;
    }

    public function getHitPoints(): ?HitPoints
    {
        return $this->hitPoints;
    }

    public function setHitPoints(HitPoints $hitPoints): void
    {
        $this->hitPoints = $hitPoints;
    }
}
