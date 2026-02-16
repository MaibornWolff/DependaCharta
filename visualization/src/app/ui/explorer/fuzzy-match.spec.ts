import {fuzzyMatch, fuzzyHighlight} from './fuzzy-match';

describe('fuzzyMatch', () => {
  describe('substring matching', () => {
    it('should match exact substring', () => {
      expect(fuzzyMatch('Clone', 'CloneJabRef')).toBe(true);
    });

    it('should match case-insensitively', () => {
      expect(fuzzyMatch('clone', 'CloneJabRef')).toBe(true);
    });

    it('should match substring in the middle', () => {
      expect(fuzzyMatch('Jab', 'CloneJabRef')).toBe(true);
    });

    it('should match full string', () => {
      expect(fuzzyMatch('MyClass', 'MyClass')).toBe(true);
    });

    it('should match empty query against any target', () => {
      expect(fuzzyMatch('', 'anything')).toBe(true);
    });
  });

  describe('word-boundary matching', () => {
    it('should match camelCase initials', () => {
      // C(ommand) L(ine) E(xecutor)
      expect(fuzzyMatch('CLE', 'CommandLineExecutor')).toBe(true);
    });

    it('should match initials after dots', () => {
      // c(om) e(xample) s(ervices)
      expect(fuzzyMatch('ces', 'com.example.services')).toBe(true);
    });

    it('should match word start plus continuation', () => {
      // K(ey) Col(lision)
      expect(fuzzyMatch('keycol', 'KeyCollisionException')).toBe(true);
    });

    it('should match hop between word boundaries with continuation', () => {
      // Clo(ne) J(ab) R(ef)
      expect(fuzzyMatch('clojr', 'CloneJabRef')).toBe(true);
    });

    it('should match initials after underscores', () => {
      expect(fuzzyMatch('mf', 'my_function')).toBe(true);
    });

    it('should match initials after hyphens', () => {
      expect(fuzzyMatch('mf', 'my-function')).toBe(true);
    });
  });

  describe('non-matches', () => {
    it('should reject scattered characters that are not at word boundaries', () => {
      // 'clone' scattered across AllowedToUseStandardStreams is not valid
      expect(fuzzyMatch('clone', 'AllowedToUseStandardStreams')).toBe(false);
    });

    it('should reject when chars are out of order', () => {
      expect(fuzzyMatch('ba', 'abc')).toBe(false);
    });

    it('should reject when query has chars not in target', () => {
      expect(fuzzyMatch('xyz', 'abc')).toBe(false);
    });

    it('should reject non-empty query against empty target', () => {
      expect(fuzzyMatch('a', '')).toBe(false);
    });

    it('should reject when word-boundary path cannot complete', () => {
      // c→C(ollision) ok, but l not at next position or word boundary
      expect(fuzzyMatch('clone', 'KeyCollisionException')).toBe(false);
    });

    it('should reject scattered match through citation-like names', () => {
      expect(fuzzyMatch('clone', 'CitationLookupResult')).toBe(false);
    });
  });
});

describe('fuzzyHighlight', () => {
  it('should return whole string unmatched for empty query', () => {
    expect(fuzzyHighlight('', 'UserService')).toEqual([
      {text: 'UserService', isMatch: false}
    ]);
  });

  it('should highlight contiguous substring match', () => {
    expect(fuzzyHighlight('Clone', 'CloneJabRef')).toEqual([
      {text: 'Clone', isMatch: true},
      {text: 'JabRef', isMatch: false}
    ]);
  });

  it('should highlight substring match in the middle', () => {
    expect(fuzzyHighlight('Jab', 'CloneJabRef')).toEqual([
      {text: 'Clone', isMatch: false},
      {text: 'Jab', isMatch: true},
      {text: 'Ref', isMatch: false}
    ]);
  });

  it('should highlight word-boundary initials', () => {
    // C(ommand) L(ine) E(xecutor)
    expect(fuzzyHighlight('CLE', 'CommandLineExecutor')).toEqual([
      {text: 'C', isMatch: true},
      {text: 'ommand', isMatch: false},
      {text: 'L', isMatch: true},
      {text: 'ine', isMatch: false},
      {text: 'E', isMatch: true},
      {text: 'xecutor', isMatch: false}
    ]);
  });

  it('should highlight word-boundary start plus continuation', () => {
    // K-e-y at Key, then hop to E-x at Exception
    expect(fuzzyHighlight('keyex', 'KeyCollisionException')).toEqual([
      {text: 'Key', isMatch: true},
      {text: 'Collision', isMatch: false},
      {text: 'Ex', isMatch: true},
      {text: 'ception', isMatch: false}
    ]);
  });

  it('should prefer substring over word-boundary', () => {
    // 'Clone' is a substring — should highlight contiguously, not scattered
    expect(fuzzyHighlight('clone', 'CloneJabRef')).toEqual([
      {text: 'Clone', isMatch: true},
      {text: 'JabRef', isMatch: false}
    ]);
  });

  it('should highlight entire string when full match', () => {
    expect(fuzzyHighlight('abc', 'abc')).toEqual([
      {text: 'abc', isMatch: true}
    ]);
  });

  it('should return unmatched for no match', () => {
    expect(fuzzyHighlight('xyz', 'abc')).toEqual([
      {text: 'abc', isMatch: false}
    ]);
  });

  it('should highlight word-boundary match across dot separators', () => {
    // c(om) e(xample) — not a substring of target
    expect(fuzzyHighlight('CE', 'com.example')).toEqual([
      {text: 'c', isMatch: true},
      {text: 'om.', isMatch: false},
      {text: 'e', isMatch: true},
      {text: 'xample', isMatch: false}
    ]);
  });
});
