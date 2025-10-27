import { CreatureRepository } from './CreatureRepository';
import { CreatureEntity } from './CreatureEntity';
import { Creatures } from '../../domain/service/Creatures';
import { Creature } from '../../domain/model/Creature';
import { CreatureId } from '../../domain/model/CreatureId';
import { NoSuchCreatureException } from '../../domain/model/NoSuchCreatureException';
import { CreatureFacade } from "../../application";

export class PersistedCreatures implements Creatures {
    constructor(
        private repository: CreatureRepository
    ) {}

    async save(creature: Creature): Promise<void> {
        await this.repository.save(new CreatureEntity(creature.getId().id));
    }

    async find(id: CreatureId): Promise<Creature> {
        const creatureEntity = await this.repository.findOne(id.id);
        if (!creatureEntity) {
            throw new NoSuchCreatureException(id);
        }
        return new Creature(new CreatureId(creatureEntity.id), CreatureFacade.STANDARD_CREATURE_TYPE);
    }
}