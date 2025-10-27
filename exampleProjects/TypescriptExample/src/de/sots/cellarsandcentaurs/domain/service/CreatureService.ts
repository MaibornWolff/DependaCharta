import { Creatures } from './Creatures';
import { Creature } from '../model/Creature';

export class CreatureService {
    private creatures: Creatures;

    constructor(creatures: Creatures) {
        this.creatures = creatures;
    }

    public save(creature: Creature): void {
        this.creatures.save(creature);
    }
}