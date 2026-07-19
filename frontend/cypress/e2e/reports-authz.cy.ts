describe('Reports and authorization', () => {
  it('exports a single student PDF from the profile page', () => {
    const admissionNumber = `PDF${Date.now().toString().slice(-8)}`;
    cy.visitAuthenticated('/students/new');
    cy.get('input[formControlName="admissionNumber"]').clear().type(admissionNumber);
    cy.get('input[formControlName="firstName"]').clear().type('Pdf');
    cy.get('input[formControlName="lastName"]').clear().type('Export');
    cy.get('select[formControlName="gender"]').select('FEMALE');
    cy.get('input[formControlName="dateOfBirth"]').clear().type('2014-05-20');
    cy.get('input[formControlName="admissionDate"]').clear().type('2024-06-01');
    cy.contains('button', 'Register student').click();
    cy.url({ timeout: 20000 }).should('match', /\/students\/[0-9a-f-]{36}$/i);

    cy.intercept('GET', '**/api/v1/reports/student/**').as('exportPdf');
    cy.contains('button', 'Export PDF').click();
    cy.get('.cdk-overlay-container').within(() => {
      cy.contains('button', 'Generate PDF').click();
    });
    cy.wait('@exportPdf', { timeout: 30000 }).its('response.statusCode').should('eq', 200);
  });

  it('hides admin-only nav for STAFF and blocks /users', () => {
    cy.ensureStaffUser();
    cy.visitAuthenticated(
      '/dashboard',
      Cypress.env('staffUsername'),
      Cypress.env('staffPassword'),
    );
    cy.get('aside[aria-label="Main navigation"]').within(() => {
      cy.contains('a', 'Users').should('not.exist');
      cy.contains('a', 'Audit Logs').should('not.exist');
    });
    cy.visit('/users');
    cy.url().should('not.include', '/users');
  });
});
