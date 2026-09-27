import {
  downloadRequest,
  getField,
  idOf,
  listRequest,
  listRequestFirst,
  request,
  requestFirst
} from './api.js';
import {
  actionLink,
  appendChildren,
  button,
  clear,
  closeModal,
  confirmDialog,
  downloadBlob,
  emptyState,
  errorState,
  field,
  filenameFromResponse,
  formStatus,
  input,
  loading,
  node,
  openModal,
  responseError,
  safeUrl,
  select,
  setBusy,
  textarea,
  toast
} from './ui.js';

export function valueOf(item, keys, fallback = '') {
  return getField(item, keys, fallback);
}

export function idFor(item) {
  return idOf(item);
}

export function classLabel(item) {
  const direct = valueOf(item, ['schoolClassName', 'className', 'class', 'schoolClass'], '');
  if (direct && typeof direct === 'object') return valueOf(direct, ['name', 'className'], '');
  return direct;
}

export function classId(item) {
  const direct = valueOf(item, ['schoolClassId', 'classId', 'school_class_id'], '');
  if (direct && typeof direct === 'object') return idOf(direct);
  return direct;
}

export function subjectLabel(item) {
  const direct = valueOf(item, ['subjectName', 'subject', 'subject_name'], '');
  if (direct && typeof direct === 'object') return valueOf(direct, ['name', 'subjectName'], '');
  return direct;
}

export function subjectId(item) {
  const direct = valueOf(item, ['subjectId', 'subject_id'], '');
  if (direct && typeof direct === 'object') return idOf(direct);
  return direct;
}

export function termLabel(item) {
  const direct = valueOf(item, ['termName', 'term', 'term_name', 'name'], '');
  if (direct && typeof direct === 'object') return valueOf(direct, ['name', 'termName'], '');
  return direct;
}

export function termId(item) {
  const direct = valueOf(item, ['termId', 'term_id'], '');
  if (direct && typeof direct === 'object') return idOf(direct);
  return direct;
}

export function studentLabel(item) {
  return valueOf(item, ['studentName', 'studentFullName', 'fullName', 'full_name', 'name'], '');
}

export function teacherLabel(item) {
  return valueOf(item, ['teacherName', 'teacherFullName', 'fullName', 'full_name', 'name'], '');
}

export function parentLabel(item) {
  return valueOf(item, ['parentFullName', 'parentName', 'fullName', 'full_name', 'name'], '');
}

export function invoiceLabel(item) {
  return valueOf(item, ['invoiceNumber', 'invoiceNo', 'invoice', 'reference'], idFor(item));
}

export function amountOf(item) {
  return valueOf(item, ['amountOwed', 'amountDue', 'amountPaid', 'amount', 'totalAmount', 'outstandingAmount'], '');
}

export function invoiceAmount(item) {
  return valueOf(item, ['amount', 'totalAmount'], '');
}

export function amountPaidOf(item) {
  return valueOf(item, ['amountPaid', 'amount', 'totalAmount'], '');
}

export function amountOwedOf(item) {
  return valueOf(item, ['amountOwed', 'outstanding', 'outstandingAmount', 'amountDue'], '');
}

export function moneyNumber(item, reader = amountOf) {
  const value = reader(item);
  if (typeof value === 'number') return value;
  const number = Number(String(value ?? '').replace(/[^0-9.-]/g, ''));
  return Number.isFinite(number) ? number : 0;
}

export function sortByDateDesc(items, fields) {
  return [...(items || [])].sort((left, right) => {
    const leftValue = fields.map((key) => new Date(valueOf(left, [key], '')).getTime()).find(Number.isFinite) || 0;
    const rightValue = fields.map((key) => new Date(valueOf(right, [key], '')).getTime()).find(Number.isFinite) || 0;
    return rightValue - leftValue;
  });
}

export function dateOf(item, keys = ['dueDate', 'createdAt', 'createdDate', 'submittedAt', 'uploadedAt', 'paidAt', 'admissionDate']) {
  return valueOf(item, keys, '');
}

export function optionValue(item, keys = ['id', 'value']) {
  return valueOf(item, keys, '');
}

export function optionLabel(item, keys = ['name', 'label', 'title']) {
  return valueOf(item, keys, '');
}

export function optionsFrom(items, valueKeys = ['id'], labelKeys = ['name']) {
  return (Array.isArray(items) ? items : []).map((item) => ({
    value: optionValue(item, valueKeys),
    label: optionLabel(item, labelKeys),
    current: valueOf(item, ['current', 'isCurrent'], false) === true,
    raw: item
  })).filter((option) => option.value !== '' || option.label !== '');
}

export function dedupeOptions(options) {
  const seen = new Set();
  return (Array.isArray(options) ? options : []).filter((option) => {
    const key = String(option.value);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

export function showCredentials(title, rows) {
  const details = rows.filter((row) => row.value !== undefined && row.value !== null && row.value !== '').map((row) => detailItem(row.label, row.value));
  return openDetailModal(title, [...details, detailItem('Handle this secret securely', 'Share it through a secure channel and ask the user to change it after first sign-in.')]);
}

export function addQueryValue(params, key, value) {
  if (value !== undefined && value !== null && value !== '') params[key] = value;
  return params;
}

export function currentOption(options, preferred) {
  if (preferred !== undefined && preferred !== null && options.some((option) => String(option.value) === String(preferred))) return preferred;
  const current = options.find((option) => option.current === true || option.raw?.current === true || option.raw?.isCurrent === true);
  return current ? current.value : options[0]?.value || '';
}

export function currentTermFrom(items) {
  const current = (items || []).find((item) => item.current === true || item.isCurrent === true || String(valueOf(item, ['status'], '')).toUpperCase() === 'CURRENT');
  return current || items?.[0] || null;
}

export async function loadList(path, query) {
  return (await listRequest(path, query)).items;
}

export async function loadListFirst(paths, query) {
  return (await listRequestFirst(paths, query)).items;
}

export async function loadOptions(path, query) {
  return optionsFrom(await loadList(path, query));
}

export async function loadOptionsFirst(paths, query) {
  return optionsFrom(await loadListFirst(paths, query));
}

export function panelRetry(container, loader) {
  clear(container).appendChild(loading());
  loader().catch((error) => clear(container).appendChild(errorState(responseError(error), () => panelRetry(container, loader))));
}

export function setError(container, error, retry = null) {
  clear(container).appendChild(errorState(responseError(error), retry));
}

export function setEmpty(container, message) {
  clear(container).appendChild(emptyState(message));
}

export function renderTableState(container, loader, renderer, emptyMessage) {
  clear(container).appendChild(loading());
  loader().then((items) => clear(container).appendChild(items.length ? renderer(items) : emptyState(emptyMessage))).catch((error) => clear(container).appendChild(errorState(responseError(error), () => renderTableState(container, loader, renderer, emptyMessage))));
}

export function openFormModal({ title, fields, values = {}, submitLabel = 'Save', onSubmit, wide = false }) {
  const form = node('form', { class: 'auth-form' });
  const grid = node('div', { class: 'form-grid' });
  fields.forEach((definition) => {
    const current = values[definition.name] ?? definition.value ?? (definition.type === 'checkbox' ? false : '');
    let control;
    if (definition.type === 'select') control = select(definition.name, definition.options || [], current, { required: definition.required !== false, multiple: definition.multiple === true, 'aria-label': definition.label });
    else if (definition.type === 'textarea') {
      control = textarea(definition.name, { required: definition.required !== false, rows: definition.rows || 4 });
      control.value = current;
    } else if (definition.type === 'checkbox') {
      control = input(definition.name, 'checkbox', { required: definition.required === true });
      control.checked = Boolean(current);
    } else {
      control = input(definition.name, definition.type || 'text', { required: definition.required !== false, accept: definition.accept, step: definition.step, min: definition.min, max: definition.max, autocomplete: definition.autocomplete });
      control.value = current;
    }
    grid.appendChild(field(definition.label, control, { full: definition.full === true, hint: definition.hint }));
  });
  form.appendChild(grid);
  const status = node('div');
  const submit = button(submitLabel, 'app-button app-button-primary', async () => {
    if (!form.reportValidity()) return;
    setBusy(submit, true, 'Saving…');
    clear(status);
    try {
      await onSubmit(readFormValues(form, fields), status);
      closeModal();
    } catch (error) {
      status.appendChild(formStatus(responseError(error), 'error'));
      setBusy(submit, false);
    }
  }, { type: 'submit' });
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    submit.click();
  });
  form.append(status, node('div', { class: 'form-actions' }, [submit, button('Cancel', 'app-button app-button-light', closeModal)]));
  return openModal(node('div', {}, [node('h2', { class: 'modal-title', text: title }), form]), { wide });
}

export function readFormValues(form, fields) {
  const values = {};
  fields.forEach((definition) => {
    const control = form.elements[definition.name];
    if (!control) return;
    if (definition.type === 'file') {
      values[definition.name] = control.files?.[0] || null;
      return;
    }
    if (definition.type === 'checkbox') {
      values[definition.name] = control.checked;
      return;
    }
    if (control.multiple) {
      values[definition.name] = Array.from(control.selectedOptions || []).map((option) => option.value);
      return;
    }
    values[definition.name] = control.value;
  });
  return values;
}

export function detailItem(label, value) {
  return node('div', { class: 'profile-item' }, [node('div', { class: 'profile-label', text: label }), node('div', { class: 'profile-value', text: value === null || value === undefined || value === '' ? '—' : String(value) })]);
}

export function openDetailModal(title, details, actions = []) {
  const content = node('div', {}, [node('h2', { class: 'modal-title', text: title }), node('div', { class: 'detail-grid' }, details)]);
  if (actions.length) content.appendChild(node('div', { class: 'form-actions' }, actions));
  return openModal(content, { wide: true });
}

export function choiceModal(title, message, choices) {
  return new Promise((resolve) => {
    let settled = false;
    const finish = (value) => {
      if (settled) return;
      settled = true;
      closeModal();
      resolve(value);
    };
    openModal(node('div', {}, [node('h2', { class: 'modal-title', text: title }), node('p', { class: 'confirm-copy', text: message }), node('div', { class: 'form-actions' }, choices.map((choice) => button(choice.label, choice.className || 'app-button app-button-light', () => finish(choice.value))))]));
  });
}

export async function downloadMaterial(item) {
  const id = idFor(item);
  const raw = valueOf(item, ['fileUrl', 'downloadUrl', 'url', 'file_url'], '');
  const direct = raw ? safeUrl(raw) : '';
  const path = direct
    ? (() => {
        const parsed = new URL(direct);
        return parsed.origin === window.location.origin && parsed.pathname.startsWith('/api/') ? `${parsed.pathname}${parsed.search}` : '';
      })()
    : id ? `/study-materials/${encodeURIComponent(id)}/download` : '';
  if (!path) throw new Error('The API did not provide a downloadable study material file.');
  const response = await downloadRequest(path);
  downloadBlob(await response.blob(), filenameFromResponse(response, valueOf(item, ['originalFilename', 'title', 'fileName'], 'material')));
}

export function pageQueryValue() {
  const hashQuery = window.location.hash.split('?')[1] || '';
  return new URLSearchParams(hashQuery || window.location.search).get('value') || '';
}

export function pageQueryId() {
  const hashQuery = window.location.hash.split('?')[1] || '';
  return new URLSearchParams(hashQuery || window.location.search).get('id') || '';
}

export function filterQuery(base, values) {
  const query = { ...base };
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query[key] = value;
  });
  return query;
}

export function showRetry(container, loader, render) {
  clear(container).appendChild(loading());
  loader().then((data) => render(data)).catch((error) => clear(container).appendChild(errorState(responseError(error), () => showRetry(container, loader, render))));
}

export function statusText(value) {
  return String(value || 'Unknown').replace(/[_-]+/g, ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function paymentDue(item) {
  const status = String(valueOf(item, ['status', 'paymentStatus', 'state'], '')).toUpperCase();
  return ['DUE', 'PENDING', 'UNPAID', 'OVERDUE', ''].includes(status);
}

export function aggregateBy(items, key, fields) {
  const map = new Map();
  (items || []).forEach((item) => {
    const rawGroup = valueOf(item, key, '—');
    const group = rawGroup && typeof rawGroup === 'object' ? optionLabel(rawGroup) : rawGroup;
    const label = String(group ?? '—');
    const current = map.get(label) || { label, values: {}, rows: [] };
    fields.forEach((fieldName) => { current.values[fieldName] = Number(valueOf(item, [fieldName], 0)) || 0; });
    current.rows.push(item);
    map.set(label, current);
  });
  return [...map.values()];
}
