import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DialogShell } from './dialog-shell';

@Component({
  standalone: true,
  imports: [DialogShell],
  template: `
    <app-dialog-shell ariaLabel="Confirm action">
      <h2 dialogTitle>Title</h2>
      <p>Body</p>
      <div dialogActions>Actions</div>
    </app-dialog-shell>
  `,
})
class DialogShellHost {
}

describe('DialogShell', () => {
  let fixture: ComponentFixture<DialogShellHost>;

  beforeEach(async () => {
    const overlayHost = document.createElement('div');
    const container = document.createElement('div');
    container.className = 'cdk-dialog-container';
    container.setAttribute('role', 'dialog');
    overlayHost.appendChild(container);

    await TestBed.configureTestingModule({
      imports: [DialogShellHost],
      providers: [
        {
          provide: DialogRef,
          useValue: {
            overlayRef: { hostElement: overlayHost },
          },
        },
        { provide: DIALOG_DATA, useValue: null },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DialogShellHost);
    fixture.detectChanges();
  });

  it('does not nest role=dialog on the shell root', () => {
    const shell = fixture.nativeElement.querySelector('.dialog-shell') as HTMLElement;
    expect(shell.getAttribute('role')).toBeNull();
  });

  it('applies aria-label to the CDK dialog container', () => {
    const dialogRef = TestBed.inject(DialogRef) as {
      overlayRef: { hostElement: HTMLElement };
    };
    const container = dialogRef.overlayRef.hostElement.querySelector(
      '.cdk-dialog-container',
    ) as HTMLElement;
    expect(container.getAttribute('aria-label')).toBe('Confirm action');
  });
});
