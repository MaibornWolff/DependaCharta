<?php
namespace de\sots\cellarsandcentaurs\domain\model;

use Exception;

class NoSuchCreatureException extends Exception
{
    public function __construct(CreatureId $id)
    {
        parent::__construct($id->getId());
        $this->message = 'NoSuchCreatureException: ' . $id->getId();
    }
}
