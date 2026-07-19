import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Select } from './select';

@Component({
  standalone: true,
  imports: [Select, ReactiveFormsModule],
  template: `
    <app-select [formControl]="control">
      <option value="">All</option>
      <option value="STAFF">Staff</option>
    </app-select>
  `,
})
class SelectHost {
  readonly control = new FormControl('', { nonNullable: true });
}

describe('Select', () => {
  let fixture: ComponentFixture<SelectHost>;
  let host: SelectHost;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectHost],
    }).compileComponents();

    fixture = TestBed.createComponent(SelectHost);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders with an auto-generated id', () => {
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    expect(select.id).toMatch(/^oms-select-/);
  });

  it('propagates selection to the form control', () => {
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    select.value = 'STAFF';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(host.control.value).toBe('STAFF');
  });
});
