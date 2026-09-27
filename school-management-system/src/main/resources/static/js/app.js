import {
  ENDPOINTS,
  clearSession,
  extractToken,
  extractUser,
  getField,
  loadSession,
  normalizeRole,
  request,
  responseMessage,
  roleFromUser,
  saveSession
} from './api.js';
import { CLASS_OPTIONS, LOGIN_ROLES, ROLE_CONFIG } from './config.js';
import {
  appendChildren,
  button,
  clear,
  confirmDialog,
  errorState,
  field,
  formStatus,
  icon,
  input,
  loading,
  node,
  openModal,
  pageHeading,
  panel,
  responseError,
  select,
  setBusy,
  statGrid,
  toast
} from './ui.js';
import { ADMIN_PAGES } from './admin-pages.js';
import { TEACHER_PAGES } from './teacher-pages.js';
import { STUDENT_PAGES } from './student-pages.js';
import { PARENT_PAGES } from './parent-pages.js';
import { ACCOUNTANT_PAGES } from './accountant-pages.js';

const appRoot = document.getElementById('appRoot');
const pageRegistry = {
  SUPER_ADMIN: ADMIN_PAGES,
  TEACHER: TEACHER_PAGES,
  STUDENT: STUDENT_PAGES,
  PARENT: PARENT_PAGES,
  ACCOUNTANT: ACCOUNTANT_PAGES
};

let session = loadSession();
let landingBound = false;
let shell = null;
let routeToken = 0;

const landingStories = [
  {
    id: 'waec-results-2026',
    date: 'OCTOBER 14, 2026',
    title: 'Crestwood Students Excel in 2026 WAEC Results',
    category: 'Academics',
    author: 'Crestwood Academic Board',
    body: 'Crestwood Academy has once again recorded exceptional performance in the West African Senior School Certificate Examination for the 2025/2026 academic session. The Academy celebrates outstanding results, with students securing distinctions across Mathematics, English Language, Sciences, and Humanities.'
  },
  {
    id: 'science-competition-2026',
    date: 'SEPTEMBER 28, 2026',
    title: 'Crestwood Academic Team Places 3rd in State Science Competition',
    category: 'Academics',
    author: 'Science Department',
    body: 'Crestwood Academy students earned state-level honors through analytical work in physics, chemistry, and biology, supported by the Academy’s Science Department and laboratory programme.'
  },
  {
    id: 'computer-lab-2026',
    date: 'SEPTEMBER 05, 2026',
    title: 'New Computer Laboratory Officially Opens at Crestwood Academy',
    category: 'Campus Facilities',
    author: 'Campus Facilities',
    body: 'The new Science and Computer Laboratory suite provides high-speed workstations, fiber internet, coding resources, and digital learning spaces for every class.'
  }
];

const pathwayContent = [
  { category: 'Academic Excellence', title: 'WAEC & JAMB Preparation', body: 'A curriculum aligned with the Nigerian secondary school syllabus, preparing students for WAEC, NECO, and JAMB with clear, trackable results.', action: 'VIEW CURRICULUM' },
  { category: 'Sports & Extracurriculars', title: 'Inter-House Sports & Clubs', body: 'Balanced development through inter-house sports, clubs, and co-curricular activities that build character alongside academics.', action: 'EXPLORE ACTIVITIES' },
  { category: 'Digital School Portal', title: 'Student, Parent & Teacher Access', body: 'A secure online portal where students check results, parents track progress and pay school fees, and teachers manage grading — all in one place.', action: 'ACCESS PORTAL' }
];

const testimonialContent = [
  { badge: 'STUDENT', quote: 'Crestwood Academy gave me a strong academic foundation and the confidence to gain admission into my dream university while graduating at the top of my class.', author: 'Chidinma Okafor', role: 'Class of 2024 / Admitted to University of Lagos' },
  { badge: 'PARENT PERSPECTIVE', quote: 'We chose Crestwood Academy for the quality of teaching, but we stay because of the caring community and how easy it is to track our child’s progress online. Our daughter has grown immensely in both confidence and academic performance.', author: 'The Okonkwo Family', role: 'Parents of SS2 Student' }
];

function navigate(path) {
  const target = path.startsWith('#') ? path : `#/${String(path).replace(/^\/+/, '')}`;
  if (window.location.hash === target) renderRoute();
  else window.location.hash = target;
}

function parseRoute() {
  const raw = window.location.hash.replace(/^#/, '');
  const withoutSlash = raw.startsWith('/') ? raw.slice(1) : raw;
  const [pathPart, queryPart = ''] = withoutSlash.split('?');
  const segments = pathPart.split('/').filter(Boolean).map((segment) => decodeURIComponent(segment));
  return { segments, query: new URLSearchParams(queryPart) };
}

function setApplicationMode(enabled) {
  document.body.classList.toggle('application-mode', enabled);
  if (enabled) window.scrollTo(0, 0);
}

function routeIsPublic(segments) {
  return ['enroll', 'login', 'forgot-password', 'reset-password'].includes(segments[0]);
}

function roleFromRoute(segment) {
  const role = Object.values(ROLE_CONFIG).find((item) => item.route === segment);
  return role ? role.key : '';
}

function roleFromSession() {
  return roleFromUser(session && session.user);
}

function routeTitle(config, pageId) {
  const page = config.nav.find((item) => item.id === pageId);
  return page ? page.label : config.label;
}

function setShellSubtitle(value) {
  if (shell && value) shell.subtitle.textContent = String(value);
}

function makeBrand(inverted = false) {
  const brand = node(inverted ? 'div' : 'a', { class: `brand-logo${inverted ? ' inverted' : ''}`, href: inverted ? undefined : '#/' }, [
    node('div', { class: 'brand-icon', text: 'C' }),
    node('div', { class: 'brand-name' }, [node('span', { class: 'crestwood', text: 'CRESTWOOD' }), node('span', { class: 'academy', text: 'Academy' })])
  ]);
  if (!inverted) brand.addEventListener('click', (event) => { event.preventDefault(); navigate('/'); });
  return brand;
}

function makeUserLabel(user) {
  const name = getField(user, ['fullName', 'full_name', 'name', 'displayName', 'email'], 'Portal user');
  return String(name);
}

function makeInitials(user) {
  const name = makeUserLabel(user);
  const parts = name.split(/\s+/).filter(Boolean);
  return parts.slice(0, 2).map((part) => part[0].toUpperCase()).join('') || '—';
}

function buildShell(config, pageId, query) {
  const sidebar = node('aside', { class: 'app-sidebar', 'aria-label': `${config.label} navigation` });
  const main = node('div', { class: 'app-main' });
  const backdrop = node('div', { class: 'drawer-backdrop', 'aria-hidden': 'true' });
  const content = node('main', { class: 'app-content' });
  const title = routeTitle(config, pageId);
  const subtitle = node('div', { class: 'topbar-subtitle', text: 'Authenticated portal workspace' });
  const pageMount = node('div');
  const closeDrawer = () => {
    sidebar.classList.remove('mobile-open');
    backdrop.classList.remove('open');
    document.body.style.overflow = '';
  };
  const openDrawer = () => {
    sidebar.classList.add('mobile-open');
    backdrop.classList.add('open');
    document.body.style.overflow = 'hidden';
  };
  const context = {
    user: session.user,
    role: config.key,
    navigate: (page) => {
      closeDrawer();
      navigate(`${config.route}/${page}`);
    },
    setSubtitle: setShellSubtitle,
    refresh: () => renderRoute()
  };
  const topbar = node('header', { class: 'app-topbar' }, [
    node('div', { class: 'topbar-heading' }, [node('div', { class: 'topbar-title', text: title }), subtitle]),
    node('div', { class: 'topbar-actions' }, [
      button(icon('menu', 20), 'mobile-menu-button', openDrawer, { 'aria-label': 'Open navigation' }),
      node('span', { class: 'topbar-role', text: config.label.toUpperCase() }),
      node('div', { class: 'user-avatar', text: makeInitials(session.user), 'aria-label': makeUserLabel(session.user) })
    ])
  ]);
  main.append(topbar, content);
  content.appendChild(pageMount);
  const sidebarBrand = node('div', { class: 'sidebar-brand' }, [makeBrand(), node('div', { class: 'sidebar-brand-copy' }, [node('strong', { text: 'CRESTWOOD' }), node('span', { text: 'Academy' })])]);
  const roleTag = node('div', { class: 'sidebar-role', text: config.label.toUpperCase() });
  const nav = node('nav', { class: 'sidebar-nav', 'aria-label': 'Dashboard pages' });
  config.nav.forEach((page) => {
    const active = page.id === pageId;
    const item = button('', `sidebar-nav-item${active ? ' active' : ''}`, () => context.navigate(page.id), { 'aria-current': active ? 'page' : undefined });
    item.append(icon(page.icon, 19), node('span', { class: 'sidebar-nav-label', text: page.label }));
    nav.appendChild(item);
  });
  const collapseButton = button('', 'sidebar-collapse', () => {
    sidebar.classList.toggle('collapsed');
    try { window.localStorage.setItem('crestwood.sidebarCollapsed', sidebar.classList.contains('collapsed') ? '1' : '0'); } catch { }
  }, { 'aria-label': 'Toggle sidebar' });
  collapseButton.append(icon('chevron', 18), node('span', { class: 'sidebar-nav-label', text: 'Collapse sidebar' }));
  const logoutButton = button('', 'sidebar-nav-item', async () => {
    const confirmed = await confirmDialog('Log out', 'Your local session will be cleared. The API logout request will also be attempted.', 'Log out');
    if (!confirmed) return;
    let logoutError = null;
    try { await request(`${ENDPOINTS.auth}/logout`, { method: 'POST' }); } catch (error) { logoutError = error; }
    clearSession();
    session = null;
    shell = null;
    if (logoutError) toast(`The local session was cleared, but the API logout failed: ${responseError(logoutError)}`, 'error');
    navigate('/login');
  });
  logoutButton.append(icon('logout', 19), node('span', { class: 'sidebar-nav-label', text: 'Log out' }));
  const bottom = node('div', { class: 'sidebar-bottom' }, [collapseButton, logoutButton]);
  sidebar.append(sidebarBrand, roleTag, nav, bottom);
  try {
    if (window.localStorage.getItem('crestwood.sidebarCollapsed') === '1') sidebar.classList.add('collapsed');
  } catch { }
  backdrop.addEventListener('click', closeDrawer);
  const layout = node('div', { class: 'app-shell' }, [sidebar, backdrop, main]);
  return { layout, sidebar, backdrop, content, pageMount, context, subtitle, closeDrawer };
}

function renderAccessDenied(role) {
  const root = node('div', { class: 'app-shell' });
  const content = node('main', { class: 'app-content' });
  content.appendChild(pageHeading('Access denied', 'Your authenticated role cannot open this page.'));
  content.appendChild(panel('Access denied', errorState('The API session does not have access to this role.'), button('Return to login', 'app-button app-button-primary', () => { clearSession(); session = null; navigate('/login'); })));
  root.appendChild(content);
  return root;
}

function renderAppRoute(segments, query) {
  const requestedRole = roleFromRoute(segments[1]);
  const actualRole = roleFromSession();
  if (!requestedRole || !actualRole || requestedRole !== actualRole) {
    setApplicationMode(true);
    clear(appRoot).appendChild(renderAccessDenied(requestedRole || actualRole));
    return;
  }
  const config = ROLE_CONFIG[actualRole];
  const pageId = segments[2] || 'overview';
  if (!config.nav.some((page) => page.id === pageId)) {
    navigate(`${config.route}/overview`);
    return;
  }
  shell = buildShell(config, pageId, query);
  setApplicationMode(true);
  clear(appRoot).appendChild(shell.layout);
  const renderer = pageRegistry[actualRole][pageId];
  const currentToken = ++routeToken;
  try {
    const pageNode = renderer(shell.context);
    if (currentToken === routeToken) shell.pageMount.replaceChildren(pageNode);
  } catch (error) {
    shell.pageMount.replaceChildren(errorState(responseError(error)));
  }
  request(ENDPOINTS.academicContext).then((payload) => {
    if (currentToken !== routeToken) return;
    const contextText = getField(payload, ['label', 'displayName', 'name', 'sessionAndTerm', 'subtitle'], '');
    if (contextText) setShellSubtitle(contextText);
  }).catch(() => {
    if (currentToken === routeToken) setShellSubtitle('Academic context unavailable from the API');
  });
}

function renderPublicRoute(type, query) {
  setApplicationMode(true);
  clear(appRoot);
  if (type === 'enroll') renderEnrollment();
  else if (type === 'login') renderLogin(query);
  else if (type === 'forgot-password') renderForgotPassword();
  else renderResetPassword(query);
}

function authShell(title, content, wide = false) {
  const page = node('div', { class: 'auth-page' });
  const card = node('section', { class: `auth-card${wide ? ' auth-card-wide' : ''}` });
  card.appendChild(makeBrand());
  card.appendChild(node('span', { class: 'auth-eyebrow', text: 'Crestwood Academy Portal' }));
  card.appendChild(node('h1', { class: 'auth-title', text: title }));
  card.appendChild(content);
  page.appendChild(card);
  return page;
}

function footerLink(label, target) {
  return node('a', { class: 'auth-footer-link', href: `#/${target}`, text: label });
}

function renderEnrollment() {
  const status = node('div');
  const form = node('form', { class: 'auth-form' });
  const parentName = input('parentFullName', 'text', { required: true });
  const email = input('parentEmail', 'email', { required: true });
  const phone = input('parentPhone', 'tel', { required: true });
  const childName = input('studentFullName', 'text', { required: true });
  const childDob = input('studentDateOfBirth', 'date', { required: true });
  const applyingClass = select('applyingForClass', CLASS_OPTIONS, CLASS_OPTIONS[0], { required: true });
  form.append(
    node('div', { class: 'auth-note', text: 'Applications are reviewed by an administrator before an account is created. Your submission will enter the school enrollment queue for review.' }),
    node('div', { class: 'form-grid' }, [field('Parent / Guardian full name', parentName), node('div', { class: 'form-grid' }, [field('Email', email), field('Phone', phone)]), field('Child’s full name', childName), node('div', { class: 'form-grid' }, [field('Date of birth', childDob), field('Applying for class', applyingClass)])]),
    status
  );
  const submit = button('Submit enrollment application', 'app-button app-button-primary', async () => {
    if (!form.reportValidity()) return;
    setBusy(submit, true, 'Submitting…');
    clear(status);
    try {
      const payload = await request(ENDPOINTS.enrollment, { method: 'POST', body: { parentFullName: parentName.value, parentEmail: email.value, parentPhone: phone.value, studentFullName: childName.value, studentDateOfBirth: childDob.value, applyingForClass: applyingClass.value } });
      status.appendChild(formStatus(responseMessage(payload, 'Your enrollment application was submitted.'), 'success'));
    } catch (error) {
      status.appendChild(formStatus(responseError(error), 'error'));
    } finally {
      setBusy(submit, false);
    }
  }, { type: 'submit' });
  form.addEventListener('submit', (event) => { event.preventDefault(); submit.click(); });
  form.appendChild(node('div', { class: 'form-actions' }, [submit]));
  const content = node('div', {}, [form, node('div', { class: 'auth-footer' }, ['Already have an account? ', footerLink('Log in', 'login')])]);
  clear(appRoot).appendChild(authShell('Apply for admission', content, true));
  parentName.focus();
}

function renderLogin(query) {
  const requested = String(query.get('role') || '').toUpperCase();
  let selectedRole = LOGIN_ROLES.find((item) => item.key === requested || item.route === requested.toLowerCase()) || LOGIN_ROLES[0];
  const status = node('div');
  const tabs = node('div', { class: 'role-tabs', role: 'tablist', 'aria-label': 'Choose portal role' });
  const form = node('form', { class: 'auth-form' });
  const identifier = input('email', 'email', { required: true, autocomplete: 'username' });
  const password = input('password', 'password', { required: true, autocomplete: 'current-password' });
  form.append(node('div', { class: 'form-grid' }, [field('Email address', identifier), field('Password', password)]), status);
  const submit = button('Sign in', 'app-button app-button-primary', async () => {
    if (!form.reportValidity()) return;
    setBusy(submit, true, 'Signing in…');
    clear(status);
    try {
      const payload = await request(`${ENDPOINTS.auth}/login`, { method: 'POST', suppressAuthRedirect: true, body: { email: identifier.value, password: password.value, role: selectedRole.key } });
      const token = extractToken(payload);
      const user = extractUser(payload);
      if (!token || !user) throw new Error('The login API did not return an access token and user.');
      const returnedRole = roleFromUser(user) || normalizeRole(getField(payload, ['role', 'userRole'], ''));
      if (returnedRole && returnedRole !== selectedRole.key) throw new Error('The API returned a different role than the selected portal role.');
      if (!returnedRole) user.role = selectedRole.key;
      saveSession(token, user);
      session = { user };
      navigate(`/app/${ROLE_CONFIG[selectedRole.key].route}/overview`);
    } catch (error) {
      status.appendChild(formStatus(responseError(error), 'error'));
    } finally {
      setBusy(submit, false);
    }
  }, { type: 'submit' });
  form.addEventListener('submit', (event) => { event.preventDefault(); submit.click(); });
  form.appendChild(node('div', { class: 'form-actions' }, [submit]));
  const renderTabs = () => {
    clear(tabs);
    LOGIN_ROLES.forEach((role) => {
      const tab = button(role.label, `role-tab${role.key === selectedRole.key ? ' active' : ''}`, () => {
        selectedRole = role;
        renderTabs();
      }, { role: 'tab', 'aria-selected': role.key === selectedRole.key ? 'true' : 'false' });
      tabs.appendChild(tab);
    });
  };
  renderTabs();
  const content = node('div', {}, [node('p', { class: 'auth-copy', text: 'Sign in to your assigned Crestwood Academy workspace.' }), tabs, form, node('div', { class: 'auth-footer' }, [footerLink('Forgot password?', 'forgot-password'), ' · ', footerLink('Apply now', 'enroll')])]);
  clear(appRoot).appendChild(authShell('Portal login', content));
  identifier.focus();
}

function renderForgotPassword() {
  const status = node('div');
  const form = node('form', { class: 'auth-form' });
  const email = input('email', 'email', { required: true, autocomplete: 'email' });
  form.append(node('div', { class: 'form-grid' }, [field('Email', email)]), status);
  const submit = button('Send reset link', 'app-button app-button-primary', async () => {
    if (!form.reportValidity()) return;
    setBusy(submit, true, 'Sending…');
    clear(status);
    try {
      const payload = await request(`${ENDPOINTS.auth}/forgot-password`, { method: 'POST', suppressAuthRedirect: true, body: { email: email.value } });
      status.appendChild(formStatus(responseMessage(payload, 'If that email exists in our system, you’ll receive a reset link shortly.'), 'success'));
    } catch (error) {
      status.appendChild(formStatus(responseError(error), 'error'));
    } finally {
      setBusy(submit, false);
    }
  }, { type: 'submit' });
  form.addEventListener('submit', (event) => { event.preventDefault(); submit.click(); });
  form.appendChild(node('div', { class: 'form-actions' }, [submit]));
  const content = node('div', {}, [node('p', { class: 'auth-copy', text: 'Enter the email address associated with your portal account.' }), form, node('div', { class: 'auth-note', text: 'If that email exists in our system, you’ll receive a reset link shortly.' }), node('div', { class: 'auth-footer' }, [footerLink('Back to login', 'login')])]);
  clear(appRoot).appendChild(authShell('Reset your password', content));
  email.focus();
}

function renderResetPassword(query) {
  const token = query.get('token') || '';
  const status = node('div');
  if (!token) status.appendChild(formStatus('This reset link does not contain a token.', 'error'));
  const form = node('form', { class: 'auth-form' });
  const password = input('newPassword', 'password', { required: true, autocomplete: 'new-password' });
  const confirmation = input('confirmPassword', 'password', { required: true, autocomplete: 'new-password' });
  form.append(node('div', { class: 'form-grid' }, [field('New Password', password), field('Confirm Password', confirmation)]), status);
  const submit = button('Update password', 'app-button app-button-primary', async () => {
    if (!token) return;
    if (password.value !== confirmation.value) {
      clear(status).appendChild(formStatus('New password and confirmation do not match.', 'error'));
      return;
    }
    if (!form.reportValidity()) return;
    setBusy(submit, true, 'Updating…');
    clear(status);
    try {
      const payload = await request(`${ENDPOINTS.auth}/reset-password`, { method: 'POST', suppressAuthRedirect: true, body: { token, newPassword: password.value } });
      const message = responseMessage(payload, 'Your password was updated.');
      clear(appRoot).appendChild(authShell('Choose a new password', node('div', {}, [form, formStatus(message, 'success'), node('div', { class: 'auth-footer' }, [footerLink('Return to login', 'login')])])));
    } catch (error) {
      status.appendChild(formStatus(responseError(error), 'error'));
    } finally {
      setBusy(submit, false);
    }
  }, { type: 'submit' });
  form.addEventListener('submit', (event) => { event.preventDefault(); submit.click(); });
  form.appendChild(node('div', { class: 'form-actions' }, [submit]));
  const content = node('div', {}, [node('p', { class: 'auth-copy', text: 'Choose a new password for your Crestwood Academy portal account.' }), form, node('div', { class: 'auth-footer' }, [footerLink('Back to login', 'login')])]);
  clear(appRoot).appendChild(authShell('Choose a new password', content));
  password.focus();
}

function showLandingInfo(title, message) {
  openModal(node('div', {}, [node('h2', { class: 'modal-title', text: title }), node('p', { class: 'modal-copy', text: message }), node('div', { class: 'form-actions' }, [button('Close', 'app-button app-button-primary', () => openModalClose())])]));
}

function openModalClose() {
  document.querySelectorAll('.modal-layer').forEach((layer) => layer.remove());
  delete document.body.dataset.modalOpen;
}

function openStoryById(id) {
  const story = landingStories.find((item) => item.id === id) || landingStories[0];
  openModal(node('div', {}, [node('span', { class: 'auth-eyebrow', text: story.category }), node('h2', { class: 'modal-title', text: story.title }), node('p', { class: 'modal-copy', text: `${story.date} · ${story.author}` }), node('p', { class: 'confirm-copy', text: story.body }), node('div', { class: 'form-actions' }, [button('Close story', 'app-button app-button-primary', openModalClose)])]));
}

function renderLandingMobileContent() {
  const pathway = document.getElementById('pathwayCardMobile');
  const pathwayButtons = [...document.querySelectorAll('.pathway-tab-btn, .pathway-dot-btn')];
  const testimonials = document.getElementById('testimonialCardMobile');
  const testimonialButtons = [...document.querySelectorAll('.test-tab-btn, .test-dot-btn')];
  const news = document.getElementById('newsCardMobile');
  const newsButtons = [...document.querySelectorAll('.news-dot-btn')];
  const newsPrev = document.getElementById('newsPrevBtn');
  const newsNext = document.getElementById('newsNextBtn');
  const newsIndex = document.getElementById('newsIndexText');
  if (pathway) {
    const renderPathway = (index) => {
      const item = pathwayContent[index];
      clear(pathway);
      pathway.className = `pathway-card ${index === 1 ? 'dark' : 'light'}`;
      pathway.append(node('div', {}, [node('span', { class: 'cat-label', text: item.category }), node('h3', { class: 'card-title', text: item.title }), node('p', { class: 'card-body', text: item.body })]), button(item.action, 'card-btn', () => { if (index === 2) navigate('/login'); else showLandingInfo('Crestwood Academy Programs', 'Explore the Academy’s academic, extracurricular, and student support pathways through the admissions office.'); }));
      pathwayButtons.forEach((control) => control.classList.toggle('active', Number(control.dataset.index) === index));
    };
    pathwayButtons.forEach((control) => control.addEventListener('click', () => renderPathway(Number(control.dataset.index))));
    renderPathway(0);
  }
  if (testimonials) {
    const renderTestimonial = (index) => {
      const item = testimonialContent[index];
      clear(testimonials);
      testimonials.append(node('div', {}, [node('span', { class: 'test-badge', text: item.badge }), node('blockquote', { class: 'test-quote', text: `“${item.quote}”` })]), node('div', { class: 'test-author-info' }, [node('div', { class: 'test-author-name', text: item.author }), node('div', { class: 'test-author-role', text: item.role })]));
      testimonialButtons.forEach((control) => control.classList.toggle('active', Number(control.dataset.index) === index));
    };
    testimonialButtons.forEach((control) => control.addEventListener('click', () => renderTestimonial(Number(control.dataset.index))));
    renderTestimonial(0);
  }
  if (news) {
    let index = 0;
    const renderNews = (nextIndex) => {
      index = (nextIndex + landingStories.length) % landingStories.length;
      const item = landingStories[index];
      clear(news);
      const card = button('', 'news-card', () => openStoryById(item.id));
      card.append(node('div', { class: 'news-card-img' }, [node('span', { class: 'news-card-tag', text: item.category })]), node('div', { class: 'news-card-body' }, [node('span', { class: 'news-date', text: item.date }), node('h3', { class: 'news-title', text: item.title }), node('p', { class: 'card-body', text: item.body })]));
      news.appendChild(card);
      if (newsIndex) newsIndex.textContent = `Story ${index + 1} of ${landingStories.length}`;
      newsButtons.forEach((control) => control.classList.toggle('active', Number(control.dataset.index) === index));
    };
    newsButtons.forEach((control) => control.addEventListener('click', () => renderNews(Number(control.dataset.index))));
    if (newsPrev) newsPrev.addEventListener('click', () => renderNews(index - 1));
    if (newsNext) newsNext.addEventListener('click', () => renderNews(index + 1));
    renderNews(0);
  }
}

function bindLanding() {
  if (landingBound) return;
  landingBound = true;
  const mobileToggle = document.getElementById('mobileToggle');
  const mobileDrawer = document.getElementById('mobileDrawer');
  const mobileBackdrop = document.getElementById('mobileBackdrop');
  const closeMobile = () => {
    if (mobileToggle) mobileToggle.classList.remove('active');
    if (mobileDrawer) mobileDrawer.classList.remove('open');
    if (mobileBackdrop) mobileBackdrop.classList.remove('open');
    document.body.style.overflow = '';
  };
  if (mobileToggle) mobileToggle.addEventListener('click', () => {
    const open = !mobileDrawer.classList.contains('open');
    mobileToggle.classList.toggle('active', open);
    mobileDrawer.classList.toggle('open', open);
    mobileBackdrop.classList.toggle('open', open);
    document.body.style.overflow = open ? 'hidden' : '';
  });
  if (mobileBackdrop) mobileBackdrop.addEventListener('click', closeMobile);
  document.querySelectorAll('[data-scroll-to]').forEach((control) => control.addEventListener('click', (event) => {
    event.preventDefault();
    closeMobile();
    const target = document.getElementById(control.dataset.scrollTo);
    if (target) window.scrollTo({ top: target.getBoundingClientRect().top + window.scrollY - 80, behavior: 'smooth' });
  }));
  const portalWrapper = document.getElementById('desktopPortalWrapper');
  const portalButton = document.getElementById('desktopPortalBtn');
  if (portalWrapper && portalButton) {
    portalButton.addEventListener('click', (event) => {
      event.stopPropagation();
      const open = portalWrapper.classList.toggle('open');
      portalButton.setAttribute('aria-expanded', String(open));
    });
    document.addEventListener('click', (event) => {
      if (!portalWrapper.contains(event.target)) {
        portalWrapper.classList.remove('open');
        portalButton.setAttribute('aria-expanded', 'false');
      }
    });
  }
  const mobilePortalSection = document.getElementById('mobilePortalSection');
  const mobilePortalButton = document.getElementById('mobilePortalBtn');
  if (mobilePortalSection && mobilePortalButton) mobilePortalButton.addEventListener('click', () => mobilePortalSection.classList.toggle('expanded'));
  const bindTriggers = () => {
    document.querySelectorAll('[data-open-modal]').forEach((control) => {
      if (control.dataset.bound === 'true') return;
      control.dataset.bound = 'true';
      control.addEventListener('click', (event) => {
        event.preventDefault();
        const type = control.dataset.openModal;
        if (type === 'apply') {
          closeMobile();
          navigate('/enroll');
          return;
        }
        if (type === 'portal') {
          closeMobile();
          const payload = String(control.dataset.modalPayload || '').toLowerCase();
          const role = payload.includes('parent') ? 'parent' : payload.includes('teacher') ? 'teacher' : payload.includes('admin') ? 'SUPER_ADMIN' : payload.includes('student') ? 'student' : '';
          navigate(role ? `/login?role=${encodeURIComponent(role)}` : '/login');
          return;
        }
        if (type === 'video') showLandingInfo('Crestwood Academy Campus', 'Campus video content is available through the Academy admissions office.');
        else if (type === 'tour') showLandingInfo('Schedule a campus visit', 'Contact the Crestwood Academy admissions office to arrange a campus visit.');
        else if (type === 'prospectus') showLandingInfo('Crestwood Academy prospectus', 'Request the current prospectus and admissions information from the Academy admissions office.');
      });
    });
  };
  bindTriggers();
  renderLandingMobileContent();
  document.querySelectorAll('.news-card[onclick]').forEach((card) => {
    const match = card.getAttribute('onclick')?.match(/'([^']+)'/);
    if (match) card.addEventListener('click', () => openStoryById(match[1]));
  });
  window.openStoryById = openStoryById;
}

function renderRoute() {
  const route = parseRoute();
  if (!route.segments.length) {
    shell = null;
    setApplicationMode(false);
    clear(appRoot);
    bindLanding();
    return;
  }
  if (routeIsPublic(route.segments)) {
    shell = null;
    renderPublicRoute(route.segments[0], route.query);
    return;
  }
  if (route.segments[0] === 'app') {
    if (!session) {
      navigate('/login');
      return;
    }
    renderAppRoute(route.segments, route.query);
    return;
  }
  navigate('/');
}

function normalizePathRoute() {
  const path = window.location.pathname.replace(/^\/+/, '');
  if (!path || path === 'index.html') return;
  const search = window.location.search.replace(/^\?/, '');
  if (path.startsWith('api/')) return;
  window.location.replace(`${window.location.pathname.split('/').slice(0, 1).join('/')}/#/${path}${search ? `?${search}` : ''}`);
}

window.addEventListener('hashchange', renderRoute);
window.addEventListener('crestwood:unauthorized', () => {
  if (!session) return;
  clearSession();
  session = null;
  shell = null;
  navigate('/login');
});
window.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    document.querySelectorAll('.modal-layer').forEach((layer) => layer.remove());
    delete document.body.dataset.modalOpen;
  }
});
normalizePathRoute();
renderRoute();
