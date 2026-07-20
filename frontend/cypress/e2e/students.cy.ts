describe('Student critical paths', () => {
  const admissionNumber = `E2E${Date.now().toString().slice(-8)}`;

  it('registers, searches, archives, and restores a student', () => {
    cy.visitAuthenticated('/students/new');

    cy.get('input[formControlName="admissionNumber"]').clear().type(admissionNumber);
    cy.get('input[formControlName="firstName"]').clear().type('E2E');
    cy.get('input[formControlName="lastName"]').clear().type('Student');
    cy.get('select[formControlName="gender"]').select('MALE');
    cy.get('input[formControlName="dateOfBirth"]').clear().type('2015-01-15');
    cy.get('input[formControlName="admissionDate"]').clear().type('2024-06-01');
    cy.get('input[formControlName="guardianName"]').clear().type('Guardian E2E');
    cy.get('input[formControlName="guardianPhone"]').clear().type('9876543210');

    cy.contains('button', 'Register student').click();
    cy.url({ timeout: 20000 }).should('match', /\/students\/[0-9a-f-]{36}$/i);
    cy.contains(admissionNumber).should('be.visible');

    cy.visit('/students');
    cy.get('app-input[formcontrolname="search"] input').clear().type(admissionNumber);
    cy.contains('button', /^Apply$/i).click();
    cy.contains('.ag-row', admissionNumber, { timeout: 15000 }).should('be.visible');
    cy.get('button[aria-label^="View student"]', { timeout: 15000 }).first().click();
    cy.url().should('match', /\/students\/[0-9a-f-]{36}$/i);

    cy.contains('button', /^Archive$/i).click();
    cy.get('.cdk-overlay-container').within(() => {
      cy.contains('button', /^Archive$/i).click();
    });

    cy.visit('/students/inactive');
    cy.get('app-input[formcontrolname="search"] input').clear().type(admissionNumber);
    cy.contains('button', /^Apply$/i).click();
    cy.contains('.ag-row', admissionNumber, { timeout: 15000 }).should('be.visible');
    cy.get('button[aria-label^="Restore student"]', { timeout: 15000 }).first().click();
    cy.get('.cdk-overlay-container').within(() => {
      cy.contains('button', /^Restore$/i).click();
    });

    cy.visit('/students');
    cy.get('app-input[formcontrolname="search"] input').clear().type(admissionNumber);
    cy.contains('button', /^Apply$/i).click();
    cy.contains('.ag-row', admissionNumber, { timeout: 15000 }).should('be.visible');
  });
});
