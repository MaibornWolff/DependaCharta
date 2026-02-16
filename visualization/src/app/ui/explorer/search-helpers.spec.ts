import {collectAllNodes, deduplicateByParent, stripLeafSuffix} from './search-helpers';
import {GraphNode} from '../../model/GraphNode';
import * as GraphNodeTest from '../../model/GraphNode.spec';

describe('search-helpers', () => {
  const createMockNode = (id: string, children: GraphNode[] = [], parent?: GraphNode): GraphNode => {
    const node = GraphNodeTest.GraphNode.build({id, children, label: id.split('.').pop() || id});
    if (parent) {
      node.parent = parent;
    }
    children.forEach(child => child.parent = node);
    return node;
  };

  describe('collectAllNodes', () => {
    it('should collect a single leaf node', () => {
      // Given
      const leaf = createMockNode('ClassA');

      // When
      const result = collectAllNodes([leaf], []);

      // Then
      expect(result.map(n => n.id)).toEqual(['ClassA']);
    });

    it('should collect container and children in depth-first order', () => {
      // Given
      const child1 = createMockNode('pkg.ClassA');
      const child2 = createMockNode('pkg.ClassB');
      const container = createMockNode('pkg', [child1, child2]);

      // When
      const result = collectAllNodes([container], []);

      // Then
      expect(result.map(n => n.id)).toEqual(['pkg', 'pkg.ClassA', 'pkg.ClassB']);
    });

    it('should collect nested containers depth-first', () => {
      // Given
      const leaf = createMockNode('a.b.Leaf');
      const innerPkg = createMockNode('a.b', [leaf]);
      const outerPkg = createMockNode('a', [innerPkg]);

      // When
      const result = collectAllNodes([outerPkg], []);

      // Then
      expect(result.map(n => n.id)).toEqual(['a', 'a.b', 'a.b.Leaf']);
    });

    it('should exclude hidden nodes', () => {
      // Given
      const child1 = createMockNode('pkg.ClassA');
      const child2 = createMockNode('pkg.ClassB');
      const container = createMockNode('pkg', [child1, child2]);

      // When
      const result = collectAllNodes([container], ['pkg.ClassA']);

      // Then
      expect(result.map(n => n.id)).toEqual(['pkg', 'pkg.ClassB']);
    });

    it('should exclude node when ancestor is hidden', () => {
      // Given
      const leaf = createMockNode('a.b.Leaf');
      const innerPkg = createMockNode('a.b', [leaf]);
      const outerPkg = createMockNode('a', [innerPkg]);

      // When
      const result = collectAllNodes([outerPkg], ['a.b']);

      // Then
      expect(result.map(n => n.id)).toEqual(['a']);
    });

    it('should return empty list for empty input', () => {
      // When
      const result = collectAllNodes([], []);

      // Then
      expect(result).toEqual([]);
    });

    it('should collect multiple root trees', () => {
      // Given
      const leaf1 = createMockNode('a.Leaf');
      const root1 = createMockNode('a', [leaf1]);
      const root2 = createMockNode('b');

      // When
      const result = collectAllNodes([root1, root2], []);

      // Then
      expect(result.map(n => n.id)).toEqual(['a', 'a.Leaf', 'b']);
    });
  });

  describe('stripLeafSuffix', () => {
    it('should remove :leaf suffix', () => {
      expect(stripLeafSuffix('com.example.MyClass:leaf')).toBe('com.example.MyClass');
    });

    it('should return id unchanged when no :leaf suffix', () => {
      expect(stripLeafSuffix('com.example.services')).toBe('com.example.services');
    });

    it('should only strip trailing :leaf', () => {
      expect(stripLeafSuffix('leaf:leaf')).toBe('leaf');
    });
  });

  describe('deduplicateByParent', () => {
    it('should return single node as-is', () => {
      // Given
      const ids = ['com.example.ClassA'];

      // When
      const result = deduplicateByParent(ids);

      // Then
      expect(result).toEqual(['com.example.ClassA']);
    });

    it('should remove child when parent is also in list', () => {
      // Given
      const ids = ['com.example.services', 'com.example.services.UserService'];

      // When
      const result = deduplicateByParent(ids);

      // Then
      expect(result).toEqual(['com.example.services']);
    });

    it('should remove grandchild when grandparent is in list', () => {
      // Given
      const ids = ['com', 'com.example', 'com.example.ClassA'];

      // When
      const result = deduplicateByParent(ids);

      // Then
      expect(result).toEqual(['com']);
    });

    it('should keep unrelated nodes', () => {
      // Given
      const ids = ['com.pkg1.ClassA', 'com.pkg2.ClassB'];

      // When
      const result = deduplicateByParent(ids);

      // Then
      expect(result).toEqual(['com.pkg1.ClassA', 'com.pkg2.ClassB']);
    });

    it('should return empty list for empty input', () => {
      // When
      const result = deduplicateByParent([]);

      // Then
      expect(result).toEqual([]);
    });

    it('should handle mixed parents and non-parents', () => {
      // Given
      const ids = ['a.b', 'a.b.c', 'x.y'];

      // When
      const result = deduplicateByParent(ids);

      // Then
      expect(result).toEqual(['a.b', 'x.y']);
    });
  });
});
