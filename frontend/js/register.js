/* ============================================================
   FAZZI MART - Registration page script
   ============================================================ */

document.addEventListener('DOMContentLoaded', () => {
    if (isAuthed()) {
        window.location.href = 'home.html';
        return;
    }

    const form = document.getElementById('registerForm');
    const errorBox = document.getElementById('registerError');
    const registerBtn = document.getElementById('registerBtn');

    document.getElementById('togglePassword').addEventListener('click', () => {
        const pwd = document.getElementById('password');
        const icon = document.querySelector('#togglePassword i');
        const isHidden = pwd.type === 'password';
        pwd.type = isHidden ? 'text' : 'password';
        icon.className = isHidden ? 'bi bi-eye-slash' : 'bi bi-eye';
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

        const name = document.getElementById('name').value.trim();
        const phone = document.getElementById('phone').value.trim();
        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        let valid = true;

        if (!name) {
            setError(document.getElementById('name'), document.getElementById('nameError'), 'Name is required');
            valid = false;
        } else if (name.length < 2) {
            setError(document.getElementById('name'), document.getElementById('nameError'), 'Name must be at least 2 characters');
            valid = false;
        }

        if (!phone) {
            setError(document.getElementById('phone'), document.getElementById('phoneError'), 'Phone number is required');
            valid = false;
        } else if (!/^[0-9+\- ]{7,20}$/.test(phone)) {
            setError(document.getElementById('phone'), document.getElementById('phoneError'), 'Enter a valid phone number (digits only)');
            valid = false;
        }

        if (!email) {
            setError(document.getElementById('email'), document.getElementById('emailError'), 'Email is required');
            valid = false;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            setError(document.getElementById('email'), document.getElementById('emailError'), 'Enter a valid email address');
            valid = false;
        }

        if (!password) {
            setError(document.getElementById('password'), document.getElementById('passwordError'), 'Password is required');
            valid = false;
        } else if (password.length < 6) {
            setError(document.getElementById('password'), document.getElementById('passwordError'), 'Password must be at least 6 characters');
            valid = false;
        }

        if (!confirmPassword) {
            setError(document.getElementById('confirmPassword'), document.getElementById('confirmPasswordError'), 'Please confirm your password');
            valid = false;
        } else if (password !== confirmPassword) {
            setError(document.getElementById('confirmPassword'), document.getElementById('confirmPasswordError'), 'Passwords do not match');
            valid = false;
        }

        if (!valid) return;

        registerBtn.disabled = true;
        registerBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Creating account...';

        try {
            const data = await apiFetch('/auth/register', {
                method: 'POST',
                body: JSON.stringify({ name, phone, email, password, confirmPassword })
            });

            saveAuth(data.user, data.token);
            showToast('Registration successful. Welcome, ' + data.user.name + '!');
            setTimeout(() => { window.location.href = 'home.html'; }, 1100);
        } catch (err) {
            errorBox.textContent = err.message;
            errorBox.classList.add('show');
            registerBtn.disabled = false;
            registerBtn.innerHTML = '<i class="bi bi-person-plus"></i> Create Account';
        }
    });
});