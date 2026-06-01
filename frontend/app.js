(() => {
  const API_BASE = (window.COUPON_API_BASE || '/api').replace(/\/$/, '');
  const CODE_MAX = 6;
  const DESCRIPTION_MAX = 255;
  const CODE_RAW_MAX = 60;
  const DISCOUNT_MIN_CENTS = 50;

  const $ = (sel) => document.querySelector(sel);

  const els = {
    form:        $('#create-form'),
    code:        $('#code'),
    codeCounter: $('#code-counter'),
    codePreview: $('#code-preview'),
    codeError:   $('#code-error'),
    description: $('#description'),
    descCounter: $('#description-counter'),
    descError:   $('#description-error'),
    discount:    $('#discountValue'),
    discountErr: $('#discount-error'),
    expiration:  $('#expirationDate'),
    expirationErr: $('#expiration-error'),
    published:   $('#published'),
    submit:      $('#submit-btn'),
    list:        $('#coupons-list'),
    empty:       $('#empty-state'),
    count:       $('#coupons-count'),
    search:      $('#search'),
    refresh:     $('#refresh-btn'),
    status:      $('#api-status'),
    cardTpl:     $('#card-template'),
    toasts:      $('#toast-container'),
    confirm: {
      backdrop: $('#confirm-backdrop'),
      title:    $('#confirm-title'),
      msg:      $('#confirm-message'),
      ok:       $('#confirm-ok'),
      cancel:   $('#confirm-cancel'),
    },
    detail: {
      backdrop:    $('#detail-backdrop'),
      modal:       $('#detail-modal'),
      initials:    $('#detail-initials'),
      code:        $('#detail-code'),
      code2:       $('#detail-code2'),
      pubBadge:    $('#detail-pub-badge'),
      delBadge:    $('#detail-del-badge'),
      description: $('#detail-description'),
      id:          $('#detail-id'),
      discount:    $('#detail-discount'),
      expiration:  $('#detail-expiration'),
      created:     $('#detail-created'),
      deleted:     $('#detail-deleted'),
      status:      $('#detail-status'),
      copyId:      $('#detail-copy-id'),
      copyCode:    $('#detail-copy-code'),
      close:       $('#detail-close'),
      cancel:      $('#detail-cancel'),
      delete:      $('#detail-delete'),
    },
  };

  const state = {
    coupons: [],
    filter: '',
    discountCents: 0,
  };

  const sanitizeCode = (s) => (s || '').replace(/[^A-Za-z0-9]/g, '').toUpperCase();

  const fmtMoney = (cents) => {
    const value = (cents / 100);
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  };

  const fmtMoneyFromValue = (val) => {
    if (val === null || val === undefined) return '—';
    return Number(val).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  };

  const fmtDateBR = (iso) => {
    if (!iso) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  };

  const fmtInstantBR = (iso) => {
    if (!iso) return '—';
    try {
      const dt = new Date(iso);
      return dt.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch { return iso; }
  };

  function toast({ type = 'info', title, message, timeout = 4000 }) {
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    const icon = type === 'success' ? '<path d="M20 6 9 17l-5-5"/>'
      : type === 'error' ? '<circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/>'
      : type === 'warning' ? '<path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3z"/><path d="M12 9v4"/><path d="m12 17 .01 0"/>'
      : '<circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="m12 8 .01 0"/>';
    t.innerHTML = `
      <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">${icon}</svg>
      <div class="flex-1 leading-tight">
        ${title ? `<div>${escapeHtml(title)}</div>` : ''}
        ${message ? `<div class="font-normal opacity-90 text-xs mt-0.5">${escapeHtml(message)}</div>` : ''}
      </div>
      <button class="opacity-70 hover:opacity-100" aria-label="Fechar">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
      </button>`;
    els.toasts.appendChild(t);
    requestAnimationFrame(() => t.classList.add('is-visible'));
    const remove = () => { t.classList.add('is-leaving'); setTimeout(() => t.remove(), 250); };
    t.querySelector('button').addEventListener('click', remove);
    if (timeout) setTimeout(remove, timeout);
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, (c) => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' }[c]));
  }

  function openConfirm({ title, message, confirmText = 'Confirmar' }) {
    els.confirm.title.textContent = title;
    els.confirm.msg.textContent = message;
    els.confirm.ok.textContent = confirmText;
    els.confirm.backdrop.classList.add('is-open');
    return new Promise((resolve) => {
      const cleanup = (value) => {
        els.confirm.backdrop.classList.remove('is-open');
        els.confirm.ok.removeEventListener('click', onOk);
        els.confirm.cancel.removeEventListener('click', onCancel);
        els.confirm.backdrop.removeEventListener('click', onBackdrop);
        document.removeEventListener('keydown', onKey);
        resolve(value);
      };
      const onOk = () => cleanup(true);
      const onCancel = () => cleanup(false);
      const onBackdrop = (e) => { if (e.target === els.confirm.backdrop) cleanup(false); };
      const onKey = (e) => { if (e.key === 'Escape') cleanup(false); };
      els.confirm.ok.addEventListener('click', onOk);
      els.confirm.cancel.addEventListener('click', onCancel);
      els.confirm.backdrop.addEventListener('click', onBackdrop);
      document.addEventListener('keydown', onKey);
    });
  }

  function openDetail(coupon) {
    const d = els.detail;
    d.initials.textContent = coupon.code.slice(0, 3);
    d.code.textContent = coupon.code;
    d.code2.textContent = coupon.code;
    d.description.textContent = coupon.description;
    d.id.textContent = `#${coupon.id}`;
    d.discount.textContent = fmtMoneyFromValue(coupon.discountValue);
    d.expiration.textContent = fmtDateBR(coupon.expirationDate);
    d.created.textContent = fmtInstantBR(coupon.createdAt);
    d.deleted.textContent = coupon.deletedAt ? fmtInstantBR(coupon.deletedAt) : '—';

    d.pubBadge.textContent = coupon.published ? 'Publicado' : 'Rascunho';
    d.delBadge.classList.toggle('hidden', !coupon.deleted);

    const statusEl = d.status;
    statusEl.innerHTML = '';
    const pill = (txt, cls) => `<span class="inline-block text-[11px] uppercase font-bold px-2 py-1 rounded-full mr-2 ${cls}">${txt}</span>`;
    statusEl.innerHTML =
      pill(coupon.published ? 'Publicado' : 'Rascunho', coupon.published ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600') +
      pill(coupon.deleted ? 'Deletado' : 'Ativo', coupon.deleted ? 'bg-rose-100 text-rose-700' : 'bg-sky-100 text-sky-700');

    d.delete.disabled = !!coupon.deleted;
    d.delete.title = coupon.deleted ? 'Cupom já foi deletado' : 'Realizar soft delete';

    d.backdrop.classList.add('is-open');

    const cleanup = () => {
      d.backdrop.classList.remove('is-open');
      d.close.removeEventListener('click', cleanup);
      d.cancel.removeEventListener('click', cleanup);
      d.backdrop.removeEventListener('click', onBackdrop);
      d.copyId.removeEventListener('click', onCopyId);
      d.copyCode.removeEventListener('click', onCopyCode);
      d.delete.removeEventListener('click', onDelete);
      document.removeEventListener('keydown', onKey);
    };
    const onBackdrop = (e) => { if (e.target === d.backdrop) cleanup(); };
    const onKey = (e) => { if (e.key === 'Escape') cleanup(); };
    const onCopyId = () => { navigator.clipboard.writeText(String(coupon.id)); toast({ type: 'info', title: 'Copiado', message: `id ${coupon.id}` }); };
    const onCopyCode = () => { navigator.clipboard.writeText(coupon.code); toast({ type: 'info', title: 'Copiado', message: coupon.code }); };
    const onDelete = async () => { cleanup(); await confirmDelete(coupon); };

    d.close.addEventListener('click', cleanup);
    d.cancel.addEventListener('click', cleanup);
    d.backdrop.addEventListener('click', onBackdrop);
    d.copyId.addEventListener('click', onCopyId);
    d.copyCode.addEventListener('click', onCopyCode);
    d.delete.addEventListener('click', onDelete);
    document.addEventListener('keydown', onKey);
  }

  async function api(path, opts = {}) {
    const res = await fetch(`${API_BASE}${path}`, {
      ...opts,
      headers: { 'Accept': 'application/json', ...(opts.body ? { 'Content-Type': 'application/json' } : {}), ...(opts.headers || {}) },
    });
    if (res.status === 204) return null;
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
      const err = new Error(data?.message || `HTTP ${res.status}`);
      err.status = res.status;
      err.payload = data;
      throw err;
    }
    return data;
  }

  async function loadCoupons() {
    setStatus('loading');
    els.list.innerHTML = '<div class="skeleton"></div><div class="skeleton"></div><div class="skeleton"></div>';
    els.empty.classList.add('hidden');
    try {
      state.coupons = await api('/coupon');
      renderList();
      setStatus('online');
    } catch (err) {
      setStatus('offline');
      els.list.innerHTML = '';
      els.empty.classList.remove('hidden');
      els.count.textContent = '';
      toast({ type: 'error', title: 'Falha ao carregar', message: err.message });
    }
  }

  function renderList() {
    const q = state.filter.trim().toLowerCase();
    const filtered = q
      ? state.coupons.filter(c => c.code.toLowerCase().includes(q) || c.description.toLowerCase().includes(q))
      : state.coupons;

    els.list.innerHTML = '';
    if (filtered.length === 0) {
      els.empty.classList.remove('hidden');
      els.count.textContent = state.coupons.length === 0 ? 'Nenhum cupom criado ainda.' : 'Nenhum resultado para a busca.';
      return;
    }
    els.empty.classList.add('hidden');
    els.count.textContent = `${filtered.length} de ${state.coupons.length} cupom${state.coupons.length > 1 ? 's' : ''}.`;

    filtered.forEach((c) => {
      const node = els.cardTpl.content.firstElementChild.cloneNode(true);
      node.querySelector('[data-field="initials"]').textContent = c.code.slice(0, 3);
      node.querySelector('[data-field="code"]').textContent = c.code;
      node.querySelector('[data-field="description"]').textContent = c.description;
      node.querySelector('[data-field="expirationDate"]').textContent = fmtDateBR(c.expirationDate);
      node.querySelector('[data-field="discountValue"]').textContent = fmtMoneyFromValue(c.discountValue);
      node.querySelector('[data-field="id"]').textContent = c.id;
      const pubBadge = node.querySelector('[data-field="published-badge"]');
      if (c.published) {
        pubBadge.textContent = 'Publicado';
        pubBadge.classList.add('bg-emerald-100', 'text-emerald-700');
      } else {
        pubBadge.textContent = 'Rascunho';
        pubBadge.classList.add('bg-slate-100', 'text-slate-600');
      }
      const delBtn = node.querySelector('[data-action="delete"]');
      if (c.deleted) {
        node.querySelector('[data-field="deleted-badge"]').classList.remove('hidden');
        node.classList.add('opacity-60');
        delBtn.disabled = true;
        delBtn.title = 'Cupom já deletado';
      }
      node.addEventListener('click', (e) => {
        if (e.target.closest('[data-action]')) return;
        openDetail(c);
      });
      node.querySelector('[data-action="view"]').addEventListener('click', (e) => { e.stopPropagation(); openDetail(c); });
      delBtn.addEventListener('click', (e) => { e.stopPropagation(); confirmDelete(c); });
      els.list.appendChild(node);
    });
  }

  async function confirmDelete(coupon) {
    if (coupon.deleted) {
      toast({ type: 'warning', title: 'Cupom já deletado', message: `${coupon.code} já estava marcado como deletado.` });
      return;
    }
    const ok = await openConfirm({
      title: `Deletar cupom ${coupon.code}?`,
      message: `Isso fará um soft delete do cupom #${coupon.id}. Os dados originais serão preservados, mas o cupom ficará marcado como deletado.`,
      confirmText: 'Sim, deletar',
    });
    if (!ok) return;
    try {
      await api(`/coupon/${coupon.id}`, { method: 'DELETE' });
      toast({ type: 'success', title: 'Cupom deletado', message: `${coupon.code} foi marcado como deletado.` });
      await loadCoupons();
    } catch (err) {
      if (err.status === 409) toast({ type: 'warning', title: 'Já estava deletado', message: err.message });
      else if (err.status === 404) toast({ type: 'warning', title: 'Cupom não encontrado', message: 'A página será atualizada.' });
      else toast({ type: 'error', title: 'Falha ao deletar', message: err.message });
      await loadCoupons();
    }
  }

  function validate() {
    let ok = true;
    const sanitized = sanitizeCode(els.code.value);
    if (sanitized.length !== CODE_MAX) {
      showError(els.codeError, `O código precisa de exatamente ${CODE_MAX} caracteres alfanuméricos (atual: ${sanitized.length}).`);
      ok = false;
    } else hideError(els.codeError);

    const desc = els.description.value.trim();
    if (!desc) { showError(els.descError, 'A descrição é obrigatória.'); ok = false; }
    else if (desc.length > DESCRIPTION_MAX) { showError(els.descError, `Máximo ${DESCRIPTION_MAX} caracteres.`); ok = false; }
    else hideError(els.descError);

    if (state.discountCents < DISCOUNT_MIN_CENTS) {
      showError(els.discountErr, `Desconto mínimo é R$ 0,50.`);
      ok = false;
    } else hideError(els.discountErr);

    const exp = els.expiration.value;
    if (!exp) { showError(els.expirationErr, 'Data de expiração é obrigatória.'); ok = false; }
    else if (exp < els.expiration.min) { showError(els.expirationErr, 'A data não pode estar no passado.'); ok = false; }
    else hideError(els.expirationErr);

    return ok;
  }

  function showError(el, msg) { el.textContent = msg; el.classList.remove('hidden'); }
  function hideError(el) { el.classList.add('hidden'); el.textContent = ''; }

  async function submitCreate(e) {
    e.preventDefault();
    if (!validate()) {
      toast({ type: 'error', title: 'Verifique os campos', message: 'Há campos inválidos no formulário.' });
      return;
    }
    const payload = {
      code: els.code.value,
      description: els.description.value.trim(),
      discountValue: (state.discountCents / 100),
      expirationDate: els.expiration.value,
      published: els.published.checked,
    };
    els.submit.disabled = true;
    const originalText = els.submit.innerHTML;
    els.submit.innerHTML = '<svg class="w-5 h-5 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z"></path></svg> Criando…';
    try {
      const created = await api('/coupon', { method: 'POST', body: JSON.stringify(payload) });
      toast({ type: 'success', title: 'Cupom criado!', message: `${created.code} salvo com sucesso.` });
      resetForm();
      await loadCoupons();
    } catch (err) {
      if (err.status === 400) {
        const violations = err.payload?.violations;
        const detail = (violations && violations.length)
          ? violations.map(v => `${v.field}: ${v.message}`).join(' · ')
          : err.message;
        toast({ type: 'error', title: 'Não foi possível criar', message: detail, timeout: 7000 });
      } else {
        toast({ type: 'error', title: 'Falha ao criar', message: err.message });
      }
    } finally {
      els.submit.disabled = false;
      els.submit.innerHTML = originalText;
    }
  }

  function resetForm() {
    els.form.reset();
    state.discountCents = 0;
    els.discount.value = '';
    els.codePreview.classList.add('hidden');
    updateCodeCounter();
    updateDescCounter();
    initExpirationDefault();
  }

  function setStatus(kind) {
    const dot = els.status.querySelector('span:first-child');
    if (kind === 'online')  { els.status.lastChild.textContent = ' API online '; dot.className = 'w-2 h-2 rounded-full bg-emerald-500'; }
    if (kind === 'offline') { els.status.lastChild.textContent = ' API offline '; dot.className = 'w-2 h-2 rounded-full bg-rose-500'; }
    if (kind === 'loading') { els.status.lastChild.textContent = ' Carregando… '; dot.className = 'w-2 h-2 rounded-full bg-slate-300 animate-pulse'; }
  }

  function handleCodeInput(e) {
    let raw = els.code.value;
    if (raw.length > CODE_RAW_MAX) raw = raw.slice(0, CODE_RAW_MAX);
    const sanitized = sanitizeCode(raw);
    if (sanitized.length > CODE_MAX) {
      let kept = 0;
      let truncated = '';
      for (const ch of raw) {
        if (/[A-Za-z0-9]/.test(ch)) {
          if (kept >= CODE_MAX) continue;
          kept++;
        }
        truncated += ch;
      }
      raw = truncated;
    }
    raw = raw.toUpperCase();
    if (els.code.value !== raw) els.code.value = raw;
    updateCodeCounter();
    if (sanitizeCode(raw).length === CODE_MAX) hideError(els.codeError);
  }

  function updateCodeCounter() {
    const sanitized = sanitizeCode(els.code.value);
    els.codeCounter.textContent = `${sanitized.length}/${CODE_MAX} alfanuméricos`;
    els.codeCounter.classList.toggle('text-emerald-600', sanitized.length === CODE_MAX);
    els.codeCounter.classList.toggle('text-slate-400', sanitized.length !== CODE_MAX);
    if (sanitized && sanitized !== els.code.value.toUpperCase()) {
      els.codePreview.classList.remove('hidden');
      els.codePreview.textContent = `Após sanitização: ${sanitized}`;
    } else {
      els.codePreview.classList.add('hidden');
    }
  }

  function updateDescCounter() {
    const len = els.description.value.length;
    els.descCounter.textContent = `${len}/${DESCRIPTION_MAX}`;
    els.descCounter.classList.toggle('text-emerald-600', len > 0 && len <= DESCRIPTION_MAX);
    els.descCounter.classList.toggle('text-rose-600', len > DESCRIPTION_MAX);
    els.descCounter.classList.toggle('text-slate-400', len === 0);
  }

  function handleDiscountInput(e) {
    const digits = (els.discount.value || '').replace(/\D/g, '').slice(0, 12);
    state.discountCents = digits ? parseInt(digits, 10) : 0;
    els.discount.value = state.discountCents > 0 ? fmtMoney(state.discountCents) : '';
    if (state.discountCents >= DISCOUNT_MIN_CENTS) hideError(els.discountErr);
  }

  function handleDiscountKeydown(e) {
    const allowed = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Tab', 'Home', 'End'];
    if (allowed.includes(e.key)) return;
    if (e.ctrlKey || e.metaKey) return;
    if (!/^[0-9]$/.test(e.key)) e.preventDefault();
  }

  function initExpirationDefault() {
    const today = new Date();
    const min = today.toISOString().slice(0, 10);
    els.expiration.min = min;
    if (!els.expiration.value) {
      const future = new Date(today);
      future.setDate(future.getDate() + 30);
      els.expiration.value = future.toISOString().slice(0, 10);
    }
  }

  function bindEvents() {
    els.form.addEventListener('submit', submitCreate);
    els.refresh.addEventListener('click', loadCoupons);
    els.search.addEventListener('input', (e) => { state.filter = e.target.value; renderList(); });

    els.code.addEventListener('input', handleCodeInput);
    els.code.addEventListener('blur', () => {
      const sanitized = sanitizeCode(els.code.value);
      if (sanitized.length > 0 && sanitized.length < CODE_MAX) {
        showError(els.codeError, `O código precisa de ${CODE_MAX} caracteres alfanuméricos (atual: ${sanitized.length}).`);
      }
    });

    els.description.addEventListener('input', updateDescCounter);
    els.description.addEventListener('blur', () => {
      const v = els.description.value.trim();
      if (!v) showError(els.descError, 'A descrição é obrigatória.');
      else hideError(els.descError);
    });

    els.discount.addEventListener('input', handleDiscountInput);
    els.discount.addEventListener('keydown', handleDiscountKeydown);
    els.discount.addEventListener('blur', () => {
      if (state.discountCents > 0 && state.discountCents < DISCOUNT_MIN_CENTS) {
        showError(els.discountErr, 'Desconto mínimo é R$ 0,50.');
      }
    });

    els.expiration.addEventListener('change', () => {
      if (els.expiration.value && els.expiration.value < els.expiration.min) {
        showError(els.expirationErr, 'A data não pode estar no passado.');
      } else hideError(els.expirationErr);
    });
  }

  initExpirationDefault();
  bindEvents();
  updateCodeCounter();
  updateDescCounter();
  loadCoupons();
})();
