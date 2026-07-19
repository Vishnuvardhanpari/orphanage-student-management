import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Card } from './card';

describe('Card', () => {
  let fixture: ComponentFixture<Card>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Card],
    }).compileComponents();

    fixture = TestBed.createComponent(Card);
    fixture.detectChanges();
  });

  it('renders elevated variant by default', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.card--elevated')).toBeTruthy();
  });

  it('applies glass variant', () => {
    fixture.componentRef.setInput('variant', 'glass');
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.card--glass')).toBeTruthy();
  });
});
