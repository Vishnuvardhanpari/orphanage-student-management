import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Component } from '@angular/core';
import { Input } from './input';

@Component({
  standalone: true,
  imports: [Input, ReactiveFormsModule],
  template: `<app-input [formControl]="control" type="text" placeholder="Name" />`,
})
class InputHost {
  readonly control = new FormControl('initial', { nonNullable: true });
}

describe('Input', () => {
  let fixture: ComponentFixture<InputHost>;
  let host: InputHost;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InputHost],
    }).compileComponents();

    fixture = TestBed.createComponent(InputHost);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders with an auto-generated id and reflects form control value', () => {
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(input.id).toMatch(/^oms-input-/);
    expect(input.value).toBe('initial');
    expect(input.placeholder).toBe('Name');
  });

  it('propagates user input to the form control', () => {
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'updated';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(host.control.value).toBe('updated');
  });
});
