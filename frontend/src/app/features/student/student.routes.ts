import { Routes } from '@angular/router';
import { StudentListPage } from './pages/student-list-page/student-list-page';
import { StudentFormPage } from './pages/student-form-page/student-form-page';
import { StudentProfilePage } from './pages/student-profile-page/student-profile-page';
import { StudentInactiveListPage } from './pages/student-inactive-list-page/student-inactive-list-page';

export const STUDENT_ROUTES: Routes = [
  {
    path: '',
    component: StudentListPage,
    title: 'Students',
  },
  {
    path: 'inactive',
    component: StudentInactiveListPage,
    title: 'Archived students',
  },
  {
    path: 'inactive/:id',
    component: StudentProfilePage,
    title: 'Archived student profile',
    data: { archived: true },
  },
  {
    path: 'new',
    component: StudentFormPage,
    title: 'Register student',
  },
  {
    path: ':id/edit',
    component: StudentFormPage,
    title: 'Edit student',
  },
  {
    path: ':id',
    component: StudentProfilePage,
    title: 'Student profile',
  },
];
