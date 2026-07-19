import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Skeleton } from './skeleton';

describe('Skeleton', () => {
  let fixture: ComponentFixture<Skeleton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Skeleton],
    }).compileComponents();

    fixture = TestBed.createComponent(Skeleton);
    fixture.detectChanges();
  });

  it('renders block variant by default', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.skeleton--block')).toBeTruthy();
  });

  it('renders chart variant', () => {
    fixture.componentRef.setInput('variant', 'chart');
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.skeleton--chart')).toBeTruthy();
  });

  it('applies tall style when tall is true', () => {
    fixture.componentRef.setInput('tall', true);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.skeleton--tall')).toBeTruthy();
  });

  it('does not apply tall style by default', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.skeleton--tall')).toBeFalsy();
  });
});
