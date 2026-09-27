import { ENDPOINTS, listRequest, metricFrom, numeric, request } from './api.js';
import {
  actionLink,
  button,
  clear,
  closeModal,
  confirmDialog,
  emptyState,
  errorState,
  field,
  formatDate,
  icon,
  input,
  loading,
  node,
  panel,
  pageHeading,
  responseError,
  select,
  setBusy,
  statGrid,
  table,
  toast,
  updateStat
} from './ui.js';
import {
  classId,
  classLabel,
  currentOption,
  dateOf,
  dedupeOptions,
  idFor,
  openFormModal,
  optionLabel,
  optionsFrom,
  showRetry,
  studentLabel,
  subjectId,
  subjectLabel,
  termId,
  termLabel,
  valueOf
} from './page-utils.js';

function setStat(grid, key, value) {
  updateStat(grid, key, value === undefined || value === null || value === '' ? '—' : value);
}

function classSubjectLabel(item) {
  const className = classLabel(item) || valueOf(item, ['schoolClassName'], '—');
  const name = subjectLabel(item) || valueOf(item, ['subjectName'], '—');
  return `${className} — ${name}`;
}

function classStudentCount(item) {
  return valueOf(item, ['studentCount', 'student_count', 'totalStudents'], '');
}

async function loadAssignments() {
  const result = await listRequest(`${ENDPOINTS.teachers}/me/classes`);
  return result.items;
}

function assignmentFilterOptions(assignments) {
  const classOptions = dedupeOptions(optionsFrom(assignments, ['schoolClassId'], ['schoolClassName']));
  const subjectOptions = dedupeOptions(optionsFrom(assignments, ['subjectId'], ['subjectName']));
  return { classOptions, subjectOptions };
}

export function teacherOverview(context) {
  const root = node('div');
  const stats = statGrid([
    { key: 'students', label: 'My Students', value: 'Loading…' },
    { key: 'roster', label: 'Awaiting Roster Confirmation', value: 'Loading…' },
    { key: 'materials', label: 'Materials Uploaded', value: 'Loading…' },
    { key: 'results', label: 'Results Pending Entry', value: 'Loading…' }
  ]);
  const body = node('div', {}, [loading('Loading assigned classes…')]);
  root.append(pageHeading('My Classes', 'Your assigned classes and subjects'), stats, panel('Assigned Classes', body));
  const load = async () => {
    const [dashboard, classes] = await Promise.all([request(`${ENDPOINTS.dashboard}/teacher`), loadAssignments()]);
    setStat(stats, 'students', metricFrom(dashboard, 'myStudents'));
    setStat(stats, 'roster', metricFrom(dashboard, 'awaitingRosterConfirmation'));
    setStat(stats, 'materials', metricFrom(dashboard, 'materialsUploaded'));
    setStat(stats, 'results', metricFrom(dashboard, 'resultsPendingEntry'));
    if (!classes.length) {
      clear(body).appendChild(emptyState('No assigned classes were returned by the API.'));
      return;
    }
    const grid = node('div', { class: 'class-card-grid' });
    classes.forEach((item) => {
      const card = button('', 'class-card', () => {
        const query = new URLSearchParams({ classId: classId(item), subjectId: subjectId(item) });
        context.navigate(`roster?${query.toString()}`);
      });
      card.append(
        node('div', { class: 'class-card-title', text: classSubjectLabel(item) }),
        node('div', { class: 'class-card-meta', text: classStudentCount(item) === '' ? 'Student count unavailable' : `${classStudentCount(item)} students` }),
        node('div', { class: 'table-secondary', text: 'Open roster confirmation' })
      );
      grid.appendChild(card);
    });
    clear(body).appendChild(grid);
  };
  showRetry(body, load, () => {});
  return root;
}

export function teacherRoster(context) {
  const root = node('div');
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading roster…')]);
  const classOptions = node('div', { class: 'filter-row' });
  let selectedClass = new URLSearchParams(window.location.hash.split('?')[1] || '').get('classId') || '';
  const renderClassFilter = (options) => {
    clear(classOptions);
    if (options.length < 2) return;
    const control = select('rosterClass', options, selectedClass, { 'aria-label': 'Filter roster by class' });
    control.addEventListener('change', () => {
      selectedClass = control.value;
      loadRoster();
    });
    classOptions.appendChild(control);
  };
  const loadRoster = async () => {
    const query = { rosterStatus: 'PENDING' };
    if (selectedClass) query.classId = selectedClass;
    const result = await listRequest(`${ENDPOINTS.teachers}/me/roster`, query);
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No students are awaiting roster confirmation.'));
      return;
    }
    clear(body).appendChild(table(['Student Name', 'Class', 'Confirm action', 'Flag action'], result.items, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: studentLabel(item) }));
      tr.appendChild(node('td', { text: classLabel(item) }));
      const confirm = actionLink('Confirm', async () => {
        setBusy(confirm, true, 'Saving…');
        try {
          await request(`${ENDPOINTS.students}/${encodeURIComponent(idFor(item))}/roster`, { method: 'PATCH', body: { rosterConfirmed: true, status: 'CONFIRMED' } });
          toast('Roster confirmation recorded through the API.');
          await loadRoster();
        } catch (error) {
          toast(responseError(error), 'error');
        } finally {
          setBusy(confirm, false);
        }
      });
      const flag = actionLink('Flag', async () => {
        const confirmed = await confirmDialog('Flag student', 'The student will be marked as flagged through the API and excluded from result entry until corrected.', 'Flag student');
        if (!confirmed) return;
        setBusy(flag, true, 'Saving…');
        try {
          await request(`${ENDPOINTS.students}/${encodeURIComponent(idFor(item))}/roster`, { method: 'PATCH', body: { rosterConfirmed: false, rosterStatus: 'FLAGGED', status: 'FLAGGED' } });
          toast('Student flag recorded through the API.');
          await loadRoster();
        } catch (error) {
          toast(responseError(error), 'error');
        } finally {
          setBusy(flag, false);
        }
      }, 'table-action danger');
      tr.appendChild(node('td', {}, [confirm]));
      tr.appendChild(node('td', {}, [flag]));
    }));
  };
  root.append(pageHeading('Confirm Roster', 'Confirm students assigned to your classes'), classOptions, panel('Roster Confirmation', body, null, { flush: true }));
  showRetry(body, loadRoster, () => {});
  loadAssignments()
    .then((assignments) => renderClassFilter(dedupeOptions(optionsFrom(assignments, ['schoolClassId'], ['schoolClassName']))))
    .catch((error) => toast(responseError(error), 'error'));
  return root;
}

function gradeFor(total) {
  if (total >= 75) return 'A';
  if (total >= 60) return 'B';
  if (total >= 50) return 'C';
  if (total >= 45) return 'D';
  if (total >= 40) return 'E';
  return 'F';
}

function resultValue(item, keys) {
  return valueOf(item, keys, '');
}

export function teacherResults(context) {
  const root = node('div');
  const filterRow = node('div', { class: 'filter-row' });
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading result filters…')]);
  let filters = { classId: '', subjectId: '', termId: '' };
  let resultItems = [];
  const saveButton = button('Save All', 'app-button app-button-primary', async () => {
    if (!resultItems.length) return;
    if (!filters.classId || !filters.subjectId || !filters.termId) {
      toast('Choose a class, subject and term before saving results.', 'error');
      return;
    }
    setBusy(saveButton, true, 'Saving…');
    try {
      const payload = resultItems.map((item) => ({
        studentId: idFor(item),
        subjectId: Number(filters.subjectId),
        termId: Number(filters.termId),
        ca1: numeric(resultValue(item, ['ca1']), 0),
        ca2: numeric(resultValue(item, ['ca2']), 0),
        exam: numeric(resultValue(item, ['exam']), 0)
      }));
      await request(ENDPOINTS.results, { method: 'PUT', body: { classId: Number(filters.classId), subjectId: Number(filters.subjectId), termId: Number(filters.termId), results: payload } });
      toast('Results saved through the API. Totals and grades are calculated on the server.');
      await loadResults();
    } catch (error) {
      toast(responseError(error), 'error');
    } finally {
      setBusy(saveButton, false);
    }
  });
  root.append(pageHeading('Results Entry', 'Enter continuous assessment and examination scores'), filterRow, panel('Results', body, saveButton, { flush: true }));

  const renderFilters = (classOptions, subjectOptions, termOptions) => {
    clear(filterRow);
    const classControl = select('resultClass', classOptions, filters.classId, { 'aria-label': 'Class' });
    const subjectControl = select('resultSubject', subjectOptions, filters.subjectId, { 'aria-label': 'Subject' });
    const termControl = select('resultTerm', termOptions, filters.termId, { 'aria-label': 'Term' });
    filters.classId = currentOption(classOptions, filters.classId);
    filters.subjectId = currentOption(subjectOptions, filters.subjectId);
    filters.termId = currentOption(termOptions, filters.termId);
    classControl.value = filters.classId;
    subjectControl.value = filters.subjectId;
    termControl.value = filters.termId;
    [classControl, subjectControl, termControl].forEach((control) => control.addEventListener('change', () => {
      filters = { classId: classControl.value, subjectId: subjectControl.value, termId: termControl.value };
      loadResults();
    }));
    filterRow.append(classControl, subjectControl, termControl);
  };

  const renderRows = () => {
    if (!resultItems.length) {
      clear(body).appendChild(emptyState('No results were returned by the API for these filters.'));
      return;
    }
    const tableNode = table(['Student', 'CA1', 'CA2', 'Exam', 'Total', 'Grade'], resultItems, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: studentLabel(item) }));
      const inputs = {};
      [['ca1', 20], ['ca2', 20], ['exam', 60]].forEach(([key, max]) => {
        const control = input(key, 'number', { min: 0, max, step: 1, value: resultValue(item, [key], '') });
        inputs[key] = control;
        control.addEventListener('input', () => {
          item[key] = control.value;
          const total = numeric(item.ca1) + numeric(item.ca2) + numeric(item.exam);
          item.total = total;
          item.grade = gradeFor(total);
          totalNode.textContent = String(total);
          gradeNode.textContent = item.grade;
        });
        tr.appendChild(node('td', {}, [control]));
      });
      const totalNode = node('td', { text: String(numeric(resultValue(item, ['total'], 0))) });
      const gradeNode = node('td', { class: 'table-primary', text: valueOf(item, ['grade'], '—') });
      tr.appendChild(totalNode);
      tr.appendChild(gradeNode);
    });
    clear(body).appendChild(tableNode);
  };

  const loadResults = async () => {
    clear(body).appendChild(loading('Loading results…'));
    const result = await listRequest(ENDPOINTS.results, { classId: filters.classId, subjectId: filters.subjectId, termId: filters.termId });
    resultItems = result.items;
    if (!resultItems.length) {
      const roster = await listRequest(`${ENDPOINTS.teachers}/me/roster`, { classId: filters.classId, rosterStatus: 'ALL' }).catch(() => ({ items: [] }));
      resultItems = roster.items.map((student) => ({ ...student, ca1: '', ca2: '', exam: '', total: 0, grade: '' }));
    }
    renderRows();
  };
  const loadFilters = async () => {
    const [assignments, terms] = await Promise.all([loadAssignments(), listRequest(ENDPOINTS.terms)]);
    const { classOptions, subjectOptions } = assignmentFilterOptions(assignments);
    if (!classOptions.length || !subjectOptions.length) {
      clear(body).appendChild(emptyState('You have no class and subject assignments yet. Ask an administrator to assign your teaching load.'));
      return;
    }
    renderFilters(classOptions, subjectOptions, optionsFrom(terms.items));
    await loadResults();
  };
  showRetry(body, loadFilters, () => {});
  return root;
}

export function teacherMaterials(context) {
  const root = node('div');
  const filterRow = node('div', { class: 'filter-row' });
  const body = node('div', { class: 'panel-body' }, [loading('Loading study materials…')]);
  let filters = { classId: '', subjectId: '' };
  const uploadButton = button([icon('plus', 16), ' Upload Material'], 'app-button app-button-primary', () => openMaterialForm(filters, loadMaterials, classOptions, subjectOptions));
  root.append(pageHeading('Study Materials', 'Manage materials for your assigned classes', [uploadButton]), filterRow, panel('Uploaded Materials', body));
  let classOptions = [];
  let subjectOptions = [];
  const renderFilters = () => {
    clear(filterRow);
    const classControl = select('materialClass', classOptions, filters.classId, { 'aria-label': 'Class' });
    const subjectControl = select('materialSubject', subjectOptions, filters.subjectId, { 'aria-label': 'Subject' });
    filters.classId = currentOption(classOptions, filters.classId);
    filters.subjectId = currentOption(subjectOptions, filters.subjectId);
    classControl.value = filters.classId;
    subjectControl.value = filters.subjectId;
    classControl.addEventListener('change', () => { filters.classId = classControl.value; loadMaterials(); });
    subjectControl.addEventListener('change', () => { filters.subjectId = subjectControl.value; loadMaterials(); });
    filterRow.append(classControl, subjectControl);
  };
  const loadMaterials = async () => {
    const result = await listRequest(ENDPOINTS.materials, { classId: filters.classId, subjectId: filters.subjectId });
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No study materials were returned by the API.'));
      return;
    }
    const list = node('div');
    result.items.forEach((item) => {
      const row = node('div', { class: 'material-item' }, [
        node('div', {}, [node('div', { class: 'material-title', text: valueOf(item, ['title', 'fileName', 'name'], '—') }), node('div', { class: 'material-meta', text: `${subjectLabel(item) || '—'} · ${classLabel(item) || '—'}` })]),
        node('div', { class: 'item-actions' }, [node('span', { class: 'table-secondary', text: formatDate(dateOf(item, ['uploadedAt', 'createdAt'])) }), actionLink('Delete', async () => {
          const confirmed = await confirmDialog('Delete study material', 'This removes the material and its stored file through the API.', 'Delete material');
          if (!confirmed) return;
          try {
            await request(`${ENDPOINTS.materials}/${encodeURIComponent(idFor(item))}`, { method: 'DELETE' });
            closeModal();
            toast('Study material deleted through the API.');
            await loadMaterials();
          } catch (error) { toast(responseError(error), 'error'); }
        }, 'table-action danger')])
      ]);
      list.appendChild(row);
    });
    clear(body).appendChild(list);
  };
  const loadFilters = async () => {
    const assignments = await loadAssignments();
    ({ classOptions, subjectOptions } = assignmentFilterOptions(assignments));
    if (!classOptions.length || !subjectOptions.length) {
      clear(body).appendChild(emptyState('You have no class and subject assignments yet. Ask an administrator to assign your teaching load.'));
      return;
    }
    renderFilters();
    await loadMaterials();
  };
  showRetry(body, loadFilters, () => {});
  return root;
}

async function openMaterialForm(filters, reload, classOptions, subjectOptions) {
  try {
    openFormModal({
      title: 'Upload Study Material',
      submitLabel: 'Upload material',
      values: { classId: filters.classId, subjectId: filters.subjectId },
      fields: [
        { name: 'title', label: 'Title' },
        { name: 'classId', label: 'Class', type: 'select', options: classOptions },
        { name: 'subjectId', label: 'Subject', type: 'select', options: subjectOptions },
        { name: 'file', label: 'File', type: 'file', accept: '*/*', full: true }
      ],
      onSubmit: async (values) => {
        const file = values.file;
        if (!file) throw new Error('Choose a file before uploading.');
        const formData = new FormData();
        formData.append('title', values.title);
        formData.append('classId', values.classId);
        formData.append('subjectId', values.subjectId);
        formData.append('file', file);
        await request(ENDPOINTS.materials, { method: 'POST', body: formData });
        toast('Study material uploaded through the API.');
        await reload();
      }
    });
  } catch (error) {
    toast(responseError(error), 'error');
  }
}

export const TEACHER_PAGES = {
  overview: teacherOverview,
  roster: teacherRoster,
  results: teacherResults,
  materials: teacherMaterials
};
