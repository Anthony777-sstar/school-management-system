export function node(tag, attributes = {}, children = []) {
  const element = document.createElement(tag);
  Object.entries(attributes || {}).forEach(([key, value]) => {
    if (value === null || value === undefined) return;
    if (key === 'class') element.className = String(value);
    else if (key === 'text') element.textContent = String(value);
    else if (key === 'dataset') Object.entries(value).forEach(([dataKey, dataValue]) => { if (dataValue !== null && dataValue !== undefined) element.dataset[dataKey] = String(dataValue); });
    else if (key === 'style' && typeof value === 'object') Object.assign(element.style, value);
    else if (key === 'on' && typeof value === 'object') Object.entries(value).forEach(([eventName, handler]) => { if (typeof handler === 'function') element.addEventListener(eventName, handler); });
    else if (key === 'for') element.htmlFor = String(value);
    else if (key === 'checked' || key === 'disabled' || key === 'selected' || key === 'multiple') element[key] = Boolean(value);
    else if (key === 'value') element.value = value === null || value === undefined ? '' : String(value);
    else element.setAttribute(key, String(value));
  });
  appendChildren(element, children);
  return element;
}

export function appendChildren(parent, children) {
  const values = Array.isArray(children) ? children : [children];
  values.flat(Infinity).forEach((child) => {
    if (child === null || child === undefined || child === false || child === true) return;
    if (child && typeof child === 'object' && typeof child.nodeType === 'number') parent.appendChild(child);
    else parent.appendChild(document.createTextNode(String(child)));
  });
  return parent;
}

export function clear(element) {
  if (element) element.replaceChildren();
  return element;
}

export function button(label, className, onClick, attributes = {}) {
  const element = node('button', { type: 'button', class: className, ...attributes });
  if (typeof onClick === 'function') element.addEventListener('click', onClick);
  appendChildren(element, label);
  return element;
}

const iconDefinitions = {
  grid: [['rect', { x: '3', y: '3', width: '7', height: '7', rx: '1' }], ['rect', { x: '14', y: '3', width: '7', height: '7', rx: '1' }], ['rect', { x: '3', y: '14', width: '7', height: '7', rx: '1' }], ['rect', { x: '14', y: '14', width: '7', height: '7', rx: '1' }]],
  users: [['path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }], ['circle', { cx: '9', cy: '7', r: '4' }], ['path', { d: 'M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75' }]],
  wallet: [['path', { d: 'M3 7a2 2 0 0 1 2-2h14a2 2 0 1 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z' }], ['path', { d: 'M3 8h18v4h-5a2 2 0 0 0 0 4h5' }], ['path', { d: 'M16 14h.01' }]],
  cap: [['path', { d: 'm3 9 9-5 9 5-9 5z' }], ['path', { d: 'M7 12v4c2.8 2.4 7.2 2.4 10 0v-4M21 10v6' }]],
  file: [['path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }], ['polyline', { points: '14 2 14 8 20 8' }], ['line', { x1: '8', y1: '13', x2: '16', y2: '13' }], ['line', { x1: '8', y1: '17', x2: '13', y2: '17' }]],
  card: [['rect', { x: '3', y: '5', width: '18', height: '14', rx: '2' }], ['line', { x1: '3', y1: '10', x2: '21', y2: '10' }], ['line', { x1: '7', y1: '15', x2: '10', y2: '15' }]],
  settings: [['path', { d: 'M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z' }], ['path', { d: 'M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-1.7 1.7-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.04 1.56V20h-2.4v-.2a1.7 1.7 0 0 0-1.04-1.56 1.7 1.7 0 0 0-1.88.34l-.06.06-1.7-1.7.06-.06A1.7 1.7 0 0 0 8.4 15a1.7 1.7 0 0 0-1.56-1.04H6v-2.4h.84A1.7 1.7 0 0 0 8.4 10a1.7 1.7 0 0 0-.34-1.88L8 8.06l1.7-1.7.06.06a1.7 1.7 0 0 0 1.88.34A1.7 1.7 0 0 0 12.68 5.2V5h2.4v.2a1.7 1.7 0 0 0 1.04 1.56 1.7 1.7 0 0 0 1.88-.34l.06-.06 1.7 1.7-.06.06a1.7 1.7 0 0 0 .34 1.88A1.7 1.7 0 0 0 19.4 10a1.7 1.7 0 0 0 1.56 1.04h.2v2.4h-.2A1.7 1.7 0 0 0 19.4 15z' }]],
  check: [['path', { d: 'M20 6 9 17l-5-5' }], ['path', { d: 'M4 12h5' }]],
  chart: [['path', { d: 'M4 19V5M4 19h16' }], ['path', { d: 'm7 15 3-4 3 2 5-7' }], ['path', { d: 'M18 6h2v2' }]],
  folder: [['path', { d: 'M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z' }]],
  home: [['path', { d: 'm3 11 9-8 9 8' }], ['path', { d: 'M5 10v10h14V10M9 20v-6h6v6' }]],
  alert: [['path', { d: 'M10.3 3.7 2.2 18a2 2 0 0 0 1.7 3h16.2a2 2 0 0 0 1.7-3L13.7 3.7a2 2 0 0 0-3.4 0z' }], ['line', { x1: '12', y1: '9', x2: '12', y2: '13' }], ['line', { x1: '12', y1: '17', x2: '12.01', y2: '17' }]],
  ledger: [['path', { d: 'M4 4h16v16H4z' }], ['path', { d: 'M8 8h8M8 12h8M8 16h5' }]],
  download: [['path', { d: 'M12 3v12' }], ['polyline', { points: '7 10 12 15 17 10' }], ['path', { d: 'M5 21h14' }]],
  plus: [['line', { x1: '12', y1: '5', x2: '12', y2: '19' }], ['line', { x1: '5', y1: '12', x2: '19', y2: '12' }]],
  edit: [['path', { d: 'M12 20h9' }], ['path', { d: 'M16.5 3.5a2.12 2.12 0 0 1 3 3L8 18l-4 1 1-4z' }]],
  trash: [['polyline', { points: '3 6 5 6 21 6' }], ['path', { d: 'M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M10 11v6M14 11v6M6 6l1 14M18 6l-1 14' }]],
  eye: [['path', { d: 'M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z' }], ['circle', { cx: '12', cy: '12', r: '2.5' }]],
  menu: [['line', { x1: '4', y1: '6', x2: '20', y2: '6' }], ['line', { x1: '4', y1: '12', x2: '20', y2: '12' }], ['line', { x1: '4', y1: '18', x2: '20', y2: '18' }]],
  close: [['line', { x1: '6', y1: '6', x2: '18', y2: '18' }], ['line', { x1: '18', y1: '6', x2: '6', y2: '18' }]],
  chevron: [['polyline', { points: '9 18 15 12 9 6' }]],
  logout: [['path', { d: 'M10 17l5-5-5-5M15 12H3' }], ['path', { d: 'M14 3h5a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-5' }]],
  external: [['path', { d: 'M14 3h7v7M10 14 21 3' }], ['path', { d: 'M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5' }]],
  info: [['circle', { cx: '12', cy: '12', r: '9' }], ['line', { x1: '12', y1: '11', x2: '12', y2: '16' }], ['line', { x1: '12', y1: '8', x2: '12.01', y2: '8' }]]
};

export function icon(name, size = 18) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('width', String(size));
  svg.setAttribute('height', String(size));
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('fill', 'none');
  svg.setAttribute('stroke', 'currentColor');
  svg.setAttribute('stroke-width', '2');
  svg.setAttribute('stroke-linecap', 'round');
  svg.setAttribute('stroke-linejoin', 'round');
  svg.setAttribute('aria-hidden', 'true');
  (iconDefinitions[name] || iconDefinitions.grid).forEach(([tag, attributes]) => {
    const child = document.createElementNS('http://www.w3.org/2000/svg', tag);
    Object.entries(attributes).forEach(([key, value]) => child.setAttribute(key, String(value)));
    svg.appendChild(child);
  });
  return svg;
}

export function field(label, control, options = {}) {
  const wrapper = node('div', { class: `form-field${options.full ? ' full-width' : ''}` });
  const id = control.id || `field-${Math.random().toString(36).slice(2)}`;
  control.id = id;
  wrapper.appendChild(node('label', { for: id, text: label }));
  wrapper.appendChild(control);
  if (options.hint) wrapper.appendChild(node('div', { class: 'table-secondary', text: options.hint }));
  return wrapper;
}

export function input(name, type = 'text', attributes = {}) {
  return node('input', { name, type, class: 'form-control', ...attributes });
}

export function textarea(name, attributes = {}) {
  return node('textarea', { name, class: 'form-control', ...attributes });
}

export function select(name, options = [], value = '', attributes = {}) {
  const control = node('select', { name, class: 'form-control', ...attributes });
  options.forEach((option) => {
    const normalized = typeof option === 'object' ? option : { value: option, label: option };
    const optionNode = node('option', { value: normalized.value, text: normalized.label });
    if (Array.isArray(value) ? value.map(String).includes(String(normalized.value)) : String(normalized.value) === String(value)) optionNode.selected = true;
    control.appendChild(optionNode);
  });
  if (attributes.multiple && Array.isArray(value)) control.value = value.map(String);
  else control.value = value === null || value === undefined ? '' : String(value);
  return control;
}

export function pageHeading(title, subtitle, actions = []) {
  const copy = node('div', { class: 'page-heading-copy' }, [node('h1', { class: 'page-title', text: title }), subtitle ? node('p', { class: 'page-subtitle', text: subtitle }) : null]);
  const actionRow = node('div', { class: 'page-actions' }, actions);
  return node('div', { class: 'page-heading' }, [copy, actions.length ? actionRow : null]);
}

export function panel(title, body, action = null, options = {}) {
  const header = [node('h2', { class: 'panel-title', text: title })];
  if (action) header.push(action);
  return node('section', { class: `panel${options.className ? ` ${options.className}` : ''}` }, [node('div', { class: 'panel-header' }, header), node('div', { class: `panel-body${options.flush ? ' flush' : ''}` }, body)]);
}

export function statGrid(items) {
  const grid = node('div', { class: 'stats-grid' });
  items.forEach((item) => {
    const value = node('div', { class: 'stat-value', text: item.value === undefined || item.value === null || item.value === '' ? '—' : String(item.value) });
    grid.appendChild(node('div', { class: 'stat-card', dataset: { statKey: item.key || '' } }, [node('div', { class: 'stat-label', text: item.label }), value]));
  });
  return grid;
}

export function updateStat(grid, key, value) {
  if (!grid) return;
  const card = [...grid.querySelectorAll('[data-stat-key]')].find((item) => item.dataset.statKey === String(key));
  if (!card) return;
  const valueNode = card.querySelector('.stat-value');
  if (valueNode) valueNode.textContent = value === undefined || value === null || value === '' ? '—' : String(value);
}

export function loading(message = 'Loading data…') {
  return node('div', { class: 'loading-state', role: 'status' }, [node('span', { class: 'loading-dots' }, [node('span'), node('span'), node('span')]), node('span', { text: message })]);
}

export function errorState(message, retry = null) {
  const children = [node('strong', { text: 'Unable to load this section' }), node('span', { text: message || 'The API request failed.' })];
  if (retry) children.push(button('Try again', 'app-button app-button-quiet', retry));
  return node('div', { class: 'error-state', role: 'alert' }, children);
}

export function emptyState(message = 'No records were returned by the API.') {
  return node('div', { class: 'empty-state' }, [node('strong', { text: 'Nothing to show' }), node('span', { text: message })]);
}

export function humanizeStatus(value) {
  const normalized = String(value || '').toUpperCase().replace(/[_-]+/g, ' ').trim();
  if (!normalized) return 'Unknown';
  return normalized.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function statusBadge(status) {
  const normalized = String(status || '').toUpperCase().replace(/[^A-Z]+/g, '-');
  const className = ['APPROVED', 'PAID', 'CONFIRMED', 'ACTIVE', 'ENABLED', 'TRUE'].includes(normalized) ? 'status-approved' : ['REJECTED', 'FLAGGED', 'INACTIVE', 'DISABLED', 'FALSE'].includes(normalized) ? 'status-rejected' : 'status-pending';
  return node('span', { class: `status-badge ${className}`, text: humanizeStatus(normalized === 'TRUE' ? 'ACTIVE' : normalized === 'FALSE' ? 'INACTIVE' : normalized) });
}

export function rosterBadge(value) {
  const confirmed = value === true || ['YES', 'TRUE', 'CONFIRMED', '1'].includes(String(value).toUpperCase());
  return node('span', { class: `roster-badge ${confirmed ? 'roster-yes' : 'roster-no'}`, text: confirmed ? 'Yes' : 'No' });
}

export function methodBadge(method) {
  const normalized = String(method || '').toUpperCase().replace(/_/g, '-');
  const className = normalized === 'PAYSTACK' ? 'method-paystack' : normalized === 'CASH' ? 'method-cash' : normalized === 'BANK-TRANSFER' ? 'method-bank' : 'status-rejected';
  return node('span', { class: `method-badge ${className}`, text: humanizeStatus(normalized) });
}

export function table(columns, rows, rowRenderer, emptyMessage) {
  const wrap = node('div', { class: 'table-wrap' });
  if (!rows.length) {
    wrap.appendChild(emptyState(emptyMessage));
    return wrap;
  }
  const tableNode = node('table', { class: 'data-table' });
  const head = node('thead');
  const headRow = node('tr');
  columns.forEach((column) => headRow.appendChild(node('th', { scope: 'col', text: typeof column === 'string' ? column : column.label })));
  head.appendChild(headRow);
  tableNode.appendChild(head);
  const body = node('tbody');
  rows.forEach((row, index) => {
    const tr = node('tr');
    rowRenderer(row, tr, index);
    body.appendChild(tr);
  });
  tableNode.appendChild(body);
  wrap.appendChild(tableNode);
  return wrap;
}

export function previewList(rows, renderer, emptyMessage = 'No records were returned by the API.') {
  if (!rows.length) return emptyState(emptyMessage);
  const list = node('div', { class: 'preview-list' });
  rows.forEach((row) => list.appendChild(renderer(row)));
  return list;
}

export function setBusy(element, busy, busyText = 'Working…') {
  if (!element) return;
  if (busy) {
    element.disabled = true;
    element.setAttribute('aria-busy', 'true');
    if (!element.querySelector('.button-busy-label')) element.appendChild(node('span', { class: 'button-busy-label', text: busyText }));
    return;
  }
  element.disabled = false;
  element.removeAttribute('aria-busy');
  element.querySelector('.button-busy-label')?.remove();
}

export function formStatus(message, type = 'error') {
  return node('div', { class: `form-status ${type}`, role: type === 'error' ? 'alert' : 'status', text: message });
}

export function openModal(content, options = {}) {
  closeModal();
  const layer = node('div', { class: 'modal-layer', role: 'presentation' });
  const box = node('div', { class: `modal-box${options.wide ? ' modal-box-wide' : ''}`, role: 'dialog', 'aria-modal': 'true' });
  const close = button(icon('close', 18), 'modal-close', closeModal, { 'aria-label': 'Close dialog' });
  const contentNode = node('div', { class: 'modal-content' });
  if (content && typeof content === 'object' && typeof content.nodeType === 'number') contentNode.appendChild(content);
  else if (typeof content === 'function') contentNode.appendChild(content());
  else if (content !== undefined && content !== null) appendChildren(contentNode, content);
  box.append(close, contentNode);
  layer.appendChild(box);
  layer.addEventListener('click', (event) => {
    if (event.target === layer) closeModal();
  });
  document.body.appendChild(layer);
  document.body.dataset.modalOpen = 'true';
  const focusable = box.querySelector('input, select, textarea, button:not(.modal-close)');
  focusable?.focus();
  return { layer, box, content: contentNode, close: closeModal };
}

export function closeModal() {
  document.querySelectorAll('.modal-layer').forEach((layer) => layer.remove());
  delete document.body.dataset.modalOpen;
}

export function confirmDialog(title, message, confirmLabel = 'Confirm') {
  return new Promise((resolve) => {
    let settled = false;
    const finish = (value) => {
      if (settled) return;
      settled = true;
      document.removeEventListener('keydown', onKey);
      closeModal();
      resolve(value);
    };
    const onKey = (event) => {
      if (event.key === 'Escape') finish(false);
    };
    const content = node('div', {}, [node('h2', { class: 'modal-title', text: title }), node('p', { class: 'confirm-copy', text: message }), node('div', { class: 'form-actions' }, [button('Cancel', 'app-button app-button-light', () => finish(false)), button(confirmLabel, 'app-button app-button-primary', () => finish(true))])]);
    openModal(content);
    document.addEventListener('keydown', onKey);
  });
}

let toastRegion;

export function toast(message, type = 'success') {
  if (!toastRegion) {
    toastRegion = node('div', { class: 'toast-region', 'aria-live': 'polite' });
    document.body.appendChild(toastRegion);
  }
  const item = node('div', { class: `toast${type === 'error' ? ' error' : ''}`, role: 'status', text: message });
  toastRegion.appendChild(item);
  window.setTimeout(() => item.remove(), 4500);
}

export function initials(value) {
  const parts = String(value || '').trim().split(/\s+/).filter(Boolean);
  return parts.slice(0, 2).map((part) => part[0].toUpperCase()).join('') || '—';
}

export function formatDate(value, options = {}) {
  if (value === null || value === undefined || value === '') return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat('en-NG', { year: 'numeric', month: 'short', day: 'numeric', ...options }).format(date);
}

export function formatMoney(value) {
  const number = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(number)) return '—';
  return new Intl.NumberFormat('en-NG', { style: 'currency', currency: 'NGN', maximumFractionDigits: 2 }).format(number);
}

export function formatTerm(value) {
  const normalized = String(value || '').toUpperCase().replace(/[_-]+/g, ' ');
  return normalized ? normalized.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) : '—';
}

export function safeUrl(value) {
  if (!value) return '';
  try {
    const url = new URL(String(value), window.location.href);
    if (!['http:', 'https:', 'blob:'].includes(url.protocol)) return '';
    return url.href;
  } catch {
    return '';
  }
}

export function actionLink(label, onClick, className = 'table-action') {
  return button(label, className, onClick, { type: 'button' });
}

export function setPageError(container, message, retry) {
  clear(container).appendChild(errorState(message, retry));
}

export function setPageLoading(container, message) {
  clear(container).appendChild(loading(message));
}

export function setPageEmpty(container, message) {
  clear(container).appendChild(emptyState(message));
}

export function responseError(error) {
  return error instanceof Error ? error.message : 'The API request failed.';
}

export function makeLink(label, url, className = 'table-action') {
  const safe = safeUrl(url);
  if (!safe) return node('span', { class: `${className} disabled`, text: label, 'aria-disabled': 'true' });
  return node('a', { class: className, href: safe, target: '_blank', rel: 'noopener noreferrer', text: label });
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = node('a', { href: url, download: String(filename || 'download') });
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function filenameFromResponse(response, fallback) {
  const disposition = response.headers.get('content-disposition') || '';
  const match = disposition.match(/filename="?([^";]+)"?/i);
  return match ? match[1] : fallback;
}

export function sortRecent(items, fields = ['submittedAt', 'postedAt', 'uploadedAt', 'createdAt', 'paidAt']) {
  return [...(items || [])].sort((left, right) => {
    const leftValue = fields.map((field) => new Date(getFieldValue(left, field)).getTime()).find((value) => Number.isFinite(value));
    const rightValue = fields.map((field) => new Date(getFieldValue(right, field)).getTime()).find((value) => Number.isFinite(value));
    return (rightValue || 0) - (leftValue || 0);
  });
}

function getFieldValue(value, key) {
  if (!value || typeof value !== 'object') return '';
  return value[key] ?? value[key.toLowerCase()] ?? '';
}
