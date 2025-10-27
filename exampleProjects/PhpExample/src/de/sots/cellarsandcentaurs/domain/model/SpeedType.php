<?php

namespace de\sots\cellarsandcentaurs\domain\model;

enum SpeedType: string
{
    case WALKING = 'walking';
    case FLYING = 'flying';
    case SWIMMING = 'swimming';
    case CLIMBING = 'climbing';
    case BURROWING = 'burrowing';
}
