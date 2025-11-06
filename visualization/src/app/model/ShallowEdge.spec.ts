import {ShallowEdge} from "./ShallowEdge"

declare module './ShallowEdge' {
  namespace ShallowEdge {
    function build(overrides?: Partial<ShallowEdge>): ShallowEdge
  }
}

ShallowEdge.build = function(overrides: Partial<ShallowEdge> = {}): ShallowEdge {
  const defaults = new ShallowEdge(
    "source", // source
    "target", // target
    "source-target", // id
    1, // weight
    false, // isCyclic
    'usage', // type
    false // isPointingUpwards
  )

  return defaults.copy(overrides)
}

export { ShallowEdge }
