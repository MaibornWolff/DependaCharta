import type {ShallowEdge} from './ShallowEdge';

namespace ShallowEdge {
  export function build(overrides: Partial<ShallowEdge> = {}): ShallowEdge {
    const defaults: ShallowEdge = {
      id: "source-target",
      isCyclic: false,
      source: "source",
      target: "target",
      weight: 1,
      type: 'usage'
    }

    return { ...defaults, ...overrides }
  }
}
export { ShallowEdge }