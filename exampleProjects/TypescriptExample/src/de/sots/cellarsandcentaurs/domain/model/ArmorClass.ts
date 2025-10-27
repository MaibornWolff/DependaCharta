import { CreatureUtil } from '../../application';

export class ArmorClass {
    private description: string;
    private base: number;
    private bonus: number;
    private total: number;

    constructor(base: number, bonus: number, description: string = CreatureUtil.STANDARD_ARMOR_CLASS_DESCRIPTION) {
        this.description = description;
        this.base = base;
        this.bonus = bonus;
        this.total = base + bonus;
    }

    public getBase(): number {
        return this.base;
    }

    public setBase(base: number): void {
        this.base = base;
    }

    public getBonus(): number {
        return this.bonus;
    }

    public setBonus(bonus: number): void {
        this.bonus = bonus;
    }

    public getTotal(): number {
        return this.total;
    }

    public setTotal(total: number): void {
        this.total = total;
    }
}