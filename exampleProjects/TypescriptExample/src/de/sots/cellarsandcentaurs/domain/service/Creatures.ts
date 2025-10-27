import { Creature } from '../model/Creature';
import { CreatureId } from '../model/CreatureId';
import { NoSuchCreatureException } from '../model/NoSuchCreatureException';

export interface Creatures {
    save(creature: Creature): void;
    find(id: CreatureId): Creature | Promise<Creature>;  // Either returns a Creature or a Promise resolving to Creature
}