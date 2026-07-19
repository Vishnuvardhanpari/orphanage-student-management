import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmptyState } from './empty-state';

describe('EmptyState', () => {
  let fixture: ComponentFixture<EmptyState>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyState],
    }).compileComponents();

    fixture = TestBed.createComponent(EmptyState);
    fixture.componentRef.setInput('title', 'Nothing here');
    fixture.componentRef.setInput('description', 'Placeholder content');
    fixture.detectChanges();
  });

  it('should render title and description', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Nothing here');
    expect(compiled.textContent).toContain('Placeholder content');
  });

  it('applies error variant class', () => {
    fixture.componentRef.setInput('variant', 'error');
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-state--error')).toBeTruthy();
  });

  it('uses role=status for the default variant', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-state')?.getAttribute('role')).toBe('status');
  });

  it('uses role=alert for the error variant', () => {
    fixture.componentRef.setInput('variant', 'error');
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-state')?.getAttribute('role')).toBe('alert');
  });
});
