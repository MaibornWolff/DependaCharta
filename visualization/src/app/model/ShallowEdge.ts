export interface ShallowEdge {
  source: string,
  target: string,
  id: string,
  weight: number,
  isCyclic: boolean,
  type: string
}
