import {
  ENDPOINTS,
  extractReference,
  getField,
  isVerifiedPayment,
  listRequest,
  loadJsPdf,
  loadPaystack,
  paystackConfig,
  request,
  textFromPayload
} from './api.js';
import {
  actionLink,
  button,
  clear,
  closeModal,
  confirmDialog,
  emptyState,
  errorState,
  formatDate,
  formatMoney,
  formatTerm,
  loading,
  node,
  openModal,
  panel,
  pageHeading,
  previewList,
  responseError,
  setBusy,
  sortRecent,
  table,
  toast
} from './ui.js';
import {
  amountOwedOf,
  amountPaidOf,
  classLabel,
  currentOption,
  dateOf,
  idFor,
  loadOptions,
  moneyNumber,
  showRetry,
  sortByDateDesc,
  studentLabel,
  subjectLabel,
  termLabel,
  valueOf
} from './page-utils.js';

function childrenEndpoint() {
  return `${ENDPOINTS.parents}/me/children`;
}

function childIdentifier(child) {
  return valueOf(child, ['studentId', 'id', 'childId'], '');
}

function childName(child) {
  return valueOf(child, ['studentFullName', 'fullName', 'studentName', 'name'], '');
}

function invoiceIdentifier(invoice) {
  return valueOf(invoice, ['invoiceId', 'feeInvoiceId', 'id'], '');
}

function selectedChildFromHash(children) {
  const id = new URLSearchParams(window.location.hash.split('?')[1] || '').get('childId');
  return children.find((child) => String(childIdentifier(child)) === String(id)) || children[0] || null;
}

function childSwitcher(children, selected, page) {
  if (!children || children.length < 2) return null;
  const tabs = node('div', { class: 'child-tabs', role: 'tablist', 'aria-label': 'Select child' });
  children.forEach((child) => {
    const id = childIdentifier(child);
    tabs.appendChild(button(childName(child), `child-tab${selected && String(childIdentifier(selected)) === String(id) ? ' active' : ''}`, () => {
      window.location.hash = `#/app/parent/${page}?childId=${encodeURIComponent(id)}`;
    }, { role: 'tab', 'aria-selected': selected && String(childIdentifier(selected)) === String(id) ? 'true' : 'false' }));
  });
  return tabs;
}

function isDue(invoice) {
  const status = String(valueOf(invoice, ['status', 'paymentStatus', 'state'], '')).toUpperCase();
  if (status === 'PAID') return false;
  return moneyNumber(invoice, amountOwedOf) > 0;
}

function latestResult(item, childId) {
  if (!item) return emptyState('No result has been posted for this child yet.');
  return node('div', { class: 'latest-result' }, [
    node('div', {}, [node('div', { class: 'latest-result-label', text: 'Latest result' }), node('div', { class: 'latest-result-subject', text: subjectLabel(item) || valueOf(item, ['subjectName'], '—') })]),
    node('div', { class: 'result-grade', text: valueOf(item, ['grade'], '—') }),
    actionLink('View Full Results →', () => { window.location.hash = `#/app/parent/results?childId=${encodeURIComponent(childId)}`; })
  ]);
}

export function parentOverview(context) {
  const root = node('div');
  const switcherHost = node('div');
  const feeBody = node('div', {}, [loading('Loading fee summary…')]);
  const resultBody = node('div', {}, [loading('Loading latest result…')]);
  const noticesBody = node('div', {}, [loading('Loading announcements…')]);
  root.append(
    pageHeading('Dashboard', 'Your child’s fees, results and school announcements'),
    switcherHost,
    node('div', { class: 'panel-stack' }, [panel('Fee Due', feeBody), panel('Latest Result', resultBody), panel('Latest Announcements', noticesBody)])
  );
  let children = [];
  let selected = null;
  const loadChildren = async () => {
    const childResult = await listRequest(childrenEndpoint());
    children = childResult.items;
    if (!children.length) throw new Error('No linked children were returned by the API.');
    selected = selectedChildFromHash(children);
    const switcher = childSwitcher(children, selected, 'overview');
    clear(switcherHost);
    if (switcher) switcherHost.appendChild(switcher);
    const childId = childIdentifier(selected);
    const [invoices, results, notices] = await Promise.allSettled([
      listRequest(ENDPOINTS.invoices, { studentId: childId }),
      listRequest(ENDPOINTS.results, { studentId: childId }),
      listRequest(ENDPOINTS.notices)
    ]);
    if (invoices.status === 'fulfilled') {
      const due = invoices.value.items.filter(isDue).sort((a, b) => new Date(dateOf(a, ['dueDate'])) - new Date(dateOf(b, ['dueDate'])));
      if (!due.length) {
        clear(feeBody).appendChild(emptyState('No outstanding fee was returned by the API for this child.'));
      } else {
        const invoice = due[0];
        const highlight = node('div', { class: 'fee-highlight' }, [
          node('div', {}, [node('div', { class: 'fee-amount', text: formatMoney(moneyNumber(invoice, amountOwedOf)) }), node('div', { class: 'fee-due-date', text: `Due ${formatDate(dateOf(invoice, ['dueDate']))}` })]),
          actionLink('Go to Fees →', () => context.navigate(`fees?childId=${encodeURIComponent(childId)}`), 'app-button app-button-blue')
        ]);
        clear(feeBody).appendChild(highlight);
      }
    } else {
      clear(feeBody).appendChild(errorState(responseError(invoices.reason), loadChildren));
    }
    if (results.status === 'fulfilled') clear(resultBody).appendChild(latestResult(sortByDateDesc(results.value.items, ['enteredAt'])[0], childId));
    else clear(resultBody).appendChild(errorState(responseError(results.reason), loadChildren));
    if (notices.status === 'fulfilled') {
      const rows = sortRecent(notices.value.items).slice(0, 3);
      clear(noticesBody).appendChild(previewList(rows, (item) => node('div', { class: 'preview-row' }, [node('div', { class: 'preview-main' }, [node('div', { class: 'preview-title', text: valueOf(item, ['title', 'headline'], 'Untitled notice') }), node('div', { class: 'preview-meta', text: formatDate(dateOf(item, ['postedAt', 'createdAt'])) })]), actionLink('View', () => showParentNotices())]), 'No announcements were returned by the API.'));
    } else {
      clear(noticesBody).appendChild(errorState(responseError(notices.reason), loadChildren));
    }
  };
  showRetry(switcherHost, loadChildren, () => {});
  return root;
}

async function showParentNotices() {
  try {
    const result = await listRequest(ENDPOINTS.notices);
    const body = previewList(sortRecent(result.items), (item) => node('div', { class: 'preview-row' }, [node('div', { class: 'preview-main' }, [node('div', { class: 'preview-title', text: valueOf(item, ['title', 'headline'], 'Untitled notice') }), node('div', { class: 'table-secondary', text: valueOf(item, ['body', 'message', 'content'], '') })] )]));
    closeModal();
    openModal(node('div', {}, [node('h2', { class: 'modal-title', text: 'Latest Announcements' }), body]), { wide: true });
  } catch (error) {
    toast(responseError(error), 'error');
  }
}

export function parentFees(context) {
  const root = node('div');
  const switcherHost = node('div');
  const dueBody = node('div', {}, [loading('Loading fee due…')]);
  const historyBody = node('div', { class: 'panel-body flush' }, [loading('Loading payment history…')]);
  root.append(
    pageHeading('Fees', 'Outstanding school fees and payment history'),
    switcherHost,
    node('div', { class: 'panel-stack' }, [panel('Fee Due', dueBody), panel('Payment History', historyBody, null, { flush: true })])
  );
  let children = [];
  let selected = null;
  const load = async () => {
    const childResult = await listRequest(childrenEndpoint());
    children = childResult.items;
    if (!children.length) throw new Error('No linked children were returned by the API.');
    selected = selectedChildFromHash(children);
    const switcher = childSwitcher(children, selected, 'fees');
    clear(switcherHost);
    if (switcher) switcherHost.appendChild(switcher);
    const childId = childIdentifier(selected);
    const [invoiceResult, paymentResult] = await Promise.all([
      listRequest(ENDPOINTS.invoices, { studentId: childId }),
      listRequest(ENDPOINTS.payments, { studentId: childId })
    ]);
    const due = invoiceResult.items.filter(isDue).sort((a, b) => new Date(dateOf(a, ['dueDate'])) - new Date(dateOf(b, ['dueDate'])));
    if (!due.length) {
      clear(dueBody).appendChild(emptyState('No outstanding fee was returned by the API for this child.'));
    } else {
      const invoice = due[0];
      const payButton = button('Pay with Paystack', 'app-button app-button-lime', () => startPaystack(invoice, selected, payButton, load), { type: 'button' });
      clear(dueBody).appendChild(node('div', { class: 'fee-highlight' }, [
        node('div', {}, [node('div', { class: 'fee-amount', text: formatMoney(moneyNumber(invoice, amountOwedOf)) }), node('div', { class: 'fee-due-date', text: `Due ${formatDate(dateOf(invoice, ['dueDate']))}` })]),
        payButton
      ]));
    }
    if (!paymentResult.items.length) {
      clear(historyBody).appendChild(emptyState('No payment history was returned by the API.'));
    } else {
      const rows = sortByDateDesc(paymentResult.items, ['paidAt', 'createdAt']);
      clear(historyBody).appendChild(table(['Term', 'Amount', 'Date', 'Status'], rows, (item, tr) => {
        tr.appendChild(node('td', { text: termLabel(item) || valueOf(item, ['termName'], '—') }));
        tr.appendChild(node('td', { text: formatMoney(moneyNumber(item, amountPaidOf)) }));
        tr.appendChild(node('td', { text: formatDate(dateOf(item, ['paidAt', 'paymentDate', 'createdAt'])) }));
        const status = String(valueOf(item, ['status', 'paymentStatus'], 'PENDING')).toUpperCase();
        tr.appendChild(node('td', {}, [node('span', { class: `status-badge ${status === 'PAID' ? 'status-approved' : 'status-pending'}`, text: status })]));
      }));
    }
  };
  showRetry(switcherHost, load, () => {});
  return root;
}

async function startPaystack(invoice, child, payButton, reload) {
  setBusy(payButton, true, 'Opening Paystack…');
  try {
    const config = await paystackConfig();
    const initPayload = await request(`${ENDPOINTS.payments}/initialize`, {
      method: 'POST',
      body: {
        studentId: childIdentifier(child),
        feeInvoiceId: invoiceIdentifier(invoice)
      }
    });
    const publicKey = textFromPayload(initPayload, ['publicKey'], '') || textFromPayload(config, ['publicKey'], '');
    const reference = extractReference(initPayload);
    const paymentId = getField(initPayload, ['paymentId'], '');
    const serverAmount = Number(textFromPayload(initPayload, ['amount'], '0'));
    if (!publicKey || !reference || !paymentId) throw new Error('The backend did not return a Paystack public key, reference and payment id.');
    if (!Number.isFinite(serverAmount) || serverAmount <= 0) throw new Error('The API returned an invalid payable amount.');
    const currency = textFromPayload(initPayload, ['currency'], '') || textFromPayload(config, ['currency'], 'NGN');
    const PaystackPop = await loadPaystack();
    const transaction = new PaystackPop();
    transaction.newTransaction({
      key: publicKey,
      email: textFromPayload(initPayload, ['email'], ''),
      amount: Math.round(serverAmount * 100),
      currency,
      reference,
      onSuccess: async (response) => {
        setBusy(payButton, true, 'Verifying payment…');
        try {
          const verification = await request(`${ENDPOINTS.payments}/verify`, {
            method: 'POST',
            body: { reference: response.reference, paymentId: paymentId, feeInvoiceId: invoiceIdentifier(invoice), studentId: childIdentifier(child) }
          });
          if (!isVerifiedPayment(verification)) throw new Error('The server did not confirm the payment as paid.');
          toast('Payment verified by the server.');
          await reload();
        } catch (error) {
          toast(responseError(error), 'error');
        } finally {
          setBusy(payButton, false);
        }
      },
      onCancel: () => {
        setBusy(payButton, false);
        toast('Paystack payment was cancelled. Nothing was charged.', 'error');
      }
    });
  } catch (error) {
    setBusy(payButton, false);
    toast(responseError(error), 'error');
  }
}

export function parentResults(context) {
  const root = node('div');
  const filterRow = node('div', { class: 'filter-row' });
  const body = node('div', { class: 'panel-body flush' }, [loading('Loading child results…')]);
  let selectedChild = null;
  let children = [];
  let termOptions = [];
  let selectedTerm = '';
  root.append(pageHeading('Results', 'Published results by child and academic term'), filterRow, panel('Results', body, null, { flush: true }));
  const renderControls = () => {
    clear(filterRow);
    const childControl = select('parentResultChild', children.map((child) => ({ value: childIdentifier(child), label: childName(child) })), childIdentifier(selectedChild), { 'aria-label': 'Child' });
    const termControl = select('parentResultTerm', termOptions, selectedTerm, { 'aria-label': 'Term' });
    childControl.addEventListener('change', () => { selectedChild = children.find((child) => String(childIdentifier(child)) === childControl.value) || children[0]; loadResults(); });
    termControl.addEventListener('change', () => { selectedTerm = termControl.value; loadResults(); });
    filterRow.append(childControl, termControl);
  };
  const loadResults = async () => {
    if (!selectedChild) return;
    const result = await listRequest(ENDPOINTS.results, { studentId: childIdentifier(selectedChild), termId: selectedTerm, size: 1000 });
    if (!result.items.length) {
      clear(body).appendChild(emptyState('No results were returned by the API for this child and term.'));
      return;
    }
    clear(body).appendChild(table(['Subject', 'CA1', 'CA2', 'Exam', 'Total', 'Grade'], result.items, (item, tr) => {
      tr.appendChild(node('td', { class: 'table-primary', text: subjectLabel(item) || valueOf(item, ['subjectName'], '—') }));
      ['ca1', 'ca2', 'exam', 'total'].forEach((key) => tr.appendChild(node('td', { text: valueOf(item, [key], '—') })));
      tr.appendChild(node('td', { class: 'table-primary', text: valueOf(item, ['grade'], '—') }));
    }));
  };
  const load = async () => {
    const [childResult, terms] = await Promise.all([listRequest(childrenEndpoint()), loadOptions(ENDPOINTS.terms)]);
    children = childResult.items;
    if (!children.length) throw new Error('No linked children were returned by the API.');
    selectedChild = selectedChildFromHash(children);
    termOptions = terms;
    selectedTerm = currentOption(termOptions, selectedTerm);
    renderControls();
    await loadResults();
  };
  showRetry(body, load, () => {});
  return root;
}

export const PARENT_PAGES = {
  overview: parentOverview,
  fees: parentFees,
  results: parentResults
};
