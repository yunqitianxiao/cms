/** 与业务无关的界面小工具 */

export function esc(v) {
    if (v === null || v === undefined) return '';
    return String(v)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/** 后端 LocalDateTime 序列化成 ISO（2026-10-08T10:20:00），展示时换成空格 */
export function fmtTime(v, withSeconds = false) {
    if (!v) return '—';
    const s = String(v).replace('T', ' ');
    return withSeconds ? s.slice(0, 19) : s.slice(0, 16);
}

export function fmtNum(n) {
    if (n === null || n === undefined) return '0';
    return Number(n).toLocaleString('zh-CN');
}

const STATUS_LABEL = {
    DRAFT: '草稿', PENDING: '待审核', PUBLISHED: '已发布',
    REJECTED: '已驳回', OFFLINE: '已下架',
};

export function statusLabel(s) {
    return STATUS_LABEL[s] || s || '—';
}

export function statusBadge(s) {
    return `<span class="badge ${esc(s)}">${esc(statusLabel(s))}</span>`;
}

export function debounce(fn, ms = 300) {
    let t;
    return (...args) => {
        clearTimeout(t);
        t = setTimeout(() => fn(...args), ms);
    };
}

/* ---------------- toast ---------------- */

let toastHost = null;

export function toast(message, type = '') {
    if (!toastHost) {
        toastHost = document.createElement('div');
        toastHost.className = 'toast-host';
        document.body.appendChild(toastHost);
    }
    const node = document.createElement('div');
    node.className = 'toast ' + type;
    node.textContent = message;
    toastHost.appendChild(node);
    setTimeout(() => {
        node.style.transition = 'opacity .2s';
        node.style.opacity = '0';
        setTimeout(() => node.remove(), 220);
    }, type === 'err' ? 4200 : 2400);
}

/* ---------------- modal ---------------- */

/**
 * @param {{title:string, body:string|HTMLElement, footer?:string, wide?:boolean}} opts
 * @returns {{root:HTMLElement, close:()=>void, body:HTMLElement}}
 */
export function modal(opts) {
    const mask = document.createElement('div');
    mask.className = 'modal-mask';
    mask.innerHTML = `
        <div class="modal ${opts.wide ? 'wide' : ''}" role="dialog" aria-modal="true">
            <div class="modal-head">
                <h3>${esc(opts.title || '')}</h3>
                <button class="x" type="button" aria-label="关闭">×</button>
            </div>
            <div class="modal-body"></div>
            ${opts.footer ? `<div class="modal-foot">${opts.footer}</div>` : ''}
        </div>`;

    const body = mask.querySelector('.modal-body');
    if (opts.body instanceof HTMLElement) body.appendChild(opts.body);
    else body.innerHTML = opts.body || '';

    const close = () => {
        if (!mask.isConnected) return;
        mask.remove();
        document.removeEventListener('keydown', onKey);
    };
    const onKey = (e) => { if (e.key === 'Escape') close(); };

    mask.querySelector('.x').addEventListener('click', close);
    mask.addEventListener('mousedown', (e) => { if (e.target === mask) close(); });
    document.addEventListener('keydown', onKey);
    document.body.appendChild(mask);

    const first = mask.querySelector('input:not([type=hidden]), select, textarea');
    if (first) setTimeout(() => first.focus(), 30);

    return { root: mask, body, close };
}

/** 二次确认，返回 Promise<boolean> */
export function confirmDialog(message, { title = '确认操作', okText = '确定', danger = false } = {}) {
    return new Promise((resolve) => {
        const m = modal({
            title,
            body: `<div style="font-size:13.5px;line-height:1.75;color:var(--ink-2)">${esc(message)}</div>`,
            footer: `<button class="btn" data-act="cancel">取消</button>
                     <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-act="ok">${esc(okText)}</button>`,
        });
        m.root.querySelector('[data-act=cancel]').onclick = () => { m.close(); resolve(false); };
        m.root.querySelector('[data-act=ok]').onclick = () => { m.close(); resolve(true); };
    });
}

/* ---------------- 表格状态 ---------------- */

export function loadingRow(cols, text = '加载中') {
    return `<tr><td colspan="${cols}"><div class="loading"><span class="spinner"></span>${esc(text)}</div></td></tr>`;
}

export function emptyRow(cols, text = '暂无数据') {
    return `<tr><td colspan="${cols}"><div class="empty"><div class="big">◌</div>${esc(text)}</div></td></tr>`;
}

/* ---------------- 分页 ---------------- */

/** @param {HTMLElement} host @param {{pageNum,pageSize,pages,total}} page @param {(n:number)=>void} onGo */
export function renderPager(host, page, onGo) {
    if (!page) { host.innerHTML = ''; return; }
    const cur = Number(page.pageNum) || 1;
    const pages = Number(page.pages) || 0;
    const total = Number(page.total) || 0;
    host.innerHTML = `
        <span>共 <span class="cur">${total}</span> 条 · 第 <span class="cur">${cur}</span> / ${pages || 1} 页</span>
        <span class="spacer"></span>
        <button class="btn btn-sm" data-go="1" ${cur <= 1 ? 'disabled' : ''}>首页</button>
        <button class="btn btn-sm" data-go="${cur - 1}" ${cur <= 1 ? 'disabled' : ''}>上一页</button>
        <button class="btn btn-sm" data-go="${cur + 1}" ${cur >= pages ? 'disabled' : ''}>下一页</button>
        <button class="btn btn-sm" data-go="${pages}" ${cur >= pages ? 'disabled' : ''}>末页</button>`;
    host.querySelectorAll('[data-go]').forEach((b) => {
        b.onclick = () => onGo(Number(b.dataset.go));
    });
}

/** 从一串 options 生成 select 的 HTML */
export function options(list, valueKey, labelKey, selected, placeholder) {
    let html = placeholder ? `<option value="">${esc(placeholder)}</option>` : '';
    for (const it of list) {
        const v = valueKey ? it[valueKey] : it;
        const l = labelKey ? it[labelKey] : it;
        html += `<option value="${esc(v)}" ${String(v) === String(selected) ? 'selected' : ''}>${esc(l)}</option>`;
    }
    return html;
}
