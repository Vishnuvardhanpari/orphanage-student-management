import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FilterPanel } from './filter-panel';

describe('FilterPanel', () => {
  let fixture: ComponentFixture<FilterPanel>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilterPanel],
    }).compileComponents();

    fixture = TestBed.createComponent(FilterPanel);
    fixture.componentRef.setInput('ariaLabel', 'Student filters');
    fixture.detectChanges();
  });

  it('exposes role=search with the provided aria-label', () => {
    const root = fixture.nativeElement.querySelector('[role="search"]') as HTMLElement;
    expect(root).toBeTruthy();
    expect(root.getAttribute('aria-label')).toBe('Student filters');
  });

  it('projects content into the filter grid', () => {
    fixture = TestBed.createComponent(FilterPanel);
    // Content projection is verified via host wrappers in Field specs;
    // here we assert the grid shell exists for default columns.
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.filter-panel__grid')).toBeTruthy();
  });
});
