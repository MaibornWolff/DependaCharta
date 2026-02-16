export interface HighlightSegment {
  text: string;
  isMatch: boolean;
}

export function fuzzyMatch(query: string, target: string): boolean {
  if (!query) {
    return true;
  }
  return substringMatchIndices(query, target) !== null
    || wordBoundaryMatchIndices(query, target) !== null;
}

export function fuzzyHighlight(query: string, target: string): HighlightSegment[] {
  if (!query) {
    return [{text: target, isMatch: false}];
  }
  const indices = substringMatchIndices(query, target)
    ?? wordBoundaryMatchIndices(query, target);
  if (!indices) {
    return [{text: target, isMatch: false}];
  }
  return buildSegments(target, new Set(indices));
}

function substringMatchIndices(query: string, target: string): number[] | null {
  const start = target.toLowerCase().indexOf(query.toLowerCase());
  if (start === -1) {
    return null;
  }
  return Array.from({length: query.length}, (_, i) => start + i);
}

function wordBoundaryMatchIndices(query: string, target: string): number[] | null {
  const lowerQuery = query.toLowerCase();
  const lowerTarget = target.toLowerCase();
  const indices: number[] = [];
  let queryIndex = 0;

  for (let i = 0; i < target.length && queryIndex < lowerQuery.length; i++) {
    if (lowerTarget[i] !== lowerQuery[queryIndex]) {
      continue;
    }
    const isContinuation = indices.length > 0 && i === indices[indices.length - 1] + 1;
    if (isContinuation || isWordBoundary(target, i)) {
      indices.push(i);
      queryIndex++;
    }
  }

  return queryIndex === lowerQuery.length ? indices : null;
}

function isWordBoundary(target: string, index: number): boolean {
  if (index === 0) {
    return true;
  }
  const prev = target[index - 1];
  if (prev === '.' || prev === '-' || prev === '_' || prev === ' ') {
    return true;
  }
  const current = target[index];
  return prev === prev.toLowerCase()
    && prev !== prev.toUpperCase()
    && current === current.toUpperCase()
    && current !== current.toLowerCase();
}

function buildSegments(target: string, matchIndices: Set<number>): HighlightSegment[] {
  const segments: HighlightSegment[] = [];
  let currentText = '';
  let currentIsMatch = matchIndices.has(0);

  for (let i = 0; i < target.length; i++) {
    const isMatch = matchIndices.has(i);
    if (isMatch !== currentIsMatch) {
      segments.push({text: currentText, isMatch: currentIsMatch});
      currentText = target[i];
      currentIsMatch = isMatch;
    } else {
      currentText += target[i];
    }
  }

  if (currentText) {
    segments.push({text: currentText, isMatch: currentIsMatch});
  }

  return segments;
}
