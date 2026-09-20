/* ============================================================
   FAZZI MART - Login page script
   ============================================================ */

document.addEventListener('DOMContentLoaded', () => {
    if (isAuthed()) {
        window.location.href = 'home.html';
        return;
    }

    const form = document.getElementById('loginForm');
    const email = document.getElementById('email');
    const password = document.getElementById('password');
    const errorBox = document.getElementById('loginError');
    const loginBtn = document.getElementById('loginBtn');

    /* ---- Show / Hide password ---- */
    document.getElementById('togglePassword').addEventListener('click', () => {
        const icon = document.querySelector('#togglePassword i');
        const isHidden = password.type === 'password';
        password.type = isHidden ? 'text' : 'password';
        icon.className = isHidden ? 'bi bi-eye-slash' : 'bi bi-eye';
    });

    /* ---- Forgot password modal ---- */
    document.getElementById('forgotPasswordLink').addEventListener('click', (e) => {
        e.preventDefault();
        const modal = new bootstrap.Modal(document.getElementById('forgotModal'));
        modal.show();
    });

    function setError(input, errorEl, message) {
        input.classList.add('is-invalid');
        errorEl.textContent = message;
    }

    function clearErrors() {
        form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
        errorBox.classList.remove('show');
        errorBox.textContent = '';
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        clearErrors();

        let valid = true;
        const emailValue = email.value.trim();
        const passwordValue = password.value;

        if (!emailValue) {
            setError(email, document.getElementById('emailError'), 'Email is required');
            valid = false;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailValue)) {
            setError(email, document.getElementById('emailError'), 'Enter a valid email address');
            valid = false;
        }

        if (!passwordValue) {
            setError(password, document.getElementById('passwordError'), 'Password is required');
            valid = false;
        }

        if (!valid) return;

        loginBtn.disabled = true;
        loginBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Logging in...';

        try {
            const data = await apiFetch('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email: emailValue, password: passwordValue })
            });

            saveAuth(data.user, data.token);
            showToast('Welcome back, ' + data.user.name + '!');

            const params = new URLSearchParams(window.location.search);
            const redirect = params.get('redirect') || 'home.html';
            setTimeout(() => { window.location.href = redirect; }, 900);
        } catch (err) {
            errorBox.textContent = err.message;
            errorBox.classList.add('show');
            loginBtn.disabled = false;
            loginBtn.innerHTML = '<i class="bi bi-box-arrow-in-right"></i> Login';
        }
    });
});