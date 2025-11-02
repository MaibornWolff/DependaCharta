import {ShallowEdge} from "./ShallowEdge"

declare module './ShallowEdge' {
  namespace ShallowEdge {
    function build(overrides?: Partial<ShallowEdge>): ShallowEdge
  }
}

ShallowEdge.build = function(overrides: Partial<ShallowEdge> = {}): ShallowEdge {
  const defaults = ShallowEdge.new({
    id: "source-target",
    isCyclic: false,
    source: "source",
    target: "target",
    weight: 1,
    type: 'usage'
  })

  return defaults.copy(overrides)
}

export { ShallowEdge }
