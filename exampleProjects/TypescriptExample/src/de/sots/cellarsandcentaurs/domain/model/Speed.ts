export class Speed {
    private speed: number;

    constructor(speed: number) {
        this.speed = speed;
    }

    public getSpeed(): number {
        return this.speed;
    }

    public setSpeed(speed: number): void {
        this.speed = speed;
    }
}