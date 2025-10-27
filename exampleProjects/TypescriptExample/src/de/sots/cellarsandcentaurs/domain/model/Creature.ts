import { CreatureId } from './CreatureId';
import CreatureType from './CreatureType';
import { ArmorClass } from './ArmorClass';
import { SpeedType } from './SpeedType';
import { Speed } from './Speed';
import { HitPoints } from './HitPoints';
import {CreatureFacade} from "../../application";

export class Creature {
    id: CreatureId;
    type: CreatureType;
    armorClass: ArmorClass | any;
    speeds: Map<SpeedType, Speed> | any;
    hitPoints: HitPoints | any;
    //creatureFacade: CreatureFacade | any;

    constructor(id: CreatureId, type: CreatureType = CreatureFacade.STANDARD_CREATURE_TYPE) {
        this.id = id;
        this.type = type;
        //this.creatureFacade = creatureFacade;
    }

    public getId(): CreatureId {
        return this.id;
    }

    public setId(id: CreatureId): void {
        this.id = id;
    }

    public getType(): CreatureType {
        return this.type;
    }

    public setType(type: CreatureType): void {
        this.type = type;
    }

    public getArmorClass(): ArmorClass {
        return this.armorClass;
    }

    public setArmorClass(armorClass: ArmorClass): void {
        this.armorClass = armorClass;
    }

    public getSpeeds(): Map<SpeedType, Speed> {
        return this.speeds;
    }

    public setSpeeds(speeds: Map<SpeedType, Speed>): void {
        this.speeds = speeds;
    }

    public getHitPoints(): HitPoints {
        return this.hitPoints;
    }

    public setHitPoints(hitPoints: HitPoints): void {
        this.hitPoints = hitPoints;
    }
}