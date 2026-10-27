import { api } from '../core/api.js';

const form = document.getElementById('login-form');
const errBox = document.getElementById('login-error');
const btn = document.getElementById('btn-login');

const prefill = new URLSearchParams(location.search).get('u');
if (prefill) {
    document.getElementById('username').value = prefill;
    document.getElementById('password').focus();
}

form.onsubmit = async (e) => {
    e.preventDefault();
    errBox.style.display = 'none';
    btn.disabled = true;
    btn.textContent = '正在登录…';

    try {
        await api.post('/api/auth/login', {
            username: document.getElementById('username').value.trim(),
            password: document.getElementById('password').value,
        });
        const next = new URLSearchParams(location.search).get('next');
        // 只接受站内绝对路径：'//evil.com' 是协议相对 URL，同样会跳出去
        const safe = next && next.startsWith('/') && !next.startsWith('//');
        location.replace(safe ? next : '/app/dashboard');
    } catch (err) {
        errBox.textContent = err.message;
        errBox.style.display = 'block';
        btn.disabled = false;
        btn.textContent = '进入系统';
        document.getElementById('password').select();
    }
};
