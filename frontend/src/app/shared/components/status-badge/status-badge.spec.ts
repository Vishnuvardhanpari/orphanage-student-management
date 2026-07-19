import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadge } from './status-badge';

describe('StatusBadge', () => {
  let fixture: ComponentFixture<StatusBadge>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadge],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusBadge);
    fixture.componentRef.setInput('label', 'Active');
    fixture.componentRef.setInput('tone', 'success');
    fixture.detectChanges();
  });

  it('renders label and tone class', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Active');
    expect(el.querySelector('.status-badge--success')).toBeTruthy();
  });
});
