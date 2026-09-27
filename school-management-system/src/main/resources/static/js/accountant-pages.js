import { ENDPOINTS, listRequest, loadJsPdf, metricFrom, numeric, request } from './api.js';
import {
  actionLink,
  button,
  clear,
  closeModal,
  confirmDialog,
  downloadBlob,
  emptyState,
  errorState,
  field,
  formatDate,
  formatMoney,
  formatTerm,
  input,
  loading,
  node,
  panel,
  pageHeading,
  previewList,
  select,
  setBusy,
  statGrid,
  statusBadge,
  table,
  toast,
  updateStat,
  methodBadge
} from './ui.js';
import {
  amountOwedOf,
  amountPaidOf,
  classId,
  classLabel,
  currentOption,
  dateOf,
  idFor,
  invoiceAmount,
  loadOptions,
  openFormModal,
  showRetry,
  sortByDateDesc,
  studentLabel,
  subjectLabel,
  termId,
  termLabel,
  valueOf
} from './page-utils.js';

function setStat(grid, key, value) {
  updateStat(grid, key, value === undefined || value === null || value === '' ? '—' : value);
}

function daysOverdue(item) {
  const direct = valueOf(item, ['daysOverdue', 'days_overdue'], '');
  if (direct !== '') return direct;
  const due = new Date(dateOf(item, ['dueDate']));
  if (Number.isNaN(due.getTime())) return '—';
  return Math.max(0, Math.ceil((Date.now() - due.getTime()) / 86400000));
}

function paymentStatus(item) {
  return String(valueOf(item, ['status', 'paymentStatus', 'state'], '')).toUpperCase();
}

function paymentMethod(item) {
  return String(valueOf(item, ['paymentMethod', 'method'], '')).toUpperCase().replace('_', '-');
}

function paymentClassLabel(item) {
  return classLabel(item) || valueOf(item, ['className', 'schoolClassName'], '—');
}

export function accountantOverview(context) {
  const root = node('div');
  const stats = statGrid([
    { key: 'outstanding', label: 'Total Outstanding', value: 'Loading…' },
    { key: 'collected', label: 'Collected This Term', value: 'Loading…' },
    { key: 'invoices', label: 'Invoices Created', value: 'Loading…' },
    { key: 'overdue', label: 'Overdue Count', value: 'Loading…' }
  ]);
  const recentBody = node('div', {}, [loading('Loading recent payments…')]);
  root.append(pageHeading('Overview', 'Live fee collection oversight'), stats, panel('Recent Payments', recentBody, actionLink('View All →', () => context.navigate('ledger'), 'panel-action')));
  const load = async () => {
    const [dashboard, ledger] = await Promise.all([
      request(`${ENDPOINTS.dashboard}/accountant`),
      request(`${ENDPOINTS.payments}/ledger`)
    ]);
    setStat(stats, 'outstanding', formatMoney(numeric(metricFrom(dashboard, 'totalOutstanding'))));
    setStat(stats, 'collected', formatMoney(numeric(metricFrom(dashboard, 'feesCollectedThisTerm'))));
    setStat(stats, 'invoices', metricFrom(dashboard, 'invoicesCreatedThisTerm'));
    setStat(stats, 'overdue', metricFrom(dashboard, 'overdueCount'));
    const payments = Array.isArray(ledger) ? ledger : (ledger && Array.isArray(ledger.payments) ? ledger.payments : []);
    const paid = sortByDateDesc(payments.filter((item) => paymentStatus(item) === 'PAID'), ['paidAt', 'createdAt']).slice(0, 3);
    if (!paid.length) {
      clear(recentBody).appendChild(emptyState('No recent payments were returned by the API.'));
      return;
    }
    clear(recentBody).appendChild(previewList(paid, (item) => node('div', { class: 'preview-row' }, [
      node('div', { class: 'preview-main' }, [node('div', { class: 'preview-title', text: studentLabel(item) }), node('div', { class: 'preview-meta', text: `${termLabel(item) || valueOf(item, ['termName'], '—')} · ${formatDate(dateOf(item, ['paidAt', 'createdAt']))}` })]),
      node('div', { class: 'preview-value', text: formatMoney(numeric(amountPaidOf(item))) })
    ]), 'No recent payments were returned by the API.'));
  };
  showRetry(recentBody, load, () => {});
  return root;
}

export function accountantInvoices(context) {
  const root = node('div');
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading invoices…')]);
  const createButton = button([icon('plus', 16), ' Create Invoice'], 'app-button app-button-primary', () => openInvoiceForm(context, null, loadInvoices));
  root.append(pageHeading('Fee Invoices', 'Create and maintain class fee invoices', [createButton]), panel('Invoices', body, null, { flush: true }));
  const loadInvoices = async () => {
    const result = await listRequest(ENDPOINTS.invoices);
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No invoices were returned by the API.'));
      return;
    }
    clear(body).appendChild(table(['Class', 'Term', 'Amount', 'Due Date', 'Created Date', 'Edit action'], result.items, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: classLabel(item) }));
      tr.appendChild(node('td', { text: termLabel(item) || valueOf(item, ['termName'], '—') }));
      tr.appendChild(node('td', { text: formatMoney(numeric(invoiceAmount(item))) }));
      tr.appendChild(node('td', { text: formatDate(dateOf(item, ['dueDate'])) }));
      tr.appendChild(node('td', { text: formatDate(dateOf(item, ['createdAt', 'createdDate'])) }));
      tr.appendChild(node('td', {}, [actionLink('Edit', () => openInvoiceForm(context, item, loadInvoices))]));
    }));
  };
  showRetry(body, loadInvoices, () => {});
  return root;
}

async function openInvoiceForm(context, item, reload) {
  try {
    const [classes, terms] = await Promise.all([loadOptions(ENDPOINTS.classes), loadOptions(ENDPOINTS.terms)]);
    openFormModal({
      title: item ? 'Edit Fee Invoice' : 'Create Fee Invoice',
      submitLabel: item ? 'Update invoice' : 'Create invoice',
      values: {
        schoolClassId: classId(item),
        termId: termId(item),
        amount: invoiceAmount(item),
        dueDate: dateOf(item, ['dueDate'])
      },
      fields: [
        { name: 'schoolClassId', label: 'Class', type: 'select', options: classes },
        { name: 'termId', label: 'Term', type: 'select', options: terms },
        { name: 'amount', label: 'Amount per student', type: 'number', step: '0.01', min: '0.01', hint: 'This is the per-student fee for the class and term.' },
        { name: 'dueDate', label: 'Due date', type: 'date' }
      ],
      onSubmit: async (values) => {
        const payload = { schoolClassId: Number(values.schoolClassId), termId: Number(values.termId), amount: Number(values.amount), dueDate: values.dueDate };
        const id = idFor(item);
        await request(id ? `${ENDPOINTS.invoices}/${encodeURIComponent(id)}` : ENDPOINTS.invoices, { method: id ? 'PUT' : 'POST', body: payload });
        toast(id ? 'Invoice updated through the API.' : 'Invoice created through the API.');
        await reload();
      }
    });
  } catch (error) {
    toast(responseError(error), 'error');
  }
}

export function accountantOutstanding(context) {
  const root = node('div');
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading outstanding fees…')]);
  root.append(pageHeading('Outstanding Fees', 'Students with unpaid fee balances'), panel('Defaulters', body, null, { flush: true }));
  const load = async () => {
    const result = await listRequest(ENDPOINTS.outstanding);
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No outstanding fees were returned by the API.'));
      return;
    }
    clear(body).appendChild(table(['Student', 'Class', 'Amount Owed', 'Days Overdue', 'Send Reminder action'], result.items, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: studentLabel(item) }));
      tr.appendChild(node('td', { text: classLabel(item) }));
      tr.appendChild(node('td', { text: formatMoney(numeric(amountOwedOf(item))) }));
      tr.appendChild(node('td', { text: String(daysOverdue(item)) }));
      const reminder = actionLink('Send Reminder', async () => {
        const ok = await confirmDialog('Send fee reminder', 'The API records the reminder and emails the linked parent or guardian when SMTP is enabled.', 'Send reminder');
        if (!ok) return;
        setBusy(reminder, true, 'Sending…');
        try {
          const result = await request(ENDPOINTS.reminders, { method: 'POST', body: { studentId: valueOf(item, ['studentId', 'id'], ''), invoiceId: valueOf(item, ['invoiceId', 'feeInvoiceId'], '') } });
          toast(getFieldMessage(result) || 'Reminder recorded by the API.');
        } catch (error) {
          toast(responseError(error), 'error');
        } finally {
          setBusy(reminder, false);
        }
      });
      tr.appendChild(node('td', {}, [reminder]));
    }));
  };
  showRetry(body, load, () => {});
  return root;
}

function getFieldMessage(payload) {
  const message = valueOf(payload, ['message'], '');
  return typeof message === 'string' ? message : '';
}

export function accountantLedger(context) {
  const root = node('div');
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading payment ledger…')]);
  const recordButton = button([icon('plus', 16), ' Record Manual Payment'], 'app-button app-button-primary', () => openManualPaymentForm(load));
  root.append(pageHeading('Payment Ledger', 'Review and reconcile payment records', [recordButton]), panel('Payment Ledger', body, null, { flush: true }));
  const load = async () => {
    const ledger = await request(`${ENDPOINTS.payments}/ledger`);
    const payments = Array.isArray(ledger) ? ledger : (ledger && Array.isArray(ledger.payments) ? ledger.payments : []);
    if (!payments.length) {
      clear(body).appendChild(emptyState('No payment records were returned by the API.'));
      return;
    }
    const rows = sortByDateDesc(payments, ['createdAt', 'paidAt']);
    clear(body).appendChild(table(['Student', 'Class / Term', 'Amount Paid', 'Method', 'Date', 'Status', 'Mark as Paid'], rows, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: studentLabel(item) }));
      tr.appendChild(node('td', { text: `${paymentClassLabel(item)} / ${termLabel(item) || valueOf(item, ['termName'], '—')}` }));
      tr.appendChild(node('td', { text: formatMoney(numeric(amountPaidOf(item))) }));
      tr.appendChild(node('td', {}, [methodBadge(paymentMethod(item))]));
      tr.appendChild(node('td', { text: formatDate(dateOf(item, ['paidAt', 'createdAt'])) }));
      tr.appendChild(node('td', {}, [statusBadge(valueOf(item, ['status', 'paymentStatus'], 'UNKNOWN'))]));
      const canMark = ['CASH', 'BANK-TRANSFER'].includes(paymentMethod(item)) && paymentStatus(item) === 'PENDING';
      const action = actionLink('Mark as Paid', async () => {
        const ok = await confirmDialog('Mark payment as paid', 'This records the cash or bank transfer as received in the ledger.', 'Mark as paid');
        if (!ok) return;
        setBusy(action, true, 'Saving…');
        try {
          await request(`${ENDPOINTS.payments}/${encodeURIComponent(idFor(item))}`, { method: 'PATCH', body: { status: 'PAID' } });
          toast('Payment marked as paid through the API.');
          await load();
        } catch (error) {
          toast(responseError(error), 'error');
        } finally {
          setBusy(action, false);
        }
      }, 'table-action');
      action.disabled = !canMark;
      if (!canMark) action.setAttribute('aria-disabled', 'true');
      tr.appendChild(node('td', {}, [action]));
    }));
  };
  showRetry(body, load, () => {});
  return root;
}

async function openManualPaymentForm(reload) {
  try {
    const [students, invoices] = await Promise.all([listRequest(ENDPOINTS.students), listRequest(ENDPOINTS.invoices)]);
    if (!students.items.length) {
      toast('No students were returned by the API.', 'error');
      return;
    }
    const studentOptions = students.items.map((student) => ({ value: valueOf(student, ['id'], ''), label: `${studentLabel(student)} — ${classLabel(student) || '—'}` }));
    const invoiceOptions = invoices.items.map((invoice) => ({ value: valueOf(invoice, ['id'], ''), label: `${classLabel(invoice) || 'Invoice'} · ${termLabel(invoice) || '—'} · ${formatMoney(numeric(invoiceAmount(invoice)))}` }));
    openFormModal({
      title: 'Record Manual Payment',
      submitLabel: 'Record payment',
      wide: true,
      fields: [
        { name: 'studentId', label: 'Student', type: 'select', options: studentOptions, full: true },
        { name: 'feeInvoiceId', label: 'Fee invoice', type: 'select', options: invoiceOptions, full: true },
        { name: 'amountPaid', label: 'Amount received', type: 'number', step: '0.01', min: '0.01' },
        { name: 'paymentMethod', label: 'Method', type: 'select', options: [{ value: 'CASH', label: 'Cash' }, { value: 'BANK_TRANSFER', label: 'Bank transfer' }] }
      ],
      onSubmit: async (values) => {
        await request(`${ENDPOINTS.payments}/manual`, {
          method: 'POST',
          body: {
            studentId: Number(values.studentId),
            feeInvoiceId: Number(values.feeInvoiceId),
            amountPaid: Number(values.amountPaid),
            paymentMethod: values.paymentMethod
          }
        });
        toast('Manual payment recorded as pending. Mark it as paid after confirming the money.');
        await reload();
      }
    });
  } catch (error) {
    toast(responseError(error), 'error');
  }
}

function csvCell(value) {
  let text = value === null || value === undefined ? '' : String(value);
  if (/^[=+\-@\t\r]/.test(text)) text = `'${text}`;
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

function makeCsv(headers, rows) {
  return `\uFEFF${[headers, ...rows].map((row) => row.map(csvCell).join(',')).join('\n')}`;
}

function addPdfTable(doc, headers, rows, startY = 40) {
  doc.setFontSize(9);
  let y = startY;
  doc.text(headers.join(' | '), 14, y);
  y += 7;
  rows.forEach((row) => {
    if (y > 275) {
      doc.addPage();
      y = 20;
    }
    doc.text(row.map((value) => String(value ?? '')).join(' | '), 14, y);
    y += 7;
  });
}

function withinDateRange(item, from, to, fields) {
  if (!from && !to) return true;
  const raw = valueOf(item, fields, '');
  if (!raw) return false;
  const time = new Date(raw).getTime();
  if (!Number.isFinite(time)) return false;
  if (from && time < new Date(`${from}T00:00:00`).getTime()) return false;
  if (to && time > new Date(`${to}T23:59:59`).getTime()) return false;
  return true;
}

async function collectReportRows(kind, filters) {
  if (kind === 'results') {
    const result = await listRequest(ENDPOINTS.results, { classId: filters.classId || undefined, termId: filters.termId || undefined });
    return result.items.filter((item) => withinDateRange(item, filters.from, filters.to, ['enteredAt']));
  }
  const ledger = await request(`${ENDPOINTS.payments}/ledger`);
  const payments = Array.isArray(ledger) ? ledger : (ledger && Array.isArray(ledger.payments) ? ledger.payments : []);
  return payments.filter((item) => withinDateRange(item, filters.from, filters.to, ['paidAt', 'createdAt']));
}

function reportFileName(kind, format) {
  return `crestwood-${kind === 'results' ? 'results' : 'collection'}-report.${format}`;
}

async function exportResultsPdf(rows) {
  const jsPdf = await loadJsPdf();
  const Ctor = window.jspdf && window.jspdf.jsPDF ? window.jspdf.jsPDF : jsPdf.jsPDF;
  const doc = new Ctor();
  doc.setFontSize(16);
  doc.text('Crestwood Academy Results Report', 14, 20);
  doc.setFontSize(9);
  doc.text(`Generated ${formatDate(new Date().toISOString())}`, 14, 28);
  addPdfTable(doc, ['Student', 'Subject', 'CA1', 'CA2', 'Exam', 'Total', 'Grade'], rows.map((item) => [studentLabel(item), subjectLabel(item), valueOf(item, ['ca1'], ''), valueOf(item, ['ca2'], ''), valueOf(item, ['exam'], ''), valueOf(item, ['total'], ''), valueOf(item, ['grade'], '')]));
  doc.save(reportFileName('results', 'pdf'));
}

async function exportCollection(rows, format) {
  if (format === 'csv') {
    const headers = ['Student', 'Class', 'Term', 'Amount', 'Method', 'Date', 'Status'];
    const body = rows.map((item) => [studentLabel(item), paymentClassLabel(item), termLabel(item) || valueOf(item, ['termName'], ''), numeric(amountPaidOf(item)), paymentMethod(item), dateOf(item, ['paidAt', 'createdAt']), paymentStatus(item)]);
    downloadBlob(new Blob([makeCsv(headers, body)], { type: 'text/csv;charset=utf-8' }), reportFileName('collection', 'csv'));
    return;
  }
  const jsPdf = await loadJsPdf();
  const Ctor = window.jspdf && window.jspdf.jsPDF ? window.jspdf.jsPDF : jsPdf.jsPDF;
  const doc = new Ctor();
  doc.setFontSize(16);
  doc.text('Crestwood Academy Collection Report', 14, 20);
  doc.setFontSize(9);
  doc.text(`Generated ${formatDate(new Date().toISOString())}`, 14, 28);
  addPdfTable(doc, ['Student', 'Class', 'Term', 'Amount', 'Method', 'Date', 'Status'], rows.map((item) => [studentLabel(item), paymentClassLabel(item), termLabel(item) || valueOf(item, ['termName'], ''), numeric(amountPaidOf(item)), paymentMethod(item), dateOf(item, ['paidAt', 'createdAt']), paymentStatus(item)]));
  doc.save(reportFileName('collection', 'pdf'));
}

export function accountantReports(context) {
  const root = node('div');
  const filterPanelBody = node('div');
  const filters = { classId: '', termId: '', from: '', to: '' };
  const classFilter = select('reportClass', [], '', { 'aria-label': 'Class' });
  const termFilter = select('reportTerm', [], '', { 'aria-label': 'Term' });
  const fromInput = input('reportFrom', 'date');
  const toInput = input('reportTo', 'date');
  const filterForm = node('div', { class: 'report-filters' }, [field('Class', classFilter), field('Term', termFilter), field('From', fromInput), field('To', toInput)]);
  const readFilters = () => {
    filters.classId = classFilter.value;
    filters.termId = termFilter.value;
    filters.from = fromInput.value;
    filters.to = toInput.value;
  };
  const runExport = async (label, kind, format, trigger) => {
    setBusy(trigger, true, 'Exporting…');
    try {
      readFilters();
      const rows = await collectReportRows(kind, filters);
      if (!rows.length) throw new Error('The API returned no rows for this report filter.');
      if (kind === 'results') await exportResultsPdf(rows);
      else await exportCollection(rows, format);
      toast(`${label} exported from live API data.`);
    } catch (error) {
      toast(responseError(error), 'error');
    } finally {
      setBusy(trigger, false);
    }
  };
  const resultsButton = button('Export Results Report (PDF)', 'app-button app-button-primary', (event) => runExport('Results PDF', 'results', 'pdf', event.currentTarget));
  const collectionPdfButton = button('Export Collection Report (PDF)', 'app-button app-button-blue', (event) => runExport('Collection PDF', 'collection', 'pdf', event.currentTarget));
  const collectionCsvButton = button('Export Collection Report (CSV)', 'app-button app-button-light', (event) => runExport('Collection CSV', 'collection', 'csv', event.currentTarget));
  const actions = node('div', { class: 'report-actions' }, [resultsButton, collectionPdfButton, collectionCsvButton]);
  filterPanelBody.append(filterForm, actions);
  root.append(pageHeading('Reports', 'Export live API data as PDF or CSV'), panel('Report Filters', filterPanelBody));
  const loadOptionsAndData = async () => {
    const [classes, terms] = await Promise.all([loadOptions(ENDPOINTS.classes), loadOptions(ENDPOINTS.terms)]);
    const classOptions = [{ value: '', label: 'All Classes' }, ...classes];
    const termOptions = [{ value: '', label: 'All Terms' }, ...terms];
    clear(classFilter);
    classOptions.forEach((option) => classFilter.appendChild(node('option', { value: option.value, text: option.label })));
    clear(termFilter);
    termOptions.forEach((option) => termFilter.appendChild(node('option', { value: option.value, text: option.label })));
  };
  showRetry(filterPanelBody, loadOptionsAndData, () => {});
  return root;
}

export const ACCOUNTANT_PAGES = {
  overview: accountantOverview,
  invoices: accountantInvoices,
  outstanding: accountantOutstanding,
  ledger: accountantLedger,
  reports: accountantReports
};
