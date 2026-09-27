import { ENDPOINTS, listRequest, request } from './api.js';
import {
  actionLink,
  button,
  clear,
  emptyState,
  errorState,
  formatDate,
  formatTerm,
  loading,
  node,
  panel,
  pageHeading,
  select,
  setBusy,
  table,
  toast
} from './ui.js';
import {
  classId,
  classLabel,
  currentOption,
  dateOf,
  downloadMaterial,
  idFor,
  loadOptions,
  showRetry,
  sortByDateDesc,
  studentLabel,
  subjectLabel,
  termLabel,
  valueOf as itemValue
} from './page-utils.js';

function sessionStudentId(context) {
  return itemValue(context.user, ['profileId', 'studentId', 'student_id'], '');
}

function profilePayload(context) {
  return request(`${ENDPOINTS.students}/me/profile`);
}

function profileName(profile) {
  return itemValue(profile, ['fullName', 'full_name', 'studentName', 'name'], '');
}

function calculateAge(dateValue) {
  if (!dateValue) return '—';
  const birth = new Date(dateValue);
  if (Number.isNaN(birth.getTime())) return '—';
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const month = today.getMonth() - birth.getMonth();
  if (month < 0 || (month === 0 && today.getDate() < birth.getDate())) age -= 1;
  return String(age);
}

function renderProfile(profile) {
  const grid = node('div', { class: 'profile-grid' });
  [
    ['Name', profileName(profile)],
    ['Class', classLabel(profile)],
    ['Age', calculateAge(itemValue(profile, ['dateOfBirth', 'date_of_birth', 'dob'], ''))],
    ['Birthday', formatDate(itemValue(profile, ['dateOfBirth', 'date_of_birth', 'dob'], ''))],
    ['Current term', itemValue(profile, ['currentTermName', 'currentTerm'], termLabel(profile))]
  ].forEach(([label, value]) => grid.appendChild(node('div', { class: 'profile-item' }, [node('div', { class: 'profile-label', text: label }), node('div', { class: 'profile-value', text: value || '—' })])));
  return grid;
}

function latestResultPreview(item) {
  if (!item) return emptyState('No result has been posted for this student yet.');
  return node('div', { class: 'latest-result' }, [
    node('div', {}, [node('div', { class: 'latest-result-label', text: 'Latest result' }), node('div', { class: 'latest-result-subject', text: subjectLabel(item) || itemValue(item, ['subjectName'], '—') })]),
    node('div', { class: 'result-grade', text: itemValue(item, ['grade'], '—') }),
    actionLink('View Full Results →', () => window.location.hash = '#/app/student/results')
  ]);
}

export function studentOverview(context) {
  const root = node('div');
  const profileBody = node('div', {}, [loading('Loading student profile…')]);
  const teachersBody = node('div', {}, [loading('Loading teachers…')]);
  const resultBody = node('div', {}, [loading('Loading latest result…')]);
  let studentId = sessionStudentId(context);
  root.append(
    pageHeading('Dashboard', 'Your Crestwood learner profile'),
    node('div', { class: 'panel-stack' }, [panel('Profile', profileBody), panel('My Teachers', teachersBody), panel('Latest Result', resultBody)])
  );

  const loadProfile = async () => {
    const profile = await profilePayload(context);
    studentId = itemValue(profile, ['id'], studentId);
    clear(profileBody).appendChild(renderProfile(profile));
  };
  showRetry(profileBody, loadProfile, () => {});

  const loadTeachers = async () => {
    const profile = await profilePayload(context);
    const id = itemValue(profile, ['id'], studentId);
    const teachers = itemValue(profile, ['teachers'], null);
    const rows = Array.isArray(teachers) ? teachers : (await listRequest(`${ENDPOINTS.students}/${encodeURIComponent(id)}/teachers`)).items;
    if (!rows.length) {
      clear(teachersBody).appendChild(emptyState('No teachers were returned by the API.'));
      return;
    }
    const list = node('div', { class: 'teacher-list' });
    rows.forEach((item) => list.appendChild(node('div', { class: 'teacher-list-row' }, [node('div', {}, [node('div', { class: 'teacher-name', text: itemValue(item, ['teacherName', 'fullName', 'name'], '—') }), node('div', { class: 'teacher-subject', text: itemValue(item, ['subjectName', 'subject'], '—') }) ])])));
    clear(teachersBody).appendChild(list);
  };
  showRetry(teachersBody, loadTeachers, () => {});

  const loadResult = async () => {
    const profile = await profilePayload(context);
    const id = itemValue(profile, ['id'], studentId);
    const result = await listRequest(ENDPOINTS.results, { studentId: id });
    const rows = sortByDateDesc(result.items, ['enteredAt']);
    clear(resultBody).appendChild(latestResultPreview(rows[0]));
  };
  showRetry(resultBody, loadResult, () => {});
  return root;
}

export function studentResults(context) {
  const root = node('div');
  const filterRow = node('div', { class: 'filter-row' });
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading terms…')]);
  let studentId = sessionStudentId(context);
  let termOptions = [];
  let selectedTerm = '';
  root.append(pageHeading('Results', 'View results by academic term'), filterRow, panel('Results', body, null, { flush: true }));
  const loadResults = async () => {
    const result = await listRequest(ENDPOINTS.results, { studentId, termId: selectedTerm });
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No results were returned by the API for this term.'));
      return;
    }
    clear(body).appendChild(table(['Subject', 'CA1', 'CA2', 'Exam', 'Total', 'Grade'], result.items, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: subjectLabel(item) || itemValue(item, ['subjectName'], '—') }));
      ['ca1', 'ca2', 'exam', 'total'].forEach((key) => tr.appendChild(node('td', { text: itemValue(item, [key], '—') })));
      tr.appendChild(node('td', { class: 'table-primary', text: itemValue(item, ['grade'], '—') }));
    }));
  };
  const loadTerms = async () => {
    const profile = await profilePayload(context);
    studentId = itemValue(profile, ['id'], studentId);
    termOptions = await loadOptions(ENDPOINTS.terms);
    selectedTerm = currentOption(termOptions, selectedTerm);
    clear(filterRow);
    const control = select('studentResultTerm', termOptions, selectedTerm, { 'aria-label': 'Term' });
    control.addEventListener('change', () => { selectedTerm = control.value; loadResults(); });
    filterRow.appendChild(control);
    await loadResults();
  };
  showRetry(body, loadTerms, () => {});
  return root;
}

export function studentMaterials(context) {
  const root = node('div');
  const filterRow = node('div', { class: 'filter-row' });
  const body = node('div', { class: 'panel-body' }, [loading('Loading study materials…')]);
  let subjectOptions = [];
  let selectedSubject = '';
  let studentClassId = '';
  root.append(pageHeading('Study Materials', 'Download materials for your class'), filterRow, panel('Study Materials', body));
  const loadMaterials = async () => {
    const result = await listRequest(ENDPOINTS.materials, { classId: studentClassId, subjectId: selectedSubject });
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No study materials were returned by the API.'));
      return;
    }
    const list = node('div');
    result.items.forEach((item) => {
      const action = actionLink('Download', async () => {
        setBusy(action, true, 'Downloading…');
        try {
          await downloadMaterial(item);
        } catch (error) {
          toast(responseError(error), 'error');
        } finally {
          setBusy(action, false);
        }
      });
      list.appendChild(node('div', { class: 'material-item' }, [
        node('div', {}, [node('div', { class: 'material-title', text: itemValue(item, ['title', 'fileName', 'name'], '—') }), node('div', { class: 'material-meta', text: `${subjectLabel(item) || '—'} · ${formatDate(dateOf(item, ['uploadedAt', 'createdAt']))}` })]),
        action
      ]));
    });
    clear(body).appendChild(list);
  };
  const loadProfileAndFilters = async () => {
    const profile = await profilePayload(context);
    studentClassId = classId(profile);
    subjectOptions = await loadOptions(ENDPOINTS.subjects);
    selectedSubject = '';
    clear(filterRow);
    const control = select('studentMaterialSubject', [{ value: '', label: 'All Subjects' }, ...subjectOptions], '', { 'aria-label': 'Subject' });
    control.addEventListener('change', () => { selectedSubject = control.value; loadMaterials(); });
    filterRow.appendChild(control);
    await loadMaterials();
  };
  showRetry(body, loadProfileAndFilters, () => {});
  return root;
}

export const STUDENT_PAGES = {
  overview: studentOverview,
  results: studentResults,
  materials: studentMaterials
};
