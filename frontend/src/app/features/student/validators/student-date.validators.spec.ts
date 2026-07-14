import {
  AbstractControl,
  FormControl,
  FormGroup,
} from '@angular/forms';
import {
  dateOfBirthNotAfterAdmissionValidator,
  pastOrPresentDateValidator,
  todayIsoDate,
} from './student-date.validators';

describe('student-date.validators', () => {
  describe('pastOrPresentDateValidator', () => {
    const validator = pastOrPresentDateValidator();

    it('accepts today and past dates', () => {
      expect(validator(new FormControl(todayIsoDate()))).toBeNull();
      expect(validator(new FormControl('2010-01-01'))).toBeNull();
    });

    it('rejects future dates', () => {
      expect(validator(new FormControl('2999-12-31'))).toEqual({
        pastOrPresent: true,
      });
    });
  });

  describe('dateOfBirthNotAfterAdmissionValidator', () => {
    const validator = dateOfBirthNotAfterAdmissionValidator();

    function group(dob: string, admission: string): AbstractControl {
      return new FormGroup({
        dateOfBirth: new FormControl(dob),
        admissionDate: new FormControl(admission),
      });
    }

    it('accepts dob on or before admission', () => {
      expect(validator(group('2015-01-01', '2020-01-01'))).toBeNull();
      expect(validator(group('2020-01-01', '2020-01-01'))).toBeNull();
    });

    it('rejects dob after admission', () => {
      expect(validator(group('2020-01-01', '2019-01-01'))).toEqual({
        dobAfterAdmission: true,
      });
    });
  });
});
