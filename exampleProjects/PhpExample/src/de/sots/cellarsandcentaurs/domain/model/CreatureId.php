<?php
namespace de\sots\cellarsandcentaurs\domain\model;

class CreatureId
{
    public string $id;

    function __construct(string $id)
    {
        $this->id = $id;
    }

    public function getId(): string
    {
        return $this->id;
    }
}
