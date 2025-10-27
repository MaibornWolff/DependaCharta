export class HitPoints {
    current: number;
    max: number;
    temporary: number;

    constructor(current: number, max: number, temporary: number = 0) {
        this.current = current;
        this.max = max;
        this.temporary = temporary;
    }

    static init(max: number): HitPoints {
        return new HitPoints(max, max, 0);
    }
}