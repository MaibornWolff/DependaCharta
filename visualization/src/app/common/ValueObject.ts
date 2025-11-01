export abstract class ValueObject<T> {
  public constructor(from: Total<T>) {
    Object.assign(this, from)
  }

  static new<T>(this: new (from: Total<T>) => T, overrides: Total<T>): T {
    return new this(overrides)
  }

  copy(overrides: Partial<T> = {}): T {
    const merged = Object.assign({}, this, overrides) as T
    return new (this.constructor as new (fields: T) => T)(merged)
  }
}

export type Total<T> = {
  [K in keyof T as T[K] extends Function ? never : K]: T[K]
}

