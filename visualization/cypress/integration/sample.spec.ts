describe('File Loader Integration Test', () => {
  beforeEach(() => {
    // Intercept must be registered before visit so Cypress captures the request fired during NgOnInit
    // ?file= param forces a deterministic file in both local and CI environments
    cy.intercept('GET', '/resources/java-example.cg.json').as('getAnalysisFile');
    cy.visit('/?file=/resources/java-example.cg.json');
    cy.wait('@getAnalysisFile');
  });

  it('Should select a file and verify root nodes', () => {
    // Given
    cy.get('#file').should('be.visible');

    // Act
    const filePath = 'cypress/fixtures/analysis.cg.json';
    cy.get('#file').selectFile(filePath, {force: true});

    // Assert
    cy.get('.cytoscape-container')
      .should('be.visible')
      .within(() => {
        cy.get('.non-compound-node')
          .should('have.length', 2)
          .then((nodes) => {
            expect(nodes.eq(0)).to.contain.text('cypress_config');
            expect(nodes.eq(1)).to.contain.text('src');
          });
      });
  });

  it('Should expand a root node', () => {
    // Given
    cy.get('#file').should('be.visible');
    const filePath = 'cypress/fixtures/analysis.cg.json';
    cy.get('#file').selectFile(filePath, {force: true});

    // Act
    cy.get('.non-compound-node').contains('src').click();

    // Assert
    cy.get('.compound-node')
      .should('exist')
      .within(() => {
        cy.get('.header .label').should('have.text', 'src');
      });

    cy.get('.non-compound-node')
      .should('have.length', 2)
      .then((nodes) => {
        expect(nodes.eq(0)).to.contain.text('cypress_config');
        expect(nodes.eq(1)).to.contain.text('app');
      });
  });

});
