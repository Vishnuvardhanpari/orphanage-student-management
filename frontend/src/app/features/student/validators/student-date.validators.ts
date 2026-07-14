import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Rejects dates strictly after today (local calendar date).
 */
export function pastOrPresentDateValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string | null | undefined;
    if (!value) {
      return null;
    }
    const today = todayIsoDate();
    return value > today ? { pastOrPresent: true } : null;
  };
}

/**
 * Ensures dateOfBirth is on or before admissionDate.
 */
export function dateOfBirthNotAfterAdmissionValidator(): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const dob = group.get('dateOfBirth')?.value as string | null | undefined;
    const admission = group.get('admissionDate')?.value as string | null | undefined;
    if (!dob || !admission) {
      return null;
    }
    return dob > admission ? { dobAfterAdmission: true } : null;
  };
}

/** Today's date as YYYY-MM-DD in local time. */
export function todayIsoDate(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
