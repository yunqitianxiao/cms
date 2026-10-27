/**
 * 统一请求层。
 *
 * 后端所有响应都是 Result{code,message,data,traceId}，这里只做两件事：
 * 1. code === 0 时把 data 直接返回，页面里不再层层解包；
 * 2. 未登录（40100）统一跳登录页，避免每个页面各写一遍。
 */

export class ApiError extends Error {
    constructor(code, message, traceId) {
        super(message || '请求失败');
        this.name = 'ApiError';
        this.code = code;
        this.traceId = traceId;
    }
}

function toQuery(params) {
    if (!params) return '';
    const sp = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || v === null || v === '') continue;
        sp.append(k, v);
    }
    const s = sp.toString();
    return s ? '?' + s : '';
}

function gotoLogin() {
    const here = location.pathname + location.search;
    if (location.pathname === '/login') return;
    location.replace('/login?next=' + encodeURIComponent(here));
}

async function request(method, url, body) {
    const opts = { method, credentials: 'same-origin', headers: {} };
    if (body !== undefined) {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(body);
    }

    let res;
    try {
        res = await fetch(url, opts);
    } catch (e) {
        throw new ApiError(-1, '网络异常：连不上后端服务');
    }

    let payload = null;
    try {
        payload = await res.json();
    } catch (e) {
        // 401 时拦截器返回的是 JSON，这里拿不到只可能是服务端异常页
    }

    if (!payload) {
        if (res.status === 401) gotoLogin();
        throw new ApiError(res.status, `服务器返回了非 JSON 响应（HTTP ${res.status}）`);
    }

    if (payload.code === 0) return payload.data;

    if (payload.code === 40100) gotoLogin();
    throw new ApiError(payload.code, payload.message, payload.traceId);
}

export const api = {
    get: (url, params) => request('GET', url + toQuery(params)),
    post: (url, body) => request('POST', url, body === undefined ? {} : body),
    put: (url, body) => request('PUT', url, body === undefined ? {} : body),
    del: (url) => request('DELETE', url),
};
