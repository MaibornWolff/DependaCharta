export abstract class ValueObject<T> {
  constructor(from: Total<T>) {
    Object.assign(this, from)
  }

  copy(overrides: Partial<T> = {}): T {
    const merged = Object.assign({}, this, overrides) as T
    return new (this.constructor as new (fields: T) => T)(merged)
  }
}

export type Total<T> = {
  [K in keyof T as T[K] extends Function ? never : K]: T[K]
}

