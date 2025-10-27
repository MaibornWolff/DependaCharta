# 2. Use a Rich Domain Model in Visualization

Date: 2025-08-28
Involved: Adrian Endrich, Jens Dinges, Stephan Schneider, Abdelkader Degachi

## Status

Accepted

## Context

So far the behavior of the visualization's domain model was scattered around services and loose functions in files. It resembles 
an anemic domain model. State can easily become inconsistent according to domain rules since there is no model that 
combines structure and behaviour. See [Martin Fowler's blog](https://martinfowler.com/bliki/AnemicDomainModel.html)

## Decision

We implement a rich domain model that combines structure and behaviour.

## Consequences

Increased usage of "class" might come with syntax overhead.

```typescript
// production code
class Foo {
    constructor(
        public readonly answer: number,
        public readonly label: string
    ) {}

    inc(): number {
        return this.answer + 1
    }
}

// test code
namespace Foo {
    export function build(overrides: Partial<Foo> = {}): Foo {
        const defaults = new Foo(
            42,
            'World'
        )
        return defaults.sets(overrides)
    }
}

// `sets` can be inlined
interface Foo {
    sets(overrides: Partial<Foo>): Foo
}

Foo.prototype.sets = function(overrides: Partial<Foo>) {
    const data = {...this, ...overrides}
    return new Foo(
        data.answer,
        data.label
    )
}

// usage
const fooDefault = Foo.build() // Foo: {"answer": 42, "label": "World"}
const foo23 = Foo.build({answer: 23}) // Foo: {"answer": 23, "label": "World"}
```