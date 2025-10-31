import {ValueObject} from "./ValueObject";

describe('ValueObject', () => {
    class Foo extends ValueObject<Foo> {
        declare readonly a: string
        declare readonly b: number

        foo() { return 'Hello World!' }
    }

    const foo = new Foo({a: 'Hi!', b: 42})

    it('should create', () => {
        expect(new Foo({a: 'Hi!', b: 42})).toEqual(foo)
    });

    it('should copy', () => {
        const copy = foo.copy()
        expect(copy).toEqual(foo)
        expect(copy).not.toBe(foo)
        expect(copy.foo()).toEqual(foo.foo())
    });

    it('should update', () => {
        const update = foo.copy({b: 23})
        expect(update.b).toEqual(23)
    });
})
