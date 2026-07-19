import { DialogRef } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../features/auth/services/auth.service';
import { MobileNavDrawer } from './mobile-nav-drawer';

describe('MobileNavDrawer', () => {
  let fixture: ComponentFixture<MobileNavDrawer>;
  let dialogRef: jasmine.SpyObj<DialogRef<void>>;

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj('DialogRef', ['close']);
    await TestBed.configureTestingModule({
      imports: [MobileNavDrawer],
      providers: [
        provideRouter([]),
        { provide: DialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { isAdmin: () => true } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MobileNavDrawer);
    fixture.detectChanges();
  });

  it('does not set nested role=dialog on the drawer root', () => {
    const root = fixture.nativeElement.querySelector('.mobile-nav') as HTMLElement;
    expect(root.getAttribute('role')).toBeNull();
  });

  it('closes the dialog when close is invoked', () => {
    fixture.componentInstance['close']();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('closes when a nav link is clicked', () => {
    const link = fixture.nativeElement.querySelector('.mobile-nav__link') as HTMLAnchorElement;
    link.click();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
