import { CreatureService } from '../domain/service/CreatureService';
import { Creature } from '../domain/model/Creature';
import { CreatureId } from '../domain/model/CreatureId';
import { CreatureType } from '../domain/model/CreatureType';
import { HitPoints } from '../domain/model/HitPoints';
import { Speed } from '../domain/model/Speed';
import { SpeedType } from '../domain/model/SpeedType';
import { ArmorClass } from '../domain/model/ArmorClass';


export class CreatureFacade {
    private creatureService: CreatureService;
    public static readonly STANDARD_CREATURE_TYPE: CreatureType = CreatureType.MONSTROSITY;

    constructor(creatureService: CreatureService) {
        this.creatureService = creatureService;
    }

    public create(
        type: CreatureType,
        walkSpeed: Speed,
        flySpeed: Speed,
        swimSpeed: Speed,
        burrowSpeed: Speed,
        climbSpeed: Speed,
        armorClass: ArmorClass,
        hitPointsValue: number
    ): Creature {
        const creature = new Creature(new CreatureId(this.generateUUID()));
        creature.setArmorClass(armorClass);
        creature.setHitPoints(HitPoints.init(hitPointsValue));
        creature.setType(type);

        creature.setSpeeds(new Map()
            .set(SpeedType.WALKING, walkSpeed)
            .set(SpeedType.FLYING, flySpeed)
            .set(SpeedType.SWIMMING, swimSpeed)
            .set(SpeedType.BURROWING, burrowSpeed)
            .set(SpeedType.CLIMBING, climbSpeed));

        this.creatureService.save(creature);
        return creature
    }

    private generateUUID(): string {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
            const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }
}