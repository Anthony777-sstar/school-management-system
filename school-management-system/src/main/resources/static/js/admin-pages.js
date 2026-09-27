import { ENDPOINTS, listRequest, metricFrom, numeric, request } from './api.js';
import { CLASS_OPTIONS, TERM_OPTIONS } from './config.js';
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
  formatMoney,
  formatTerm,
  icon,
  input,
  loading,
  node,
  panel,
  pageHeading,
  previewList,
  responseError,
  select,
  setBusy,
  sortRecent,
  statGrid,
  statusBadge,
  table,
  toast,
  updateStat
} from './ui.js';
import {
  amountPaidOf,
  classId,
  classLabel,
  currentOption,
  dateOf,
  detailItem as pageDetailItem,
  idFor,
  invoiceAmount,
  loadOptions,
  openDetailModal as openDetailModalPage,
  openFormModal,
  optionLabel,
  optionsFrom,
  pageQueryId,
  parentLabel,
  showCredentials,
  showRetry,
  studentLabel,
  subjectId,
  subjectLabel,
  termId,
  termLabel,
  valueOf
} from './page-utils.js';

const moneyFields = ['amount', 'amountPaid', 'amountDue', 'totalAmount'];

function joined(value) {
  if (Array.isArray(value)) return value.map((entry) => typeof entry === 'object' ? optionLabel(entry) : String(entry)).filter(Boolean).join(', ');
  if (value && typeof value === 'object') return optionLabel(value);
  return value ? String(value) : '—';
}

function rowIdValue(item, keys) {
  return valueOf(item, keys, '');
}

function optionValues(items, keys) {
  return (Array.isArray(items) ? items : []).map((item) => valueOf(item, keys, '')).filter(Boolean).map(String);
}

function metricValue(payload, keys) {
  for (const key of keys) {
    const value = metricFrom(payload, key);
    if (value !== undefined) return value;
  }
  return undefined;
}

function setStatOrUnavailable(grid, key, value) {
  updateStat(grid, key, value === undefined || value === null || value === '' ? '—' : value);
}

function addViewAll(label, onClick) {
  return actionLink(`${label} →`, onClick, 'panel-action');
}

function overviewStatValue(result, keys, count = true) {
  if (result.status !== 'fulfilled') return undefined;
  const payload = result.value;
  const metric = metricValue(payload, keys);
  if (metric !== undefined) return metric;
  return count ? countFrom(payload) : undefined;
}

function renderOverviewPanelError(body, error, retry) {
  clear(body).appendChild(errorState(responseError(error), retry));
}

function renderRecentEnrollmentRows(items, navigate) {
  return previewList(sortRecent(items, ['submittedAt', 'createdAt']).slice(0, 3), (item) => {
    const row = button('', 'preview-row clickable', () => {
      const id = idFor(item);
      navigate(`enrollment?id=${encodeURIComponent(id)}`);
    });
    const main = node('div', { class: 'preview-main' }, [
      node('div', { class: 'preview-title', text: parentLabel(item) || studentLabel(item) }),
      node('div', { class: 'preview-meta', text: classLabel(item) || valueOf(item, ['applyingForClassName', 'applyingForClass'], 'Class not provided') })
    ]);
    row.appendChild(main);
    row.appendChild(node('div', { class: 'preview-value', text: formatDate(dateOf(item, ['submittedAt', 'createdAt'])) }));
    return row;
  }, 'No pending enrollment requests were returned by the API.');
}

function renderAnnouncementRows(items, onViewAll) {
  return previewList(sortRecent(items, ['postedAt', 'createdAt']).slice(0, 3), (item) => {
    const row = node('div', { class: 'preview-row' });
    const main = node('div', { class: 'preview-main' }, [
      node('div', { class: 'preview-title', text: valueOf(item, ['title', 'headline'], 'Untitled notice') }),
      node('div', { class: 'preview-meta', text: formatDate(dateOf(item, ['postedAt', 'createdAt', 'date'])) })
    ]);
    row.appendChild(main);
    row.appendChild(button('View', 'table-action', onViewAll));
    return row;
  }, 'No announcements were returned by the API.');
}

export function adminOverview(context) {
  const root = node('div');
  const stats = statGrid([
    { key: 'students', label: 'Total Students', value: 'Loading…' },
    { key: 'teachers', label: 'Total Teachers', value: 'Loading…' },
    { key: 'enrollments', label: 'Pending Enrollments', value: 'Loading…' },
    { key: 'fees', label: 'Fees Collected This Term', value: 'Loading…' }
  ]);
  const recentBody = node('div', {}, [loading('Loading enrollment requests…')]);
  const noticesBody = node('div', {}, [loading('Loading announcements…')]);
  const recentPanel = panel('Recent Enrollment Requests', recentBody, addViewAll('View All', () => context.navigate('enrollment')));
  const noticesPanel = panel('Latest Announcements', noticesBody, addViewAll('View All', () => showAllNotices(context)));
  root.append(
    pageHeading('Overview', 'Live school administration overview'),
    stats,
    node('div', { class: 'panel-stack' }, [recentPanel, noticesPanel])
  );

  const loadStats = async () => {
    try {
      const dashboard = await request(`${ENDPOINTS.dashboard}/admin`);
      const students = metricFrom(dashboard, 'totalStudents');
      const teachers = metricFrom(dashboard, 'totalTeachers');
      const enrollments = metricFrom(dashboard, 'pendingEnrollments');
      const fees = metricFrom(dashboard, 'feesCollectedThisTerm');
      setStatOrUnavailable(stats, 'students', students);
      setStatOrUnavailable(stats, 'teachers', teachers);
      setStatOrUnavailable(stats, 'enrollments', enrollments);
      setStatOrUnavailable(stats, 'fees', fees === undefined ? undefined : formatMoney(numeric(fees)));
    } catch (error) {
      ['students', 'teachers', 'enrollments', 'fees'].forEach((key) => setStatOrUnavailable(stats, key, undefined));
    }
  };
  loadStats();

  const loadRecent = () => listRequest(ENDPOINTS.enrollment, { status: 'PENDING' }).then((result) => result.items);
  showRetry(recentBody, loadRecent, (items) => {
    clear(recentBody).appendChild(renderRecentEnrollmentRows(items, context.navigate));
  });

  const loadNotices = () => listRequest(ENDPOINTS.notices).then((result) => result.items);
  const refreshNotices = () => showRetry(noticesBody, loadNotices, (items) => {
    clear(noticesBody).appendChild(renderAnnouncementRows(items, () => showAllNotices(context)));
  });
  refreshNotices();
  noticesPanel.querySelector('.panel-header')?.appendChild(button([icon('plus', 14), ' Post announcement'], 'app-button app-button-light', () => openNoticeComposer(context, refreshNotices)));
  return root;
}

function openNoticeComposer(context, refresh) {
  openFormModal({
    title: 'Post an announcement',
    submitLabel: 'Post announcement',
    wide: true,
    fields: [
      { name: 'title', label: 'Title' },
      { name: 'body', label: 'Message', type: 'textarea', full: true, rows: 5 },
      { name: 'audience', label: 'Audience', type: 'select', options: [
        { value: 'ALL', label: 'Everyone' },
        { value: 'STUDENTS', label: 'Students' },
        { value: 'PARENTS', label: 'Parents' },
        { value: 'TEACHERS', label: 'Teachers' },
        { value: 'STAFF', label: 'Staff' }
      ] }
    ],
    onSubmit: async (values) => {
      await request(ENDPOINTS.notices, { method: 'POST', body: { title: values.title, body: values.body, audience: values.audience, audienceClassId: null } });
      toast('Announcement posted through the API.');
      await refresh();
    }
  });
}

async function showAllNotices(context) {
  try {
    const result = await listRequest(ENDPOINTS.notices);
    const body = previewList(sortRecent(result.items, ['postedAt', 'createdAt']), (item) => node('div', { class: 'preview-row' }, [
      node('div', { class: 'preview-main' }, [
        node('div', { class: 'preview-title', text: valueOf(item, ['title', 'headline'], 'Untitled notice') }),
        node('div', { class: 'preview-meta', text: formatDate(dateOf(item, ['postedAt', 'createdAt'])) }),
        node('div', { class: 'table-secondary', text: valueOf(item, ['body', 'message', 'content'], '') })
      ]),
      actionLink('Delete', async () => {
        const ok = await confirmDialog('Delete announcement', 'This permanently removes the announcement for every role.', 'Delete');
        if (!ok) return;
        try {
          await request(`${ENDPOINTS.notices}/${encodeURIComponent(idFor(item))}`, { method: 'DELETE' });
          toast('Announcement deleted through the API.');
          closeModal();
          context.refresh();
        } catch (error) {
          toast(responseError(error), 'error');
        }
      }, 'table-action danger')
    ]), 'No announcements were returned by the API.');
    openDetailModalPage('Latest Announcements', body);
  } catch (error) {
    toast(responseError(error), 'error');
  }
}

export function adminTeachers(context) {
  const root = node('div');
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading teachers…')]);
  const addButton = button([icon('plus', 16), ' Add Teacher'], 'app-button app-button-primary', () => openTeacherForm(context, null, loadTeachers));
  root.append(pageHeading('Teachers', 'Manage teaching staff and assignments', [addButton]), panel('Teachers', body, null, { flush: true }));

  const loadTeachers = async () => {
    const result = await listRequest(ENDPOINTS.teachers, { size: 1000 });
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No teachers were returned by the API.'));
      return;
    }
    clear(body).appendChild(table(['Name', 'Subject(s)', 'Class(es)', 'Status', 'Edit action'], result.items, (item, tr) => {
      tr.appendChild(node('td', {}, [node('div', { class: 'table-primary', text: valueOf(item, ['fullName', 'full_name', 'name'], '—') })]));
      tr.appendChild(node('td', { text: joined(valueOf(item, ['subjects', 'subjectNames', 'subject'], [])) }));
      tr.appendChild(node('td', { text: joined(valueOf(item, ['classes', 'classNames', 'schoolClasses'], [])) }));
      tr.appendChild(node('td', {}, [statusBadge(valueOf(item, ['status', 'enabled'], 'UNKNOWN'))]));
      tr.appendChild(node('td', {}, [actionLink('Edit', () => openTeacherForm(context, item, loadTeachers))]));
    }));
  };
  showRetry(body, loadTeachers, () => {});
  return root;
}

async function openTeacherForm(context, item, reload) {
  try {
    const [subjects, classes] = await Promise.all([loadOptions(ENDPOINTS.subjects), loadOptions(ENDPOINTS.classes)]);
    const subjectValues = optionValues(valueOf(item, ['subjects', 'teacherSubjectClasses'], []), ['subjectId', 'id']);
    const classValues = optionValues(valueOf(item, ['classes', 'teacherClasses'], []), ['classId', 'id']);
    const classTeacherClassId = valueOf(item, ['classTeacherSchoolClassId'], '');
    openFormModal({
      title: item ? 'Edit Teacher' : 'Add Teacher',
      submitLabel: item ? 'Update teacher' : 'Create teacher',
      wide: true,
      values: {
        fullName: valueOf(item, ['fullName', 'full_name', 'name'], ''),
        email: valueOf(item, ['email', 'emailAddress'], ''),
        phone: valueOf(item, ['phone', 'phoneNumber'], ''),
        subjectIds: subjectValues,
        classIds: classValues,
        classTeacherSchoolClassId: classTeacherClassId
      },
      fields: [
        { name: 'fullName', label: 'Full name' },
        { name: 'email', label: 'Email', type: 'email', required: !item, hint: item ? 'Email cannot be changed after the account is created.' : '' },
        { name: 'phone', label: 'Phone', type: 'tel' },
        { name: 'subjectIds', label: 'Subject(s)', type: 'select', options: subjects, multiple: true, full: true },
        { name: 'classIds', label: 'Class(es)', type: 'select', options: classes, multiple: true, full: true },
        { name: 'classTeacherSchoolClassId', label: 'Class teacher for', type: 'select', options: [{ value: '', label: 'Not a class teacher' }, ...classes], required: false, full: true, hint: 'Class teachers can confirm their class roster.' }
      ],
      onSubmit: async (values) => {
        const payload = {
          fullName: values.fullName,
          phone: values.phone,
          subjectIds: values.subjectIds,
          schoolClassIds: values.classIds,
          classTeacherSchoolClassId: values.classTeacherSchoolClassId ? Number(values.classTeacherSchoolClassId) : null
        };
        if (!item) payload.email = values.email;
        const id = idFor(item);
        const created = await request(id ? `${ENDPOINTS.teachers}/${encodeURIComponent(id)}` : ENDPOINTS.teachers, { method: id ? 'PUT' : 'POST', body: payload });
        toast(id ? 'Teacher updated from the API response.' : 'Teacher created from the API response.');
        if (!id && valueOf(created, ['temporaryPassword'], '')) {
          showCredentials('Teacher account created', [
            { label: 'Teacher name', value: valueOf(created, ['fullName'], '') },
            { label: 'Email', value: valueOf(created, ['email'], '') },
            { label: 'Temporary password', value: valueOf(created, ['temporaryPassword'], '') }
          ]);
        }
        await reload();
      }
    });
  } catch (error) {
    toast(responseError(error), 'error');
  }
}

export function adminAccountants(context) {
  const root = node('div');
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading accountants…')]);
  const addButton = button([icon('plus', 16), ' Add Accountant'], 'app-button app-button-primary', () => openAccountantForm(context, null, loadAccountants));
  root.append(pageHeading('Accountants', 'Manage accountant accounts', [addButton]), panel('Accountants', body, null, { flush: true }));

  const loadAccountants = async () => {
    const result = await listRequest(ENDPOINTS.accountants, { size: 1000 });
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No accountants were returned by the API.'));
      return;
    }
    clear(body).appendChild(table(['Name', 'Status', 'Edit action'], result.items, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: valueOf(item, ['fullName', 'full_name', 'name'], '—') }));
      tr.appendChild(node('td', {}, [statusBadge(valueOf(item, ['status', 'enabled'], 'UNKNOWN'))]));
      tr.appendChild(node('td', {}, [actionLink('Edit', () => openAccountantForm(context, item, loadAccountants))]));
    }));
  };
  showRetry(body, loadAccountants, () => {});
  return root;
}

async function openAccountantForm(context, item, reload) {
  try {
    openFormModal({
      title: item ? 'Edit Accountant' : 'Add Accountant',
      submitLabel: item ? 'Update accountant' : 'Create accountant',
      values: {
        fullName: valueOf(item, ['fullName', 'full_name', 'name'], ''),
        email: valueOf(item, ['email', 'emailAddress'], ''),
        phone: valueOf(item, ['phone', 'phoneNumber'], '')
      },
      fields: [
        { name: 'fullName', label: 'Full name' },
        { name: 'email', label: 'Email', type: 'email', required: !item, hint: item ? 'Email cannot be changed after the account is created.' : '' },
        { name: 'phone', label: 'Phone', type: 'tel' }
      ],
      onSubmit: async (values) => {
        const id = idFor(item);
        const payload = item ? { fullName: values.fullName, phone: values.phone } : { fullName: values.fullName, email: values.email, phone: values.phone };
        const created = await request(id ? `${ENDPOINTS.accountants}/${encodeURIComponent(id)}` : ENDPOINTS.accountants, { method: id ? 'PUT' : 'POST', body: payload });
        toast(id ? 'Accountant updated from the API response.' : 'Accountant created from the API response.');
        if (!id && valueOf(created, ['temporaryPassword'], '')) {
          showCredentials('Accountant account created', [
            { label: 'Accountant name', value: valueOf(created, ['fullName'], '') },
            { label: 'Email', value: valueOf(created, ['email'], '') },
            { label: 'Temporary password', value: valueOf(created, ['temporaryPassword'], '') }
          ]);
        }
        await reload();
      }
    });
  } catch (error) {
    toast(responseError(error), 'error');
  }
}

export function adminStudents(context) {
  const root = node('div');
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading students…')]);
  let selectedClass = '';
  const classFilter = select('adminStudentClass', [{ value: '', label: 'All Classes' }, ...CLASS_OPTIONS.map((name) => ({ value: name, label: name }))], '', { 'aria-label': 'Filter students by class' });
  classFilter.addEventListener('change', () => {
    selectedClass = classFilter.value;
    loadStudents();
  });
  root.append(pageHeading('Students', 'School-wide student directory', [classFilter]), panel('Students', body, null, { flush: true }));

  const loadStudents = async () => {
    const query = selectedClass ? { className: selectedClass, size: 1000 } : { size: 1000 };
    const result = await listRequest(ENDPOINTS.students, query);
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No students were returned by the API for this filter.'));
      return;
    }
    clear(body).appendChild(table(['Name', 'Class', 'Roster Confirmed', 'Admission Date', 'View action'], result.items, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: studentLabel(item) }));
      tr.appendChild(node('td', { text: classLabel(item) }));
      const confirmed = valueOf(item, ['rosterConfirmed', 'roster_confirmed'], false);
      tr.appendChild(node('td', {}, [node('span', { class: `roster-badge ${confirmed ? 'roster-yes' : 'roster-no'}`, text: confirmed ? 'Yes' : 'No' })]));
      tr.appendChild(node('td', { text: formatDate(dateOf(item, ['admissionDate', 'admission_date'])) }));
      tr.appendChild(node('td', {}, [actionLink('View', () => openStudentDetail(item))]));
    }));
  };
  showRetry(body, loadStudents, () => {});
  return root;
}

function openStudentDetail(item) {
  openDetailModal('Student Details', [
    pageDetailItem('Name', studentLabel(item)),
    pageDetailItem('Class', classLabel(item)),
    pageDetailItem('Roster Confirmed', valueOf(item, ['rosterConfirmed'], false) ? 'Yes' : 'No'),
    pageDetailItem('Admission date', formatDate(dateOf(item, ['admissionDate', 'admission_date']))),
    pageDetailItem('Date of birth', formatDate(valueOf(item, ['dateOfBirth', 'date_of_birth', 'dob'], ''))),
    pageDetailItem('Gender', valueOf(item, ['gender'], '—'))
  ]);
}

export function adminEnrollment(context) {
  const root = node('div');
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading enrollment requests…')]);
  let filter = 'PENDING';
  const tabs = node('div', { class: 'filter-tabs' });
  const tabDefs = [
    ['PENDING', 'Pending'],
    ['APPROVED', 'Approved'],
    ['REJECTED', 'Rejected'],
    ['ALL', 'All']
  ];
  const renderTabs = () => {
    clear(tabs);
    tabDefs.forEach(([value, label]) => {
      const tab = button(label, `filter-tab${filter === value ? ' active' : ''}`, () => {
        filter = value;
        renderTabs();
        loadRequests();
      });
      tabs.appendChild(tab);
    });
  };
  renderTabs();
  root.append(pageHeading('Enrollment Requests', 'Review applications before account creation'), tabs, panel('Applications', body, null, { flush: true }));

  const loadRequests = async () => {
    const query = { size: 1000 };
    if (filter !== 'ALL') query.status = filter;
    const result = await listRequest(ENDPOINTS.enrollment, query);
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No enrollment applications were returned by the API for this filter.'));
      return;
    }
    clear(body).appendChild(table(['Applicant', 'Applying For', 'Submitted Date', 'Status', 'Review action'], result.items, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: parentLabel(item) || studentLabel(item) }));
      tr.appendChild(node('td', { text: classLabel(item) || valueOf(item, ['applyingForClassName', 'applyingForClass'], '—') }));
      tr.appendChild(node('td', { text: formatDate(dateOf(item, ['submittedAt', 'createdAt'])) }));
      tr.appendChild(node('td', {}, [statusBadge(valueOf(item, ['status'], 'UNKNOWN'))]));
      tr.appendChild(node('td', {}, [actionLink('Review', () => openEnrollmentDetail(item, loadRequests))]));
    }));
    const requestedId = pageQueryId();
    if (requestedId) {
      const match = result.items.find((item) => String(idFor(item)) === String(requestedId));
      if (match) openEnrollmentDetail(match, loadRequests);
    }
  };
  showRetry(body, loadRequests, () => {});
  return root;
}

function openEnrollmentDetail(item, reload) {
  const status = String(valueOf(item, ['status'], '')).toUpperCase();
  const actions = [];
  if (status === 'PENDING') {
    actions.push(button('Approve', 'app-button app-button-lime', async () => {
      const confirmed = await confirmDialog('Approve enrollment application', 'Approving creates the student and parent accounts through the API.', 'Approve');
      if (!confirmed) return;
      setBusy(actions[0], true, 'Approving…');
      try {
        const approval = await request(`${ENDPOINTS.enrollment}/${encodeURIComponent(idFor(item))}`, { method: 'PATCH', body: { status: 'APPROVED' } });
        closeModal();
        toast('Enrollment approval recorded by the API.');
        showCredentials('Enrollment approved', [
          { label: 'Student', value: valueOf(approval, ['studentEmail'], '') },
          { label: 'Parent / guardian', value: valueOf(approval, ['parentEmail'], '') },
          { label: 'Temporary password', value: valueOf(approval, ['temporaryPassword'], '') },
          { label: 'Email delivery', value: valueOf(approval, ['emailDelivery'], '') }
        ]);
        await reload();
      } catch (error) {
        toast(responseError(error), 'error');
      }
    }));
    actions.push(button('Reject', 'app-button app-button-danger', async () => {
      const confirmed = await confirmDialog('Reject enrollment application', 'This status change will be sent to the API. Continue?', 'Reject');
      if (!confirmed) return;
      try {
        await request(`${ENDPOINTS.enrollment}/${encodeURIComponent(idFor(item))}`, { method: 'PATCH', body: { status: 'REJECTED' } });
        closeModal();
        toast('Enrollment rejection recorded by the API.');
        await reload();
      } catch (error) {
        toast(responseError(error), 'error');
      }
    }));
  }
  openDetailModal('Enrollment Application', [
    pageDetailItem('Parent / guardian', parentLabel(item)),
    pageDetailItem('Email', valueOf(item, ['parentEmail', 'email'], '—')),
    pageDetailItem('Phone', valueOf(item, ['parentPhone', 'phone'], '—')),
    pageDetailItem('Child', studentLabel(item)),
    pageDetailItem('Date of birth', formatDate(valueOf(item, ['studentDateOfBirth', 'dateOfBirth', 'date_of_birth'], ''))),
    pageDetailItem('Applying for', classLabel(item) || valueOf(item, ['applyingForClassName', 'applyingForClass'], '—')),
    pageDetailItem('Submitted', formatDate(dateOf(item, ['submittedAt', 'createdAt']))),
    pageDetailItem('Status', formatTerm(valueOf(item, ['status'], 'UNKNOWN')))
  ], actions);
}

export function adminFees(context) {
  const root = node('div');
  const stats = statGrid([
    { key: 'collected', label: 'Total Collected This Term', value: 'Loading…' },
    { key: 'outstanding', label: 'Total Outstanding', value: 'Loading…' },
    { key: 'invoices', label: 'Invoices Created This Term', value: 'Loading…' },
    { key: 'overdue', label: 'Overdue Count', value: 'Loading…' }
  ]);
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading fee summary…')]);
  root.append(pageHeading('Fees & Payments', 'Read-only administration oversight'), stats, panel('Fee Summary', body, null, { flush: true }));
  const load = async () => {
    try {
      const [dashboard, outstanding] = await Promise.all([
        request(`${ENDPOINTS.dashboard}/admin`),
        listRequest(ENDPOINTS.outstanding)
      ]);
      setStatOrUnavailable(stats, 'collected', formatMoney(numeric(metricFrom(dashboard, 'feesCollectedThisTerm'))));
      setStatOrUnavailable(stats, 'outstanding', formatMoney(numeric(metricFrom(dashboard, 'totalOutstanding'))));
      setStatOrUnavailable(stats, 'invoices', metricFrom(dashboard, 'invoicesCreatedThisTerm'));
      setStatOrUnavailable(stats, 'overdue', metricFrom(dashboard, 'overdueCount'));
      const groups = new Map();
      outstanding.items.forEach((row) => {
        const key = `${classLabel(row) || '—'}|${termLabel(row) || '—'}`;
        const group = groups.get(key) || { className: classLabel(row) || '—', term: termLabel(row) || '—', invoiced: 0, collected: 0, students: 0 };
        group.invoiced += numeric(amountPaidOf(row)) + numeric(valueOf(row, ['amountOwed'], 0));
        group.collected += numeric(amountPaidOf(row));
        group.students += 1;
        groups.set(key, group);
      });
      const rows = [...groups.values()].map((group) => ({ ...group, outstanding: Math.max(group.invoiced - group.collected, 0) }));
      if (!rows.length) {
        clear(body).appendChild(emptyState('No outstanding fee balances were returned by the API for the current term.'));
        return;
      }
      clear(body).appendChild(table(['Class', 'Term', 'Students Owed', 'Amount Invoiced', 'Amount Collected', 'Outstanding'], rows, (row, tr) => {
        tr.appendChild(node('td', { class: 'table-primary', text: row.className }));
        tr.appendChild(node('td', { text: formatTerm(row.term) }));
        tr.appendChild(node('td', { text: String(row.students) }));
        tr.appendChild(node('td', { text: formatMoney(row.invoiced) }));
        tr.appendChild(node('td', { text: formatMoney(row.collected) }));
        tr.appendChild(node('td', { text: formatMoney(row.outstanding) }));
      }));
    } catch (error) {
      ['collected', 'outstanding', 'invoices', 'overdue'].forEach((key) => setStatOrUnavailable(stats, key, undefined));
      clear(body).appendChild(errorState(responseError(error), load));
    }
  };
  showRetry(body, load, () => {});
  load().catch(() => {});
  return root;
}

function applyBranding(profile) {
  const name = valueOf(profile, ['schoolName', 'name'], '');
  const color = valueOf(profile, ['primaryBrandColor', 'primaryColor', 'brandColor'], '');
  if (/^#[0-9a-fA-F]{3,8}$/.test(color)) {
    document.documentElement.style.setProperty('--color-navy', color);
    document.documentElement.style.setProperty('--color-blue', color);
  }
  if (name) {
    document.querySelectorAll('.sidebar-brand strong').forEach((nodeElement) => { nodeElement.textContent = String(name).split(/\s+/)[0].toUpperCase(); });
    document.title = `${name} | Crestwood Academy Portal`;
  }
  const logoUrl = valueOf(profile, ['logoUrl'], '');
  if (logoUrl) {
    const image = new URL(String(logoUrl), window.location.origin);
    if (image.origin === window.location.origin) {
      document.querySelectorAll('.brand-icon').forEach((nodeElement) => {
        if (nodeElement.querySelector('img')) return;
        clear(nodeElement);
        nodeElement.appendChild(node('img', { src: image.pathname, alt: 'School logo', class: 'brand-logo-image' }));
      });
    }
  }
}

export function adminSettings(context) {
  const root = node('div');
  const profileBody = node('div', {}, [loading('Loading school profile…')]);
  const academicBody = node('div', {}, [loading('Loading academic sessions and terms…')]);
  const profilePanel = panel('School Profile', profileBody);
  const academicPanel = panel('Academic Sessions & Terms', academicBody);
  root.append(pageHeading('Settings', 'School configuration only'), node('div', { class: 'panel-stack' }, [profilePanel, academicPanel]));

  const loadProfile = async () => {
    const payload = await request(`${ENDPOINTS.settings}/school-profile`);
    const profile = payload && payload.data && typeof payload.data === 'object' ? payload.data : payload;
    applyBranding(profile);
    const form = node('form', { class: 'settings-form' });
    const schoolName = input('schoolName', 'text', { value: valueOf(profile, ['schoolName', 'name'], ''), required: true });
    const logo = input('logo', 'file', { accept: 'image/*' });
    const color = input('brandColor', 'color', { value: valueOf(profile, ['primaryBrandColor', 'brandColor'], '#180D38') });
    const logoUrl = valueOf(profile, ['logoUrl'], '');
    form.append(
      field('School Name', schoolName),
      field('Primary brand color', color),
      field('Logo upload', logo, { full: true })
    );
    if (logoUrl) form.appendChild(node('div', { class: 'table-secondary full-width', text: `Current logo file: ${logoUrl}` }));
    const status = node('div');
    const save = button('Save profile', 'app-button app-button-primary', async () => {
      if (!form.reportValidity()) return;
      setBusy(save, true, 'Saving…');
      try {
        let updated = await request(`${ENDPOINTS.settings}/school-profile`, { method: 'PUT', body: { schoolName: schoolName.value, primaryBrandColor: color.value } });
        if (logo.files && logo.files[0]) {
          const formData = new FormData();
          formData.append('logo', logo.files[0]);
          updated = await request(`${ENDPOINTS.settings}/school-profile/logo`, { method: 'POST', body: formData });
        }
        applyBranding(updated);
        status.replaceChildren(node('div', { class: 'form-status success', text: 'School profile saved through the API.' }));
        toast('School profile saved and applied to this portal.');
      } catch (error) {
        status.replaceChildren(node('div', { class: 'form-status error', text: responseError(error) }));
      } finally {
        setBusy(save, false);
      }
    }, { type: 'submit' });
    form.addEventListener('submit', (event) => { event.preventDefault(); save.click(); });
    form.append(node('div', { class: 'form-actions full-width' }, [save]), status);
    clear(profileBody).appendChild(form);
  };
  showRetry(profileBody, loadProfile, () => {});

  const loadAcademic = async () => {
    const [sessions, terms] = await Promise.all([listRequest(ENDPOINTS.sessions, { size: 1000 }), listRequest(ENDPOINTS.terms, { size: 1000 })]);
    const wrapper = node('div', { class: 'settings-form' });
    const sessionList = node('div', { class: 'session-list' });
    sessions.items.forEach((session) => {
      const current = valueOf(session, ['current', 'isCurrent'], false) === true;
      const item = node('div', { class: `session-item${current ? ' current' : ''}` }, [
        node('div', {}, [node('div', { class: 'session-name', text: valueOf(session, ['name', 'sessionName'], '—') }), node('div', { class: 'session-meta', text: current ? 'Current session' : 'Academic session' })]),
        node('div', { class: 'item-actions' }, [
          actionLink('Edit', () => openSessionForm(session, loadAcademic)),
          !current ? actionLink('Mark current', async () => {
            const ok = await confirmDialog('Mark current session', 'This setting will be updated through the API.', 'Mark current');
            if (!ok) return;
            try {
              await request(`${ENDPOINTS.sessions}/${encodeURIComponent(idFor(session))}`, { method: 'PATCH', body: { current: true } });
              toast('Current session updated through the API.');
              await loadAcademic();
            } catch (error) { toast(responseError(error), 'error'); }
          }) : null
        ])
      ]);
      sessionList.appendChild(item);
    });
    const termList = node('div', { class: 'term-list' });
    terms.items.forEach((term) => {
      const current = valueOf(term, ['current', 'isCurrent'], false) === true;
      const item = node('div', { class: `term-item${current ? ' current' : ''}` }, [
        node('div', {}, [node('div', { class: 'term-name', text: termLabel(term) || valueOf(term, ['name'], '—') }), node('div', { class: 'term-meta', text: current ? 'Current term' : formatDate(valueOf(term, ['startDate', 'start_date'], '')) })]),
        node('div', { class: 'item-actions' }, [
          actionLink('Edit', () => openTermForm(term, loadAcademic)),
          !current ? actionLink('Mark current', async () => {
            const ok = await confirmDialog('Mark current term', 'This setting will be updated through the API.', 'Mark current');
            if (!ok) return;
            try {
              await request(`${ENDPOINTS.terms}/${encodeURIComponent(idFor(term))}`, { method: 'PATCH', body: { current: true } });
              toast('Current term updated through the API.');
              await loadAcademic();
            } catch (error) { toast(responseError(error), 'error'); }
          }) : null
        ])
      ]);
      termList.appendChild(item);
    });
    wrapper.append(
      node('div', {}, [node('h3', { class: 'panel-title', text: 'Sessions' }), button([icon('plus', 15), ' Create session'], 'app-button app-button-light', () => openSessionForm(null, loadAcademic)), sessionList]),
      node('div', {}, [node('h3', { class: 'panel-title', text: 'Terms' }), button([icon('plus', 15), ' Create term'], 'app-button app-button-light', () => openTermForm(null, loadAcademic)), termList])
    );
    clear(academicBody).appendChild(wrapper);
  };
  showRetry(academicBody, loadAcademic, () => {});
  return root;
}

function openSessionForm(session, reload) {
  openFormModal({
    title: session ? 'Edit Academic Session' : 'Create Academic Session',
    submitLabel: session ? 'Update session' : 'Create session',
    values: {
      name: valueOf(session, ['name', 'sessionName'], ''),
      current: valueOf(session, ['current', 'isCurrent'], false) === true
    },
    fields: [
      { name: 'name', label: 'Session name', hint: 'For example 2025/2026.' },
      { name: 'current', label: 'Set as the current session', type: 'checkbox', required: false }
    ],
    onSubmit: async (values) => {
      const id = idFor(session);
      await request(id ? `${ENDPOINTS.sessions}/${encodeURIComponent(id)}` : ENDPOINTS.sessions, { method: id ? 'PUT' : 'POST', body: { name: values.name, current: values.current } });
      toast('Academic session saved through the API.');
      await reload();
    }
  });
}

function openTermForm(term, reload) {
  openFormModal({
    title: term ? 'Edit Term' : 'Create Term',
    submitLabel: term ? 'Update term' : 'Create term',
    values: {
      academicSessionId: valueOf(term, ['academicSessionId'], ''),
      name: valueOf(term, ['name', 'termName'], ''),
      current: valueOf(term, ['current', 'isCurrent'], false) === true,
      startDate: valueOf(term, ['startDate', 'start_date'], ''),
      endDate: valueOf(term, ['endDate', 'end_date'], '')
    },
    fields: [
      { name: 'academicSessionId', label: 'Academic session ID', type: 'number', min: '1', hint: 'Numeric ID of the academic session from GET /api/v1/academic-sessions.' },
      { name: 'name', label: 'Term', type: 'select', options: TERM_OPTIONS },
      { name: 'startDate', label: 'Start date', type: 'date' },
      { name: 'endDate', label: 'End date', type: 'date' },
      { name: 'current', label: 'Set as the current term', type: 'checkbox', required: false, full: true }
    ],
    onSubmit: async (values) => {
      const id = idFor(term);
      const body = {
        academicSessionId: values.academicSessionId ? Number(values.academicSessionId) : null,
        name: values.name,
        current: values.current,
        startDate: values.startDate,
        endDate: values.endDate
      };
      await request(id ? `${ENDPOINTS.terms}/${encodeURIComponent(id)}` : ENDPOINTS.terms, { method: id ? 'PUT' : 'POST', body });
      toast('Academic term saved through the API.');
      await reload();
    }
  });
}

export const ADMIN_PAGES = {
  overview: adminOverview,
  teachers: adminTeachers,
  accountants: adminAccountants,
  students: adminStudents,
  enrollment: adminEnrollment,
  fees: adminFees,
  settings: adminSettings
};
