describe('Auth critical paths', () => {
  it('logs in to dashboard and persists session across reload', () => {
    cy.login();
    cy.get('aside[aria-label="Main navigation"]').should('be.visible');
    cy.reload();
    cy.url().should('include', '/dashboard');
    cy.get('aside[aria-label="Main navigation"]').should('be.visible');
  });

  it('redirects unauthenticated deep links to login', () => {
    cy.clearAllSessionStorage();
    cy.visit('/dashboard');
    cy.url().should('include', '/auth/login');
  });

  it('redirects authenticated users away from login', () => {
    cy.visitAuthenticated('/auth/login');
    cy.url().should('include', '/dashboard');
  });

  it('logs out and clears protected access', () => {
    cy.login();
    cy.get('[data-cy=logout]').click();
    cy.url().should('include', '/auth/login');
    cy.visit('/dashboard');
    cy.url().should('include', '/auth/login');
  });
});
