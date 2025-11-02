import {ValueObject} from "../common/ValueObject"

export class ShallowEdge extends ValueObject<ShallowEdge> {
  declare source: string
  declare target: string
  declare id: string
  declare weight: number
  declare isCyclic: boolean
  declare type: string
}
