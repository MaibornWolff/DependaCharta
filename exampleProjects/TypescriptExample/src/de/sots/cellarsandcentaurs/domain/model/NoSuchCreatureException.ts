import { CreatureId } from './CreatureId';

export class NoSuchCreatureException extends Error {
    constructor(id: CreatureId) {
        super(id.id);
        this.name = 'NoSuchCreatureException';
    }
}