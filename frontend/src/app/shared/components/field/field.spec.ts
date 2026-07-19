import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Field } from './field';
import { Input } from '../input/input';
import { Select } from '../select/select';

@Component({
  standalone: true,
  imports: [Field, Input, ReactiveFormsModule],
  template: `
    <app-field label="Search">
      <app-input type="text" />
    </app-field>
  `,
})
class FieldHostAuto {
}

@Component({
  standalone: true,
  imports: [Field, Input, ReactiveFormsModule],
  template: `
    <app-field label="Email" forId="custom-email">
      <app-input id="custom-email" type="email" />
    </app-field>
  `,
})
class FieldHostManual {
}

@Component({
  standalone: true,
  imports: [Field, Select],
  template: `
    <app-field label="Role">
      <app-select>
        <option value="STAFF">Staff</option>
      </app-select>
    </app-field>
  `,
})
class FieldHostSelect {
}

describe('Field', () => {
  it('associates the label with the projected input id automatically', async () => {
    await TestBed.configureTestingModule({
      imports: [FieldHostAuto],
    }).compileComponents();

    const fixture = TestBed.createComponent(FieldHostAuto);
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label') as HTMLLabelElement;
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    expect(input.id).toBeTruthy();
    expect(label.htmlFor).toBe(input.id);
  });

  it('uses forId override when provided', async () => {
    await TestBed.configureTestingModule({
      imports: [FieldHostManual],
    }).compileComponents();

    const fixture = TestBed.createComponent(FieldHostManual);
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label') as HTMLLabelElement;
    expect(label.htmlFor).toBe('custom-email');
    expect((fixture.nativeElement.querySelector('input') as HTMLInputElement).id).toBe(
      'custom-email',
    );
  });

  it('associates the label with a projected select', async () => {
    await TestBed.configureTestingModule({
      imports: [FieldHostSelect],
    }).compileComponents();

    const fixture = TestBed.createComponent(FieldHostSelect);
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label') as HTMLLabelElement;
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    expect(select.id).toBeTruthy();
    expect(label.htmlFor).toBe(select.id);
  });
});
