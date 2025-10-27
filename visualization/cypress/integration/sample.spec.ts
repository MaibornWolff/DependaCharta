describe('File Loader Integration Test', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('Should select a file and verify root nodes', () => {
    // Given
    // We assume that the java-example.cg.json is always loaded as default and that it is the last thing our NgOnInit is doing so we make sure it's finished before next steps
    cy.intercept('GET', '/resources/java-example.cg.json').as('getAnalysisFile');
    cy.wait('@getAnalysisFile');
    cy.get('#file').should('be.visible');

    // Act
    const filePath = 'cypress/fixtures/analysis.cg.json';
    cy.get('#file').selectFile(filePath, {force: true});

    // Assert
    cy.get('.cytoscape-container')
      .should('be.visible')
      .within(() => {
        cy.get('.non-compound-node')
          .should('exist')
          .and('have.length.at.least', 1)
          .should('have.length', 2)
          .then((nodes) => {
            expect(nodes.eq(0)).to.contain.text('CYPRESS_CONFIG');
            expect(nodes.eq(1)).to.contain.text('SRC');
          });
      });
  });

  it('Should expand a root node', () => {
    // Given
    cy.get('#file').should('be.visible');
    const filePath = 'cypress/fixtures/analysis.cg.json';
    cy.get('#file').selectFile(filePath, {force: true});

    // Act
    cy.get('.non-compound-node').contains('SRC').click();

    // Assert
    cy.get('.compound-node')
      .should('exist')
      .within(() => {
        cy.get('.header .label').should('have.text', 'SRC');
      });

    cy.get('.non-compound-node')
      .should('have.length', 2)
      .then((nodes) => {
        expect(nodes.eq(0)).to.contain.text('CYPRESS_CONFIG');
        expect(nodes.eq(1)).to.contain.text('APP');
      });
  });

});
