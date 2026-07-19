import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Textarea } from './textarea';

@Component({
  standalone: true,
  imports: [Textarea, ReactiveFormsModule],
  template: `<app-textarea [formControl]="control" placeholder="Notes" [rows]="4" />`,
})
class TextareaHost {
  readonly control = new FormControl('', { nonNullable: true });
}

describe('Textarea', () => {
  let fixture: ComponentFixture<TextareaHost>;
  let host: TextareaHost;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TextareaHost],
    }).compileComponents();

    fixture = TestBed.createComponent(TextareaHost);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders with an auto-generated id and rows', () => {
    const el = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    expect(el.id).toMatch(/^oms-textarea-/);
    expect(el.rows).toBe(4);
    expect(el.placeholder).toBe('Notes');
  });

  it('propagates input to the form control', () => {
    const el = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    el.value = 'Hello';
    el.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(host.control.value).toBe('Hello');
  });
});
