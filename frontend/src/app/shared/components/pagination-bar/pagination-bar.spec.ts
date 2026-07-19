import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginationBar } from './pagination-bar';

describe('PaginationBar', () => {
  let fixture: ComponentFixture<PaginationBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginationBar],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginationBar);
    fixture.componentRef.setInput('page', 0);
    fixture.componentRef.setInput('pageSize', 20);
    fixture.componentRef.setInput('totalElements', 45);
    fixture.componentRef.setInput('itemLabel', 'students');
    fixture.detectChanges();
  });

  it('shows meta and disables previous on first page', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('45 students');
    expect(el.textContent).toContain('Page 1 of 3');
    const buttons = el.querySelectorAll('button');
    expect(buttons[0].disabled).toBeTrue();
    expect(buttons[1].disabled).toBeFalse();
  });
});
