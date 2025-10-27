<?php

namespace de\sots\cellarsandcentaurs\adapter\persistence;

class CreatureEntity
{
    public string $id;

    public function __construct(?int $id)
    {
        if($id !== null) {
            $this->id = $id;
        } else {
            $this->id = "ididid";
        }
    }

    public function getId(): string
    {
        return $this->id;
    }
}
