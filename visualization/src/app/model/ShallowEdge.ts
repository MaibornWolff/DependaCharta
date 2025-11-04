export class ShallowEdge {
  constructor(
    readonly source: string,
    readonly target: string,
    readonly id: string,
    readonly weight: number,
    readonly isCyclic: boolean,
    readonly type: string
  ) {}

  copy(overrides: Partial<ShallowEdge>) {
    return Object.assign(this, overrides)
  }  
}
