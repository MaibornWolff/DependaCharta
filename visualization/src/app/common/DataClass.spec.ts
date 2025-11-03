import {DataClass} from "./DataClass";

describe('DataClass', () => {
    class Foo extends DataClass<Foo> {
        declare readonly a: string
        declare readonly b: number

        foo() { return 'Hello World!' }
    }

    const foo = Foo.new({a: 'Hi!', b: 42})

    it('should create', () => {
        expect(Foo.new({a: 'Hi!', b: 42})).toEqual(foo)
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
