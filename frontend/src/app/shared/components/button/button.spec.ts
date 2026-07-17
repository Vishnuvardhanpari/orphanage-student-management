import { ChangeDetectorRef, Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { Button } from './button';

/**
 * Host wrapper so specs can drive real content projection through
 * `<app-button>...</app-button>`, rather than relying on the empty light DOM
 * that `TestBed.createComponent(Button)` produces on its own. This is what
 * exposed BUG-UI-001 (routerLink variant lost its projected label) — the
 * previous specs never rendered a host with actual projected content.
 */
@Component({
  standalone: true,
  imports: [Button],
  template: `
    <app-button [routerLink]="routerLink" [disabled]="disabled">{{
      label
    }}</app-button>
  `,
})
class ButtonHost {
  label = 'Click me';
  routerLink: string | null = null;
  disabled = false;
}

describe('Button', () => {
  let fixture: ComponentFixture<Button>;
  let button: Button;

  async function setup(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [Button],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(Button);
    button = fixture.componentInstance;
  }

  async function setupHost(
    options: Partial<Pick<ButtonHost, 'label' | 'routerLink' | 'disabled'>> = {},
  ): Promise<{ fixture: ComponentFixture<ButtonHost>; host: HTMLElement }> {
    await TestBed.configureTestingModule({
      imports: [ButtonHost],
      providers: [provideRouter([])],
    }).compileComponents();
    const hostFixture = TestBed.createComponent(ButtonHost);
    Object.assign(hostFixture.componentInstance, options);
    hostFixture.detectChanges();
    return { fixture: hostFixture, host: hostFixture.nativeElement as HTMLElement };
  }

  it('renders a native <button> when no routerLink is set', async () => {
    await setup();
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('button')).not.toBeNull();
    expect(host.querySelector('a')).toBeNull();
  });

  it('emits pressed when the button is clicked', async () => {
    await setup();
    fixture.detectChanges();
    const emitted = jasmine.createSpy('pressed');
    button.pressed.subscribe(emitted);

    (fixture.nativeElement as HTMLElement).querySelector('button')!.dispatchEvent(
      new MouseEvent('click'),
    );

    expect(emitted).toHaveBeenCalled();
  });

  it('does not emit pressed when disabled', async () => {
    await setup();
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    const emitted = jasmine.createSpy('pressed');
    button.pressed.subscribe(emitted);

    (fixture.nativeElement as HTMLElement)
      .querySelector('button')!
      .dispatchEvent(new MouseEvent('click'));

    expect(emitted).not.toHaveBeenCalled();
  });

  // Regression for QA BUG-004: navigation actions must render as a single
  // anchor styled like a button, never a <button> nested inside an <a>.
  it('renders a styled anchor instead of a button when routerLink is set', async () => {
    await setup();
    fixture.componentRef.setInput('routerLink', '/students/inactive');
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const anchor = host.querySelector('a');
    expect(anchor).not.toBeNull();
    expect(host.querySelector('button')).toBeNull();
    expect(anchor?.classList.contains('oms-btn')).toBeTrue();
  });

  it('marks the anchor variant aria-disabled and blocks navigation when disabled', async () => {
    await setup();
    fixture.componentRef.setInput('routerLink', '/students/inactive');
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    const emitted = jasmine.createSpy('pressed');
    button.pressed.subscribe(emitted);

    const anchor = (fixture.nativeElement as HTMLElement).querySelector('a')!;
    expect(anchor.getAttribute('aria-disabled')).toBe('true');
    const event = new MouseEvent('click', { cancelable: true });
    anchor.dispatchEvent(event);

    expect(event.defaultPrevented).toBeTrue();
    expect(emitted).not.toHaveBeenCalled();
  });

  // Regression suite for QA BUG-UI-001: Angular's content projection is
  // resolved statically, so <ng-content> placed directly inside more than
  // one @if/@else branch only projects into one of them. These specs drive
  // real content projection through a host component (see ButtonHost above)
  // so they fail the same way a user would notice: a visibly empty button.
  describe('projected label text (BUG-UI-001)', () => {
    it('renders the projected label inside the <button> variant', async () => {
      const { host } = await setupHost({ label: 'Add student' });

      const button = host.querySelector('button')!;
      expect(button).not.toBeNull();
      expect(button.textContent?.trim()).toBe('Add student');
    });

    it('renders the projected label inside the routerLink <a> variant', async () => {
      const { host } = await setupHost({
        label: 'Archived students',
        routerLink: '/students/inactive',
      });

      const anchor = host.querySelector('a')!;
      expect(anchor).not.toBeNull();
      expect(anchor.textContent?.trim()).toBe('Archived students');
    });

    it('renders the projected label inside a disabled routerLink <a> variant', async () => {
      const { host } = await setupHost({
        label: 'Add student',
        routerLink: '/students/new',
        disabled: true,
      });

      const anchor = host.querySelector('a')!;
      expect(anchor).not.toBeNull();
      expect(anchor.textContent?.trim()).toBe('Add student');
    });

    it('keeps the label visible when routerLink switches from unset to set on a live instance', async () => {
      const { fixture: hostFixture, host } = await setupHost({
        label: 'Add student',
        routerLink: null,
      });
      expect(host.querySelector('button')?.textContent?.trim()).toBe('Add student');

      hostFixture.componentInstance.routerLink = '/students/new';
      hostFixture.detectChanges();
      // OnPush + signal inputs: force the child to refresh in this test harness
      // (mirrors what a reactive host update, e.g. a route/auth change, would
      // trigger for real); this asserts the @if/@else re-render — not just the
      // initial render — still projects the label via the ngTemplateOutlet fix.
      const buttonDebugEl = hostFixture.debugElement.query(By.directive(Button));
      buttonDebugEl.injector.get(ChangeDetectorRef).markForCheck();
      hostFixture.detectChanges();

      expect(host.querySelector('a')?.textContent?.trim()).toBe('Add student');
    });
  });
});
