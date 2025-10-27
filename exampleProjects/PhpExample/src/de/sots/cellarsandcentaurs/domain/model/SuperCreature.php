<?php
namespace de\sots\cellarsandcentaurs\domain\model;

class SuperCreature extends Creature {
    private $specialPower;

    public function __construct(CreatureId $id, CreatureType $type = CreatureFacade::STANDARD_CREATURE_TYPE, $specialPower = "Super Strength") {
        parent::__construct($id, $type);
        $this->specialPower = $specialPower;
    }

    public function getSpecialPower(): string {
        return $this->specialPower;
    }

    public function setSpecialPower($specialPower): void {
        $this->specialPower = $specialPower;
    }

    public function displayInfo(): string {
        $info = "ID: ".$this->getId()->idAsString().", Type: ".$this->getType()->getType().", Special Power: ".$this->getSpecialPower();
        $hitPoints = $this->getHitPoints();
        if ($hitPoints) {
            $info .= ", Hit Points: ".$hitPoints->getHP();
        }
        $armorClass = $this->getArmorClass();
        if ($armorClass) {
            $info .= ", Armor Class: ".$armorClass->getArmorValue();
        }
        return $info;
    }
}