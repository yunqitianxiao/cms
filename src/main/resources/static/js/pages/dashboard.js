import { me, bootReady, canAny } from '../core/boot.js';
import { api } from '../core/api.js';
import { esc, fmtTime, statusBadge, toast } from '../core/ui.js';
import { askGuide } from '../guide.js';

// 本页没有依赖模块级 const，但保持同一写法：初始化异常要落到控制台而不是静默
bootReady.then(() => (me ? main() : null)).catch((e) => console.error('工作台初始化失败', e));

async function main() {
    const hour = new Date().getHours();
    const hello = hour < 6 ? '夜深了' : hour < 11 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好';
    document.getElementById('greeting').textContent = `${hello}，${me.nickname || me.username}`;
    document.getElementById('today').textContent = new Date().toLocaleDateString('zh-CN', {
        year: 'numeric', month: 'long', day: 'numeric', weekday: 'long',
    });

    if (canAny('article:audit', 'user:manage')) {
        document.getElementById('global-stats').style.display = 'grid';
    }

    await Promise.all([loadStats(), loadRecent(), loadSuggestions()]);
}

async function loadStats() {
    try {
        const s = await api.get('/api/dashboard/stats');
        set('st-draft', s.myDraft);
        set('st-pending', s.myPending);
        set('st-rejected', s.myRejected);
        set('st-published', s.myPublished);
        set('st-pending-total', s.pendingTotal);
        set('st-published-total', s.publishedTotal);
        set('st-categories', s.categoryCount);
        set('st-users', s.userCount);
    } catch (e) {
        toast('工作台统计加载失败：' + e.message, 'err');
    }
}

function set(id, v) {
    const el = document.getElementById(id);
    if (el) el.textContent = v === null || v === undefined ? '—' : v;
}

async function loadRecent() {
    const tbody = document.getElementById('recent-body');
    try {
        const page = await api.get('/api/articles', {
            mine: 1, pageNum: 1, pageSize: 5, sort: 'updateTime,desc',
        });
        if (!page.records.length) {
            tbody.innerHTML = `<tr><td colspan="4"><div class="empty"><div class="big">◌</div>
                还没有稿件，<a href="/app/article-edit">写第一篇</a></div></td></tr>`;
            return;
        }
        tbody.innerHTML = page.records.map((a) => `
            <tr>
                <td class="ellipsis" style="max-width:260px">
                    <a href="/app/article-edit?id=${a.id}">${esc(a.title)}</a>
                </td>
                <td>${statusBadge(a.status)}</td>
                <td class="muted nowrap">${fmtTime(a.updateTime)}</td>
                <td class="actions">
                    ${a.status === 'DRAFT' || a.status === 'REJECTED'
            ? `<a class="btn-link" href="/app/article-edit?id=${a.id}">编辑</a>` : ''}
                    ${a.status === 'PENDING' ? `<a class="btn-link" href="/app/articles?status=PENDING&mine=1">查看</a>` : ''}
                    ${a.status === 'PUBLISHED' ? `<a class="btn-link" href="/portal/article/${a.id}" target="_blank" rel="noopener">前台</a>` : ''}
                </td>
            </tr>`).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="4"><div class="empty">加载失败：${esc(e.message)}</div></td></tr>`;
    }
}

async function loadSuggestions() {
    const host = document.getElementById('suggestions');
    try {
        const list = await api.get('/api/guide/suggestions');
        if (!list.length) {
            host.innerHTML = `<div class="empty" style="padding:28px 0"><div class="big">✓</div>
                暂时没有需要提醒的事，去<a href="/app/articles">文章列表</a>看看。</div>`;
            return;
        }
        host.innerHTML = list.map((s) => `
            <button class="sug" type="button" data-q="${esc(s.text)}">
                <span class="dot ${s.level === 'WARN' ? 'warn' : ''}"></span>
                <span class="txt">${esc(s.text)}</span>
                <span class="go">问助手 →</span>
            </button>`).join('');
        host.querySelectorAll('.sug').forEach((b) => {
            b.onclick = () => askGuide(b.dataset.q);
        });
    } catch (e) {
        host.innerHTML = `<div class="empty" style="padding:28px 0">引导建议加载失败：${esc(e.message)}</div>`;
    }
}
