import type {ShallowGraphEdge} from './ShallowGraphEdge';

namespace ShallowGraphEdge {
  export function build(overrides: Partial<ShallowGraphEdge> = {}): ShallowGraphEdge {
    const defaults: ShallowGraphEdge = {
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
export { ShallowGraphEdge }