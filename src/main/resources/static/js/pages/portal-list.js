import { api } from '../core/api.js';
import { esc, fmtTime, renderPager } from '../core/ui.js';

const state = { pageNum: 1, categoryId: '' };

async function main() {
    await loadCategories();
    await loadPosts();
}

async function loadCategories() {
    try {
        const roots = await api.get('/api/portal/categories');
        const flat = [];
        const walk = (nodes) => nodes.forEach((n) => {
            flat.push(n);
            if (n.children && n.children.length) walk(n.children);
        });
        walk(roots);

        const host = document.getElementById('cats');
        host.innerHTML = `<button class="cat-pill active" data-id="">全部</button>` + flat.map((c) =>
            `<button class="cat-pill" data-id="${c.id}">${esc(c.name)}</button>`).join('');
        host.onclick = (e) => {
            const b = e.target.closest('[data-id]');
            if (!b) return;
            host.querySelectorAll('.cat-pill').forEach((x) => x.classList.remove('active'));
            b.classList.add('active');
            state.categoryId = b.dataset.id;
            state.pageNum = 1;
            loadPosts();
        };
    } catch (e) {
        // 分类拿不到就只显示全部
    }
}

async function loadPosts() {
    const grid = document.getElementById('post-grid');
    grid.innerHTML = `<div class="loading" style="grid-column:1/-1"><span class="spinner"></span>加载中</div>`;
    try {
        const page = await api.get('/api/portal/articles', {
            pageNum: state.pageNum,
            pageSize: 12,
            categoryId: state.categoryId,
            sort: 'publishTime,desc',
        });
        if (!page.records.length) {
            grid.innerHTML = `<div class="empty" style="grid-column:1/-1"><div class="big">◌</div>这个分类下还没有已发布的文章</div>`;
            document.getElementById('pager').innerHTML = '';
            return;
        }
        grid.innerHTML = page.records.map((a) => `
            <a class="post" href="/portal/article/${a.id}">
                <div class="cat">${esc(a.categoryName || '未分类')}</div>
                <h3>${esc(a.title)}</h3>
                <p>${esc(a.summary || '（没有摘要）')}</p>
                <div class="meta">
                    <span>${esc(a.authorName || '佚名')}</span>
                    <span>${fmtTime(a.publishTime || a.createTime)}</span>
                    <span>阅读 ${a.viewCount ?? 0}</span>
                </div>
            </a>`).join('');
        renderPager(document.getElementById('pager'), page, (n) => { state.pageNum = n; loadPosts(); window.scrollTo({ top: 0, behavior: 'smooth' }); });
    } catch (e) {
        grid.innerHTML = `<div class="empty" style="grid-column:1/-1">加载失败：${esc(e.message)}</div>`;
    }
}

main();
