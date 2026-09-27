import { ENDPOINTS } from './config.js';

export { ENDPOINTS };

export const API_BASE = '/api/v1';
export const TOKEN_KEY = 'crestwood.accessToken';
export const SESSION_KEY = 'crestwood.session';

export class ApiError extends Error {
  constructor(message, status = 0, payload = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

function storageGet(key) {
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function storageSet(key, value) {
  try {
    window.localStorage.setItem(key, value);
    return true;
  } catch {
    return false;
  }
}

function storageRemove(key) {
  try {
    window.localStorage.removeItem(key);
    return true;
  } catch {
    return false;
  }
}

export function getAccessToken() {
  return storageGet(TOKEN_KEY);
}

export function loadSession() {
  const raw = storageGet(SESSION_KEY);
  if (!raw) return null;
  try {
    const session = JSON.parse(raw);
    if (!session || typeof session !== 'object' || !session.user || !getAccessToken()) return null;
    return session;
  } catch {
    storageRemove(SESSION_KEY);
    return null;
  }
}

export function saveSession(token, user) {
  if (!token || !user || typeof user !== 'object') return false;
  storageSet(TOKEN_KEY, String(token));
  storageSet(SESSION_KEY, JSON.stringify({ user, savedAt: Date.now() }));
  return true;
}

export function clearSession() {
  storageRemove(TOKEN_KEY);
  storageRemove(SESSION_KEY);
}

export function normalizeRole(value) {
  const role = String(value || '').trim().toUpperCase().replace(/[ -]+/g, '_');
  if (role === 'ADMIN' || role === 'SUPERADMIN' || role === 'SUPER_ADMIN') return 'SUPER_ADMIN';
  if (['TEACHER', 'STUDENT', 'PARENT', 'ACCOUNTANT'].includes(role)) return role;
  return '';
}

export function roleFromUser(user) {
  return normalizeRole(getField(user, ['role', 'userRole', 'accountRole']));
}

export function getField(value, keys, fallback = '') {
  if (!value || typeof value !== 'object') return fallback;
  for (const key of keys) {
    if (Object.prototype.hasOwnProperty.call(value, key) && value[key] !== null && value[key] !== undefined) return value[key];
  }
  return fallback;
}

export function nestedField(value, paths, fallback = '') {
  if (!value || typeof value !== 'object') return fallback;
  for (const path of paths) {
    const parts = path.split('.');
    let current = value;
    let found = true;
    for (const part of parts) {
      if (!current || typeof current !== 'object' || !Object.prototype.hasOwnProperty.call(current, part)) {
        found = false;
        break;
      }
      current = current[part];
    }
    if (found && current !== null && current !== undefined) return current;
  }
  return fallback;
}

export function buildPath(path, query) {
  const base = String(path || '');
  if (!query || typeof query !== 'object') return base;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return;
    if (Array.isArray(value)) {
      value.forEach((entry) => {
        if (entry !== null && entry !== undefined && entry !== '') params.append(key, String(entry));
      });
      return;
    }
    params.set(key, String(value));
  });
  const queryString = params.toString();
  if (!queryString) return base;
  return base.includes('?') ? `${base}&${queryString}` : `${base}?${queryString}`;
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') || '';
  if (response.status === 204) return null;
  if (contentType.includes('application/json') || contentType.includes('+json')) {
    try {
      return await response.json();
    } catch {
      throw new ApiError('The server returned invalid JSON.', response.status);
    }
  }
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

function errorMessage(payload, status) {
  if (payload && typeof payload === 'object') {
    const value = getField(payload, ['error', 'message', 'detail', 'title']);
    if (typeof value === 'string' && value.trim()) return value;
  }
  if (status === 401) return 'Your session is not authorized. Please sign in again.';
  if (status === 403) return 'You do not have permission to perform this action.';
  if (status === 404) return 'The requested API resource was not found.';
  if (status >= 500) return 'The server could not complete the request.';
  return 'The request could not be completed.';
}

export async function request(path, options = {}) {
  const method = options.method || 'GET';
  const headers = new Headers(options.headers || {});
  headers.set('Accept', 'application/json');
  const token = getAccessToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  let body = options.body;
  if (body !== undefined && body !== null && !(body instanceof FormData) && typeof body !== 'string' && !(body instanceof Blob)) {
    headers.set('Content-Type', 'application/json');
    body = JSON.stringify(body);
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${buildPath(path, options.query)}`, {
      method,
      headers,
      body,
      signal: options.signal
    });
  } catch (error) {
    if (error && error.name === 'AbortError') throw error;
    throw new ApiError('The API could not be reached.');
  }
  const payload = await parseResponse(response);
  if (!response.ok) {
    const apiError = new ApiError(errorMessage(payload, response.status), response.status, payload);
    if (response.status === 401 && !options.suppressAuthRedirect && typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('crestwood:unauthorized'));
    }
    throw apiError;
  }
  return payload;
}

export async function requestFirst(paths, options = {}) {
  const candidates = (Array.isArray(paths) ? paths : [paths]).filter(Boolean);
  let lastError = null;
  for (const path of candidates) {
    try {
      return await request(path, options);
    } catch (error) {
      lastError = error;
      if (!(error instanceof ApiError) || ![404, 405].includes(error.status)) throw error;
    }
  }
  throw lastError || new ApiError('The API resource could not be reached.');
}

export function listFrom(payload, depth = 0) {
  if (Array.isArray(payload)) return payload;
  if (!payload || typeof payload !== 'object' || depth > 4) return [];
  const directKeys = ['items', 'content', 'results', 'rows', 'records', 'data'];
  for (const key of directKeys) {
    const value = payload[key];
    if (Array.isArray(value)) return value;
    if (value && typeof value === 'object') {
      const nested = listFrom(value, depth + 1);
      if (nested.length || Object.keys(value).length === 0) return nested;
    }
  }
  return [];
}

export function countFrom(payload) {
  if (typeof payload === 'number' && Number.isFinite(payload)) return payload;
  if (!payload || typeof payload !== 'object') return listFrom(payload).length;
  const keys = ['totalElements', 'totalCount', 'total', 'count', 'numberOfElements'];
  for (const key of keys) {
    const value = payload[key];
    if (typeof value === 'number' && Number.isFinite(value)) return value;
  }
  if (payload.page && typeof payload.page === 'object') {
    const pageCount = countFrom(payload.page);
    if (pageCount !== undefined) return pageCount;
  }
  if (payload.data && typeof payload.data === 'object') {
    const dataCount = countFrom(payload.data);
    if (dataCount !== undefined) return dataCount;
  }
  return listFrom(payload).length;
}

export function metricFrom(payload, key) {
  return nestedField(payload, [key, `data.${key}`, `summary.${key}`, `stats.${key}`, `data.summary.${key}`]);
}

export async function listRequest(path, query) {
  const payload = await request(path, { query });
  return { payload, items: listFrom(payload), total: countFrom(payload) };
}

export async function listRequestFirst(paths, query) {
  const payload = await requestFirst(paths, { query });
  return { payload, items: listFrom(payload), total: countFrom(payload) };
}

export function idOf(value) {
  return getField(value, ['id', 'entityId', 'uuid'], '');
}

export function statusOf(value) {
  return String(getField(value, ['status', 'state', 'paymentStatus', 'rosterStatus'], '')).toUpperCase();
}

export function numeric(value, fallback = 0) {
  const number = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(number) ? number : fallback;
}

export function sumBy(items, fields) {
  const keys = Array.isArray(fields) ? fields : [fields];
  return (Array.isArray(items) ? items : []).reduce((total, item) => {
    for (const key of keys) {
      const value = getField(item, [key], undefined);
      if (value !== undefined && value !== null && value !== '') return total + numeric(value);
    }
    return total;
  }, 0);
}

export function textFromPayload(payload, keys, fallback = '') {
  if (!payload || typeof payload !== 'object') return fallback;
  const value = nestedField(payload, [...keys, ...keys.map((key) => `data.${key}`)], fallback);
  return value === null || value === undefined ? fallback : String(value);
}

export async function downloadRequest(path, options = {}) {
  const value = String(path || '');
  const url = value.startsWith('/api/') || value === API_BASE || value.startsWith(`${API_BASE}/`) ? value : `${API_BASE}${value.startsWith('/') ? value : `/${value}`}`;
  const headers = new Headers(options.headers || {});
  const token = getAccessToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  let response;
  try {
    response = await fetch(url, { method: options.method || 'GET', headers, body: options.body, signal: options.signal });
  } catch (error) {
    if (error && error.name === 'AbortError') throw error;
    throw new ApiError('The API could not be reached.');
  }
  if (!response.ok) {
    const payload = await parseResponse(response);
    throw new ApiError(errorMessage(payload, response.status), response.status, payload);
  }
  return response;
}

const scriptPromises = new Map();

export function loadScript(src, globalName) {
  if (window[globalName]) return Promise.resolve(window[globalName]);
  if (scriptPromises.has(src)) return scriptPromises.get(src);
  const promise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    script.referrerPolicy = 'strict-origin-when-cross-origin';
    script.onload = () => {
      if (window[globalName]) resolve(window[globalName]);
      else reject(new Error(`${globalName} did not load.`));
    };
    script.onerror = () => reject(new Error(`${globalName} could not be loaded.`));
    document.head.appendChild(script);
  });
  scriptPromises.set(src, promise);
  return promise;
}

export function loadPaystack() {
  return loadScript('https://js.paystack.co/v1/inline.js', 'PaystackPop');
}

export function loadJsPdf() {
  return loadScript('https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js', 'jspdf');
}

export async function paystackConfig() {
  return request(ENDPOINTS.paystackConfig);
}

export function extractToken(payload) {
  return textFromPayload(payload, ['accessToken', 'access_token', 'token', 'jwt'], '');
}

export function extractUser(payload) {
  if (!payload || typeof payload !== 'object') return null;
  const user = payload.user || payload.account || payload.profile || payload.data?.user;
  if (user && typeof user === 'object') return user;
  if (payload.email || payload.role || payload.userRole) return payload;
  return null;
}

export function extractReference(payload) {
  return textFromPayload(payload, ['reference', 'paystackReference', 'transactionReference'], '');
}

export function isVerifiedPayment(payload) {
  if (!payload || typeof payload !== 'object') return false;
  const status = statusOf(payload);
  return status === 'PAID' || getField(payload, ['verified', 'paymentVerified', 'success'], false) === true;
}

export function responseMessage(payload, fallback = '') {
  if (!payload || typeof payload !== 'object') return fallback;
  const value = getField(payload, ['message', 'error', 'detail'], '');
  return typeof value === 'string' && value.trim() ? value : fallback;
}
