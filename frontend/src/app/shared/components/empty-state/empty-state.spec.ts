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
});
