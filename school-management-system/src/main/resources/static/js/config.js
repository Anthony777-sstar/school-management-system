export const LOGIN_ROLES = [
  { key: 'STUDENT', label: 'Student', route: 'student' },
  { key: 'PARENT', label: 'Parent', route: 'parent' },
  { key: 'TEACHER', label: 'Teacher', route: 'teacher' },
  { key: 'ACCOUNTANT', label: 'Accountant', route: 'accountant' },
  { key: 'SUPER_ADMIN', label: 'Admin', route: 'admin' }
];

export const ROLE_CONFIG = {
  SUPER_ADMIN: {
    key: 'SUPER_ADMIN',
    route: 'admin',
    label: 'Super Admin',
    nav: [
      { id: 'overview', label: 'Overview', icon: 'grid' },
      { id: 'teachers', label: 'Teachers', icon: 'users' },
      { id: 'accountants', label: 'Accountants', icon: 'wallet' },
      { id: 'students', label: 'Students', icon: 'cap' },
      { id: 'enrollment', label: 'Enrollment Requests', icon: 'file' },
      { id: 'fees', label: 'Fees & Payments', icon: 'card' },
      { id: 'settings', label: 'Settings', icon: 'settings' }
    ]
  },
  TEACHER: {
    key: 'TEACHER',
    route: 'teacher',
    label: 'Teacher',
    nav: [
      { id: 'overview', label: 'My Classes', icon: 'grid' },
      { id: 'roster', label: 'Confirm Roster', icon: 'check' },
      { id: 'results', label: 'Results Entry', icon: 'chart' },
      { id: 'materials', label: 'Study Materials', icon: 'folder' }
    ]
  },
  STUDENT: {
    key: 'STUDENT',
    route: 'student',
    label: 'Student',
    nav: [
      { id: 'overview', label: 'Dashboard', icon: 'home' },
      { id: 'results', label: 'Results', icon: 'chart' },
      { id: 'materials', label: 'Study Materials', icon: 'folder' }
    ]
  },
  PARENT: {
    key: 'PARENT',
    route: 'parent',
    label: 'Parent',
    nav: [
      { id: 'overview', label: 'Dashboard', icon: 'home' },
      { id: 'fees', label: 'Fees', icon: 'card' },
      { id: 'results', label: 'Results', icon: 'chart' }
    ]
  },
  ACCOUNTANT: {
    key: 'ACCOUNTANT',
    route: 'accountant',
    label: 'Accountant',
    nav: [
      { id: 'overview', label: 'Overview', icon: 'grid' },
      { id: 'invoices', label: 'Fee Invoices', icon: 'file' },
      { id: 'outstanding', label: 'Outstanding Fees', icon: 'alert' },
      { id: 'ledger', label: 'Payment Ledger', icon: 'ledger' },
      { id: 'reports', label: 'Reports', icon: 'download' }
    ]
  }
};

export const ENDPOINTS = {
  auth: '/auth',
  enrollment: '/enrollment-applications',
  teachers: '/teachers',
  accountants: '/accountants',
  students: '/students',
  parents: '/parents',
  classes: '/school-classes',
  subjects: '/subjects',
  sessions: '/academic-sessions',
  terms: '/terms',
  results: '/results',
  materials: '/study-materials',
  invoices: '/fee-invoices',
  payments: '/fee-payments',
  outstanding: '/outstanding-fees',
  reminders: '/fee-reminders',
  notices: '/noticeboard-posts',
  settings: '/settings',
  dashboard: '/dashboard',
  academicContext: '/academic-context',
  paystackConfig: '/config/paystack'
};

export const CLASS_OPTIONS = ['JSS 1', 'JSS 2', 'JSS 3', 'SS 1', 'SS 2', 'SS 3'];

export const TERM_OPTIONS = [
  { value: 'FIRST_TERM', label: 'First Term' },
  { value: 'SECOND_TERM', label: 'Second Term' },
  { value: 'THIRD_TERM', label: 'Third Term' }
];
