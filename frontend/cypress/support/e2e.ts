/// <reference types="cypress" />

const STORAGE_KEYS = {
  accessToken: 'oms.access_token',
  refreshToken: 'oms.refresh_token',
  currentUser: 'oms.current_user',
} as const;

declare global {
  namespace Cypress {
    interface Chainable {
      login(username?: string, password?: string): Chainable<void>;
      visitAuthenticated(
        path: string,
        username?: string,
        password?: string,
      ): Chainable<void>;
      ensureStaffUser(): Chainable<void>;
    }
  }
}

Cypress.Commands.add('login', (username?: string, password?: string) => {
  const user = username ?? Cypress.env('adminUsername');
  const pass = password ?? Cypress.env('adminPassword');
  cy.visit('/auth/login');
  cy.get('[data-cy=login-username]').clear().type(user);
  cy.get('[data-cy=login-password]').clear().type(pass, { log: false });
  cy.get('[data-cy=login-submit]').click();
  cy.url({ timeout: 15000 }).should('include', '/dashboard');
  cy.window().then((win) => {
    expect(win.sessionStorage.getItem(STORAGE_KEYS.accessToken)).to.be.a('string');
    expect(win.sessionStorage.getItem(STORAGE_KEYS.refreshToken)).to.be.a('string');
    expect(win.sessionStorage.getItem(STORAGE_KEYS.currentUser)).to.be.a('string');
  });
});

Cypress.Commands.add(
  'visitAuthenticated',
  (path: string, username?: string, password?: string) => {
    const user = username ?? Cypress.env('adminUsername');
    const pass = password ?? Cypress.env('adminPassword');
    cy.request('POST', `${Cypress.env('apiBaseUrl')}/auth/login`, {
      username: user,
      password: pass,
    }).then((response) => {
      expect(response.status).to.eq(200);
      const { accessToken, refreshToken, user: authUser } = response.body;
      cy.visit(path, {
        onBeforeLoad(win) {
          win.sessionStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
          win.sessionStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
          win.sessionStorage.setItem(
            STORAGE_KEYS.currentUser,
            JSON.stringify(authUser),
          );
        },
      });
    });
  },
);

Cypress.Commands.add('ensureStaffUser', () => {
  const staffUsername = Cypress.env('staffUsername');
  const staffPassword = Cypress.env('staffPassword');
  cy.request('POST', `${Cypress.env('apiBaseUrl')}/auth/login`, {
    username: Cypress.env('adminUsername'),
    password: Cypress.env('adminPassword'),
  }).then((adminLogin) => {
    const token = adminLogin.body.accessToken as string;
    cy.request({
      method: 'GET',
      url: `${Cypress.env('apiBaseUrl')}/users`,
      qs: { search: staffUsername, size: 20 },
      headers: { Authorization: `Bearer ${token}` },
    }).then((list) => {
      const found = (list.body?.content ?? []).some(
        (u: { username: string }) =>
          u.username?.toLowerCase() === String(staffUsername).toLowerCase(),
      );
      if (found) {
        return;
      }
      cy.request({
        method: 'POST',
        url: `${Cypress.env('apiBaseUrl')}/users`,
        headers: { Authorization: `Bearer ${token}` },
        body: {
          username: staffUsername,
          email: `${staffUsername}@oms.local`,
          role: 'STAFF',
          authProvider: 'LOCAL',
          password: staffPassword,
        },
      });
    });
  });
});

export {};
