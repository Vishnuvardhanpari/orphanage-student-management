import { Routes } from '@angular/router';
import { StudentListPage } from './pages/student-list-page/student-list-page';

export const STUDENT_ROUTES: Routes = [
  {
    path: '',
    component: StudentListPage,
    title: 'Students',
  },
];
