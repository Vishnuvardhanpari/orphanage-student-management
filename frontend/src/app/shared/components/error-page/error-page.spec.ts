import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ErrorPage } from './error-page';

describe('ErrorPage', () => {
  let fixture: ComponentFixture<ErrorPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorPage],
    }).compileComponents();

    fixture = TestBed.createComponent(ErrorPage);
    fixture.detectChanges();
  });

  it('renders 404 messaging and dashboard link', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('404');
    expect(el.textContent).toContain('Page not found');
    expect(el.textContent).toContain('Go to dashboard');
  });
});
