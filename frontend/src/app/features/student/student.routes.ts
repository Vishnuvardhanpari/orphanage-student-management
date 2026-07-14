import { Routes } from '@angular/router';
import { StudentListPage } from './pages/student-list-page/student-list-page';
import { StudentFormPage } from './pages/student-form-page/student-form-page';
import { StudentProfilePage } from './pages/student-profile-page/student-profile-page';

export const STUDENT_ROUTES: Routes = [
  {
    path: '',
    component: StudentListPage,
    title: 'Students',
  },
  {
    path: 'new',
    component: StudentFormPage,
    title: 'Register student',
  },
  {
    path: ':id',
    component: StudentProfilePage,
    title: 'Student profile',
  },
];
