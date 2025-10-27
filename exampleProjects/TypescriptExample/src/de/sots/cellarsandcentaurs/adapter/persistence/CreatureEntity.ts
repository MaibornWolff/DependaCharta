export class CreatureEntity {
    id: string;

    constructor(id?: string) {
        if (id) {
            this.id = id;
        } else {
            this.id = "ididid";
        }
    }
}