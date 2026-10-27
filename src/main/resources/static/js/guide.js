/**
 * 站内引导智能体（前端部分）。
 *
 * 对话框里以卡片网格呈现"网站的每一个界面"：
 * 卡片内容来自后端 /api/guide/pages，那个接口的内容又是从 Controller 的
 * @GuideCard 注解反射出来的，所以这里看到的页面集合 = 系统真实存在的页面集合。
 *
 * 卡片上没有任何业务数据，点"打开页面"才是去真实页面、走真实权限校验。
 */

import { api } from './core/api.js';
import { esc, toast } from './core/ui.js';

let state = {
    me: null,
    opened: false,
    pages: null,      // GuideCardVO[]
    busy: false,
    pendingGuideId: new URLSearchParams(location.search).get('guide'),
    // 高亮和"你刚才看的是哪一页"各用各的标记：高亮在页面加载后自动发生，
    // 问候要等用户真的打开对话框。共用一个标记会导致先发生的一方把另一方吃掉。
    pendingHighlightId: new URLSearchParams(location.search).get('guide'),
};

let refs = {};

export function initGuide(me) {
    state.me = me;
    if (!(me.permissions || []).includes('guide:use')) return;
    buildBubble();
    // 高亮目标（标题输入框、关键词输入框）都在模板里，DOM 一就绪就存在；
    // 等 900ms 是给页面自己的数据加载让路，避免高亮被随后的渲染覆盖掉。
    setTimeout(tryHighlight, 900);
}

/* ---------------- 骨架 ---------------- */

function buildBubble() {
    const bubble = document.createElement('button');
    bubble.className = 'guide-bubble';
    bubble.type = 'button';
    bubble.setAttribute('aria-label', '打开站内引导');
    bubble.innerHTML = `<span class="ping"></span><span>✦</span><span class="tip">不知道怎么用？问我</span>`;
    bubble.onclick = openPanel;
    document.body.appendChild(bubble);
    refs.bubble = bubble;
}

function buildPanel() {
    const panel = document.createElement('div');
    panel.className = 'guide-panel';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', '站内引导');
    panel.innerHTML = `
        <div class="guide-head">
            <div class="g-avatar">✦</div>
            <div>
                <div class="t">站内导航</div>
                <div class="s">只讲这个系统里有的页面</div>
            </div>
            <button class="x" type="button" data-act="llm" title="模型设置">⚙</button>
            <button class="x" type="button" data-act="all" title="全部页面">▦</button>
            <button class="x" type="button" data-act="close" title="收起">×</button>
        </div>
        <div class="guide-llm" data-role="llm" hidden>
            <div class="llm-top">
                <strong>模型设置</strong>
                <span class="llm-badge" data-role="llm-badge">规则模式</span>
            </div>
            <div class="llm-status" data-role="llm-status">正在读取状态…</div>
            <div class="llm-row">
                <input type="password" data-role="llm-key" placeholder="粘贴你的 DeepSeek API Key"
                       autocomplete="off" spellcheck="false">
                <button class="btn btn-sm btn-primary" type="button" data-role="llm-save">保存</button>
            </div>
            <div class="llm-row">
                <button class="qchip" type="button" data-role="llm-test">测试连接</button>
                <button class="qchip" type="button" data-role="llm-clear">清除</button>
                <span class="src" data-role="llm-msg"></span>
            </div>
            <div class="llm-note">密钥只保存在本次登录的服务器会话里，不写数据库、不在日志里留明文；退出登录即失效。不填也能用——那是规则模式。</div>
        </div>
        <div class="guide-scroll" data-role="scroll"></div>
        <div class="guide-quick" data-role="quick"></div>
        <form class="guide-input" data-role="form">
            <input type="text" maxlength="200" placeholder="比如：文章被驳回了怎么改？" data-role="input" autocomplete="off">
            <button type="submit" title="发送">→</button>
        </form>`;

    panel.querySelector('[data-act=close]').onclick = closePanel;
    panel.querySelector('[data-act=llm]').onclick = toggleLlmPanel;
    panel.querySelector('[data-act=all]').onclick = () => {
        addBot('这是系统里的全部页面：');
        showAllCards();
    };
    panel.querySelector('[data-role=llm-save]').onclick = saveLlmKey;
    panel.querySelector('[data-role=llm-test]').onclick = testLlm;
    panel.querySelector('[data-role=llm-clear]').onclick = clearLlmKey;
    panel.querySelector('[data-role=llm-key]').onkeydown = (e) => {
        if (e.key === 'Enter') { e.preventDefault(); saveLlmKey(); }
    };
    const form = panel.querySelector('[data-role=form]');
    form.onsubmit = (e) => {
        e.preventDefault();
        const input = panel.querySelector('[data-role=input]');
        const text = input.value.trim();
        if (!text) return;
        input.value = '';
        send(text);
    };

    document.body.appendChild(panel);
    refs.panel = panel;
    refs.scroll = panel.querySelector('[data-role=scroll]');
    refs.quick = panel.querySelector('[data-role=quick]');
    refs.input = panel.querySelector('[data-role=input]');
    refs.llm = panel.querySelector('[data-role=llm]');
}

async function openPanel() {
    if (!refs.panel) {
        buildPanel();
        welcome();
        loadSuggestions();
    }
    refs.panel.style.display = 'flex';
    if (refs.bubble) refs.bubble.style.display = 'none';
    state.opened = true;
    refs.input.focus();

    if (state.pendingGuideId) {
        const card = (await pages()).find((c) => c.id === state.pendingGuideId);
        if (card) {
            addBot(`你刚才打开的页面是「${card.title}」，需要我讲讲怎么用吗？`);
            renderCards(refs.scroll.lastElementChild, [card]);
            state.pendingGuideId = null;
        }
    }
}

function closePanel() {
    if (!refs.panel) return;
    refs.panel.style.display = 'none';
    if (refs.bubble) refs.bubble.style.display = 'grid';
    state.opened = false;
}

/* ---------------- 模型设置 ---------------- */

async function toggleLlmPanel() {
    const box = refs.llm;
    box.hidden = !box.hidden;
    if (!box.hidden) await refreshLlmStatus();
}

async function refreshLlmStatus() {
    try {
        renderLlmStatus(await api.get('/api/guide/llm'));
    } catch (e) {
        refs.panel.querySelector('[data-role=llm-status]').textContent = '读取状态失败：' + e.message;
    }
}

function renderLlmStatus(s) {
    const badge = refs.panel.querySelector('[data-role=llm-badge]');
    const text = refs.panel.querySelector('[data-role=llm-status]');
    const model = s.model || 'DeepSeek';
    if (!s.enabled) {
        badge.textContent = '通道已关闭';
        badge.className = 'llm-badge off';
        text.textContent = '服务端关闭了模型通道，当前只能按关键词规则匹配。';
        return;
    }
    if (s.configured) {
        badge.textContent = '模型已接入';
        badge.className = 'llm-badge on';
        text.textContent = s.source === 'USER'
            ? `已接入 ${model} · 密钥 ${s.keyHint} · 仅本次登录有效`
            : `已接入 ${model} · 使用服务端配置的密钥 ${s.keyHint}`;
    } else {
        badge.textContent = '规则模式';
        badge.className = 'llm-badge';
        text.textContent = '还没有密钥：当前按关键词规则匹配。填上 key 后，规则没命中的问题再交给模型理解。';
    }
}

async function saveLlmKey() {
    const input = refs.panel.querySelector('[data-role=llm-key]');
    const msg = refs.panel.querySelector('[data-role=llm-msg]');
    const key = input.value.trim();
    if (!key) {
        toast('先粘贴 API Key', 'err');
        return;
    }
    try {
        const s = await api.put('/api/guide/llm', { apiKey: key });
        input.value = '';
        msg.textContent = '';
        renderLlmStatus(s);
        toast('密钥已保存，本次登录有效', 'ok');
    } catch (e) {
        toast(e.message, 'err');
    }
}

async function testLlm() {
    const btn = refs.panel.querySelector('[data-role=llm-test]');
    const msg = refs.panel.querySelector('[data-role=llm-msg]');
    btn.disabled = true;
    msg.textContent = '测试中…';
    msg.style.color = '';
    try {
        const r = await api.post('/api/guide/llm/test');
        msg.textContent = r.ok ? `✓ ${r.message}（${r.latencyMs} ms）` : `✗ ${r.message}`;
        msg.style.color = r.ok ? 'var(--ok, #16a34a)' : 'var(--warn)';
    } catch (e) {
        msg.textContent = '✗ ' + e.message;
        msg.style.color = 'var(--warn)';
    } finally {
        btn.disabled = false;
    }
}

async function clearLlmKey() {
    const msg = refs.panel.querySelector('[data-role=llm-msg]');
    try {
        renderLlmStatus(await api.del('/api/guide/llm'));
        msg.textContent = '已清除';
        msg.style.color = '';
    } catch (e) {
        toast(e.message, 'err');
    }
}

/** 供其它页面调用：打开对话框并直接把问题发出去 */
export async function askGuide(text) {
    if (!refs.panel) {
        buildPanel();
        welcome();
        loadSuggestions();
    }
    await openPanel();
    if (text) send(text);
}

/* ---------------- 欢迎与建议 ---------------- */

function welcome() {
    const roles = (state.me.roleCodes || []).join('、') || '访客';
    addBot(`你好，${esc(state.me.nickname || state.me.username)}（${esc(roles)}）。
我可以带你看这个系统里的每个页面：这一页是干什么的、按哪几步操作、哪里容易踩坑。
先挑一张卡片，或者直接问我一件事。`);
    showAllCards();
}

async function loadSuggestions() {
    try {
        const list = await api.get('/api/guide/suggestions');
        if (!list || !list.length) return;
        refs.quick.innerHTML = '';
        list.slice(0, 4).forEach((s) => {
            const b = document.createElement('button');
            b.className = 'qchip';
            b.type = 'button';
            b.textContent = s.text;
            if (s.level === 'WARN') b.style.color = 'var(--warn)';
            b.onclick = () => send(s.text);
            refs.quick.appendChild(b);
        });
    } catch (e) {
        // 建议是锦上添花，失败就留空
    }
}

/* ---------------- 消息 ---------------- */

function addMsg(role, html) {
    const wrap = document.createElement('div');
    wrap.className = 'guide-msg ' + role;
    const bubble = document.createElement('div');
    bubble.className = 'bubble-msg';
    bubble.innerHTML = html;
    wrap.appendChild(bubble);
    refs.scroll.appendChild(wrap);
    refs.scroll.scrollTop = refs.scroll.scrollHeight;
    return wrap;
}

function addBot(text) {
    return addMsg('bot', text.replace(/\n/g, '<br>'));
}

function addMe(text) {
    return addMsg('me', esc(text));
}

async function send(text) {
    if (state.busy) return;
    state.busy = true;
    refs.input.disabled = true;
    addMe(text);
    const pending = addBot('<span class="spinner"></span>正在这个系统里找…');

    try {
        const res = await api.post('/api/guide/chat', { message: text });
        pending.remove();
        const srcLabel = res.source === 'LLM' ? '模型理解（DeepSeek）' : res.source === 'EMPTY' ? '未命中' : '规则匹配';
        const host = addBot(esc(res.answer || ''));
        const src = document.createElement('div');
        src.className = 'src';
        src.textContent = `来源：${srcLabel}`;
        host.querySelector('.bubble-msg').appendChild(src);

        if (res.cards && res.cards.length) {
            renderCards(host, res.cards);
        } else if (res.notFound) {
            const btn = document.createElement('button');
            btn.className = 'qchip';
            btn.type = 'button';
            btn.textContent = '看看系统里有哪些页面';
            btn.onclick = () => { addBot('系统里现在有这些页面：'); showAllCards(); };
            host.querySelector('.bubble-msg').appendChild(btn);
        }
    } catch (e) {
        pending.remove();
        addBot(`没查成：${esc(e.message)}${e.traceId ? `<br><span class="src">traceId ${esc(e.traceId)}</span>` : ''}`);
    } finally {
        state.busy = false;
        refs.input.disabled = false;
        refs.input.focus();
    }
}

/* ---------------- 卡片 ---------------- */

async function pages() {
    if (!state.pages) state.pages = await api.get('/api/guide/pages');
    return state.pages;
}

async function showAllCards() {
    const host = refs.scroll.lastElementChild;
    if (!host) return;
    try {
        renderCards(host, await pages());
    } catch (e) {
        toast(e.message, 'err');
    }
}

function renderCards(host, cards) {
    // 卡片要挂进气泡内部：host 是 flex 容器，直接当子节点会被排成气泡右侧的一列
    const panel = host.querySelector('.bubble-msg') || host;
    panel.classList.add('has-cards');
    const grid = document.createElement('div');
    grid.className = 'guide-cards';
    const detail = document.createElement('div');
    detail.className = 'g-detail';
    detail.style.display = 'none';

    cards.forEach((c) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'g-card' + (c.accessible ? '' : ' locked');
        btn.innerHTML = `
            <div class="g-group">${esc(c.group || '页面')}</div>
            <div class="g-title">${esc(c.title)}</div>
            <div class="g-sum">${esc(c.summary || c.denyHint || '')}</div>
            <div class="g-more">${c.accessible ? '查看步骤 →' : '无权访问'}</div>`;
        btn.onclick = () => {
            if (!c.accessible) {
                toast(c.denyHint || '当前角色无权访问这个页面', 'err');
                return;
            }
            const same = detail.dataset.openId === c.id && detail.style.display !== 'none';
            grid.querySelectorAll('.g-card').forEach((n) => n.style.borderColor = '');
            if (same) {
                detail.style.display = 'none';
                detail.dataset.openId = '';
                return;
            }
            detail.dataset.openId = c.id;
            btn.style.borderColor = 'var(--brand)';
            detail.innerHTML = cardDetailHtml(c);
            detail.style.display = 'block';
            detail.querySelector('[data-open]').onclick = () => {
                const url = c.highlight ? `${c.route}?guide=${encodeURIComponent(c.id)}` : c.route;
                location.href = url;
            };
            detail.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        };
        grid.appendChild(btn);
    });

    panel.appendChild(grid);
    panel.appendChild(detail);
    refs.scroll.scrollTop = refs.scroll.scrollHeight;
}

function cardDetailHtml(c) {
    const steps = (c.steps || []).map((s) => `<li>${esc(s)}</li>`).join('');
    const tips = (c.tips || []).map((s) => `<li>${esc(s)}</li>`).join('');
    return `
        <h4>${esc(c.title)}</h4>
        <div class="sum">${esc(c.summary || '')}</div>
        ${steps ? `<div class="lbl">怎么做</div><ol>${steps}</ol>` : ''}
        ${tips ? `<div class="lbl">注意</div><div class="tipbox"><ul style="margin:0;padding-left:16px">${tips}</ul></div>` : ''}
        ${c.apiPath ? `<div class="api">${esc(c.httpMethod || '')} ${esc(c.apiPath)}</div>` : ''}
        <div class="acts">
            <button class="btn btn-sm btn-primary" type="button" data-open>打开这个页面</button>
            <span class="src" style="align-self:center">页面地址 ${esc(c.route)}</span>
        </div>`;
}

/* ---------------- 从 URL 高亮目标元素 ---------------- */

async function tryHighlight() {
    const id = state.pendingHighlightId;
    if (!id) return;
    try {
        const card = (await pages()).find((c) => c.id === id);
        if (!card || !card.highlight) { state.pendingHighlightId = null; return; }
        const el = document.querySelector(card.highlight);
        if (!el) return;
        state.pendingHighlightId = null;
        el.classList.add('guide-highlight');
        el.scrollIntoView({ block: 'center', behavior: 'smooth' });
        if (el.focus) el.focus({ preventScroll: true });
        setTimeout(() => el.classList.remove('guide-highlight'), 5200);
    } catch (e) {
        // 高亮失败不影响页面
    }
}
