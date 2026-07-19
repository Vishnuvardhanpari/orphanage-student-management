import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ResetPasswordDialog } from './reset-password-dialog';

describe('ResetPasswordDialog', () => {
  let fixture: ComponentFixture<ResetPasswordDialog>;
  let component: ResetPasswordDialog;
  let dialogRef: jasmine.SpyObj<DialogRef<string | null>>;

  beforeEach(async () => {
    const overlayHost = document.createElement('div');
    const container = document.createElement('div');
    container.className = 'cdk-dialog-container';
    container.setAttribute('role', 'dialog');
    overlayHost.appendChild(container);

    dialogRef = jasmine.createSpyObj('DialogRef', ['close']);
    (dialogRef as unknown as { overlayRef: { hostElement: HTMLElement } }).overlayRef = {
      hostElement: overlayHost,
    };

    await TestBed.configureTestingModule({
      imports: [ResetPasswordDialog],
      providers: [
        { provide: DialogRef, useValue: dialogRef },
        { provide: DIALOG_DATA, useValue: { username: 'staff1' } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('does not submit when passwords mismatch', () => {
    component.form.setValue({
      newPassword: 'Password123!',
      confirmPassword: 'Different1!',
    });
    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.hasError('passwordsMismatch')).toBeTrue();
  });

  it('closes with new password when valid', () => {
    component.form.setValue({
      newPassword: 'Password123!',
      confirmPassword: 'Password123!',
    });
    component.submit();
    expect(dialogRef.close).toHaveBeenCalledWith('Password123!');
  });

  it('reports confirm password errors', () => {
    component.form.controls.confirmPassword.markAsTouched();
    expect(component['confirmPasswordError']()).toBe(
      'Confirm password is required.',
    );

    component.form.setValue({
      newPassword: 'Password123!',
      confirmPassword: 'nope',
    });
    component.form.controls.confirmPassword.markAsTouched();
    expect(component['confirmPasswordError']()).toBe('Passwords do not match.');
  });
});
