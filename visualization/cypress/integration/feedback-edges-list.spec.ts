describe('Feedback Edges List', () => {
  beforeEach(() => {
    cy.visit('/');
    // Wait for the graph to render by checking for visible nodes
    cy.get('.non-compound-node', {timeout: 10000}).should('be.visible');
  });

  const loadFeedbackEdgesTestFixture = () => {
    cy.get('#file').should('be.visible');
    const filePath = 'cypress/fixtures/feedback-edges-test.cg.json';
    cy.get('#file').selectFile(filePath, {force: true});
    // Wait for the graph to render with our fixture data by checking for node names unique to our fixture
    cy.get('.non-compound-node').should('contain.text', 'moduleA');
  };

  it('Should display feedback edges overlay', () => {
    // Given
    loadFeedbackEdgesTestFixture();

    // Then
    cy.get('.feedback-edges-overlay').should('be.visible');
  });

  it('Should show count badge when feedback edges exist', () => {
    // Given
    loadFeedbackEdgesTestFixture();

    // Then
    cy.get('.feedback-edges-overlay .count-badge').should('contain', '1');
  });

  it('Should expand and collapse the overlay when header is clicked', () => {
    // Given
    loadFeedbackEdgesTestFixture();

    // Then: starts collapsed
    cy.get('.feedback-edges-overlay').should('not.have.class', 'expanded');

    // When: click to expand
    cy.get('.feedback-edges-overlay .header').click();

    // Then: is expanded
    cy.get('.feedback-edges-overlay').should('have.class', 'expanded');
    cy.get('.feedback-edges-overlay .edge-list').should('be.visible');

    // When: click to collapse
    cy.get('.feedback-edges-overlay .header').click();

    // Then: is collapsed
    cy.get('.feedback-edges-overlay').should('not.have.class', 'expanded');
  });

  it('Should display edge item with source, target and weight', () => {
    // Given
    loadFeedbackEdgesTestFixture();

    // When
    cy.get('.feedback-edges-overlay .header').click();

    // Then
    cy.get('.feedback-edges-overlay .edge-item').should('have.length', 1);
    cy.get('.feedback-edges-overlay .edge-item .source').should('contain', 'ClassB');
    cy.get('.feedback-edges-overlay .edge-item .target').should('contain', 'ClassA');
    cy.get('.feedback-edges-overlay .edge-item .weight').should('contain', '3');
  });

  it('Should show sort dropdown when expanded', () => {
    // Given
    loadFeedbackEdgesTestFixture();

    // When
    cy.get('.feedback-edges-overlay .header').click();

    // Then
    cy.get('.feedback-edges-overlay .sort-container select').should('be.visible');
  });

  it('Should show empty state when no feedback edges exist', () => {
    // Given
    cy.get('#file').should('be.visible');
    const filePath = 'cypress/fixtures/analysis.cg.json';
    cy.get('#file').selectFile(filePath, {force: true});
    cy.get('.non-compound-node').should('contain.text', 'cypress_config');

    // When
    cy.get('.feedback-edges-overlay .header').click();

    // Then
    cy.get('.feedback-edges-overlay .empty-state').should('be.visible');
    cy.get('.feedback-edges-overlay .empty-state').should('contain', 'No feedback edges found');
  });
});
