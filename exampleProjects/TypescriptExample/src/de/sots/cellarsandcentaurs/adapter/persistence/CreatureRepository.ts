import {CreatureEntity as Entity} from "./CreatureEntity";

export class CreatureRepository {
    private creatures: Map<string, Entity> = new Map();

    public async save(creature: any): Promise<void> {
        this.creatures.set(creature.id, creature);
    }

    public findOne(id: string): any {
        return this.creatures.get(id);
    }

    public findAll(): any[] {
        return Array.from(this.creatures.values());
    }
}
