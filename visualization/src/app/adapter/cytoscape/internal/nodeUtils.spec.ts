import {getAncestors, getSubtreeOf} from './nodeUtils';
import {ElementDefinitionBuilder, NodeCollectionBuilder} from './converter/CytoscapeModelBuilders.spec';
import cytoscape from 'cytoscape';

describe('GraphUtils', () => {
  // Destroy cy instance after every test, because otherwise the tests share state
  describe('getSubtreeOf', () => {
    it('returns node if there are no children', () => {
      const node = new ElementDefinitionBuilder().build()
      const cy = cytoscape()
      cy.add(node)

      const subtree = getSubtreeOf(cy.nodes()[0])

      expect(subtree[0].data().id).toEqual(node.data.id);
      cy.destroy()
    });

    it('returns node and all its children', () => {
      const parent = new ElementDefinitionBuilder()
        .build()
      const child = new ElementDefinitionBuilder()
        .setParent(parent.data.id!)
        .build()
      const grandchild = new ElementDefinitionBuilder()
        .setParent(child.data.id!)
        .build()

      const cy = cytoscape()
      cy.add([parent, child, grandchild])

      const parentNode = cy.nodes()
        .filter(node => node.data().id === parent.data.id)[0]

      const nodeIdsOfSubtree = getSubtreeOf(parentNode)
        .map(node => node.data().id)

      expect(nodeIdsOfSubtree).toEqual([grandchild.data.id, child.data.id, parent.data.id]);
      cy.destroy()
    });
  });

  describe('getAncestors', () => {
    it('returns all ancestors of node', () => {
      // given
      const parent = new ElementDefinitionBuilder()
        .setId("parent")
        .build()
      const child = new ElementDefinitionBuilder()
        .setId("child")
        .setParent(parent.data.id!)
        .build()
      const grandchild = new ElementDefinitionBuilder()
        .setId("grandchild")
        .setParent(child.data.id!)
        .build()

      const grandchildNode = new NodeCollectionBuilder()
        .addElementDefinition(parent)
        .addElementDefinition(child)
        .addElementDefinition(grandchild)
        .build()
        .filter(node => node.data().id === "grandchild")[0]

      // when
      const ancestors = getAncestors(grandchildNode)

      // then
      const expected = [child.data.id, parent.data.id]
      expect(ancestors.map(node => node.data().id)).toEqual(expected)
    })
  });
})
