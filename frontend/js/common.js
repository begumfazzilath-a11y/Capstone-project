/* ============================================================
   FAZZI MART - Shared frontend helpers (common.js)
   Load this file on every page BEFORE the page script.
   ============================================================ */

// Relative on deployed hosts (Render/Railway serve app + API from one URL,
// so /api must stay same-origin). Absolute localhost:9090 only for local dev.
const API_BASE = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
    ? 'http://localhost:9090/api'
    : '/api';

const TOKEN_KEY = 'fazzi_token';
const USER_KEY = 'fazzi_user';
const CART_COUNT_KEY = 'fazzi_cart_count';

const CATEGORIES = ['Pet Food', 'Pet Products', 'Pet Accessories', 'Pet Toys'];

/* ---------------- Auth helpers ---------------- */

function getToken() {
    return localStorage.getItem(TOKEN_KEY) || '';
}

function getStoredUser() {
    try {
        return JSON.parse(localStorage.getItem(USER_KEY));
    } catch (e) {
        return null;
    }
}

function saveAuth(user, token) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    if (!localStorage.getItem(CART_COUNT_KEY)) localStorage.setItem(CART_COUNT_KEY, '0');
}

function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(CART_COUNT_KEY);
}

function isAuthed() {
    return !!getToken();
}

function requireAuth() {
    if (!isAuthed()) {
        window.location.href = 'index.html';
    }
}

/* ---------------- Cart badge helpers ---------------- */

function setCartCount(n) {
    localStorage.setItem(CART_COUNT_KEY, parseInt(n || 0, 10));
}

function getCartCount() {
    return parseInt(localStorage.getItem(CART_COUNT_KEY) || '0', 10);
}

function updateNavCartBadge() {
    const badge = document.getElementById('navCartCount');
    if (badge) badge.textContent = getCartCount();
}

/* ---------------- API fetch helper ---------------- */

async function apiFetch(url, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch(API_BASE + url, { ...options, headers });

    let data = null;
    const text = await res.text();
    if (text) {
        try { data = JSON.parse(text); } catch (e) { data = text; }
    }

    if (!res.ok) {
        const message = data && data.message ? data.message : ('Request failed (HTTP ' + res.status + ')');
        const err = new Error(message);
        err.status = res.status;
        throw err;
    }
    return data;
}

/* ---------------- Formatting helpers ---------------- */

function formatPrice(amount) {
    return 'PHP ' + Number(amount || 0).toLocaleString('en-PH', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function formatDate(value) {
    if (!value) return '';
    const d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return d.toLocaleDateString('en-PH', { year: 'numeric', month: 'short', day: 'numeric' }) +
        ' ' + d.toLocaleTimeString('en-PH', { hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function renderStars(rating) {
    const r = Math.round((Number(rating) || 0) * 2) / 2;
    let html = '';
    for (let i = 1; i <= 5; i++) {
        if (r >= i) html += '<i class="bi bi-star-fill text-warning"></i>';
        else if (r >= i - 0.5) html += '<i class="bi bi-star-half text-warning"></i>';
        else html += '<i class="bi bi-star text-warning"></i>';
    }
    html += ' <span class="small text-muted">(' + (Number(rating) || 0).toFixed(1) + ')</span>';
    return html;
}

/* ---------------- Product card template ---------------- */

function productCard(p) {
    const inStock = p.stock > 0;
    const stockBadge = inStock
        ? '<span class="badge bg-success">In Stock</span>'
        : '<span class="badge bg-danger">Out of Stock</span>';

    return `
    <div class="col-6 col-md-4 col-xl-3 mb-4">
      <div class="card product-card h-100">
        <a href="product-details.html?id=${p.id}" class="product-img-link">
          <img src="${escapeHtml(p.imageUrl)}" class="product-img" alt="${escapeHtml(p.name)}" loading="lazy"
               onerror="this.onerror=null;this.src='images/dog-food.svg';">
        </a>
        <span class="category-badge">${escapeHtml(p.category)}</span>
        <div class="card-body d-flex flex-column p-3">
          <h5 class="card-title">${escapeHtml(p.name)}</h5>
          <div class="mb-1">${renderStars(p.rating)}</div>
          <div class="d-flex align-items-center justify-content-between mb-1">
            <span class="price">${formatPrice(p.price)}</span>
            ${stockBadge}
          </div>
          <p class="text-muted stock-text mb-3">${inStock ? p.stock + ' left in stock' : 'Currently unavailable'}</p>
          <div class="mt-auto d-flex gap-2">
            <button class="btn btn-fm flex-fill btn-sm" data-add-cart="${p.id}" ${inStock ? '' : 'disabled'}>
              ${inStock ? '<i class="bi bi-cart-plus"></i> Add to Cart' : 'Out of Stock'}
            </button>
            <a href="product-details.html?id=${p.id}" class="btn btn-outline-fm flex-fill btn-sm">View Details</a>
          </div>
        </div>
      </div>
    </div>`;
}

/* ---------------- Add to cart helper ---------------- */

async function addToCart(productId, quantity = 1) {
    if (!isAuthed()) {
        showToast('Please login first', 'error');
        setTimeout(() => { window.location.href = 'index.html?redirect=home.html'; }, 1200);
        return null;
    }
    const data = await apiFetch('/cart/add', {
        method: 'POST',
        body: JSON.stringify({ productId, quantity })
    });
    setCartCount(data.itemCount);
    updateNavCartBadge();
    return data;
}

/* ---------------- Toast notifications ---------------- */

function showToast(message, type = 'success') {
    const existing = document.querySelector('.fm-toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = 'fm-toast ' + (type === 'error' ? 'error' : 'success');
    toast.innerHTML = (type === 'error' ? '⚠️ ' : '✅ ') + escapeHtml(message);
    document.body.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add('show'));
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 350);
    }, 3200);
}

/* ---------------- Navbar ---------------- */

function renderNavbar(active) {
    const nav = document.getElementById('navbar');
    if (!nav) return;

    const dropdown = `
      <li class="nav-item dropdown">
        <a class="nav-link dropdown-toggle" href="#" id="categoriesDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">Categories</a>
        <ul class="dropdown-menu" aria-labelledby="categoriesDropdown">
          <li><a class="dropdown-item" href="products.html?category=Pet%20Food"><i class="bi bi-basket me-1"></i>Pet Food</a></li>
          <li><a class="dropdown-item" href="products.html?category=Pet%20Products"><i class="bi bi-water me-1"></i>Pet Products</a></li>
          <li><a class="dropdown-item" href="products.html?category=Pet%20Accessories"><i class="bi bi-handbag me-1"></i>Pet Accessories</a></li>
          <li><a class="dropdown-item" href="products.html?category=Pet%20Toys"><i class="bi bi-stars me-1"></i>Pet Toys</a></li>
        </ul>
      </li>`;

    nav.innerHTML = `
      <nav class="navbar navbar-expand-lg fm-nav fixed-top">
        <div class="container">
          <a class="navbar-brand fw-bold" href="home.html">🐾 FAZZI MART</a>
          <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#fmNav" aria-controls="fmNav" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
          </button>
          <div class="collapse navbar-collapse" id="fmNav">
            <ul class="navbar-nav ms-auto align-items-lg-center">
              <li class="nav-item">
                <a class="nav-link ${active === 'home' ? 'active' : ''}" href="home.html">Home</a>
              </li>
              <li class="nav-item">
                <a class="nav-link ${active === 'products' ? 'active' : ''}" href="products.html">Products</a>
              </li>
              ${dropdown}
              <li class="nav-item">
                <a class="nav-link ${active === 'cart' ? 'active' : ''}" href="cart.html">
                  <i class="bi bi-cart3"></i> Cart
                  <span class="nav-cart-badge" id="navCartCount">${getCartCount()}</span>
                </a>
              </li>
              <li class="nav-item">
                <a class="nav-link ${active === 'account' ? 'active' : ''}" href="account.html">
                  <i class="bi bi-person-circle"></i> My Account</a>
              </li>
              <li class="nav-item ms-lg-2 mt-2 mt-lg-0">
                <button class="btn btn-outline-light btn-sm px-3" id="logoutBtn">
                  <i class="bi bi-box-arrow-right"></i> Logout</button>
              </li>
            </ul>
          </div>
        </div>
      </nav>`;

    updateNavCartBadge();

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            clearAuth();
            showToast('You have been logged out', 'success');
            setTimeout(() => { window.location.href = 'index.html'; }, 800);
        });
    }
}

/* ---------------- Footer ---------------- */

function renderFooter() {
    const footer = document.getElementById('pageFooter');
    if (!footer) return;
    footer.innerHTML = `
      <footer class="footer">
        <div class="container">
          <div class="row gy-4">
            <div class="col-lg-4">
              <h5 class="fw-bold">🐾 FAZZI MART</h5>
              <p class="mb-0">Happy Pets. Better Care. Easy Shopping.</p>
              <p class="mt-2 small mb-0">Your trusted online pet shop for food, products, accessories and toys.</p>
            </div>
            <div class="col-lg-4">
              <h6 class="fw-bold">Quick Links</h6>
              <ul class="list-unstyled small mb-0">
                <li><a href="home.html">Home</a></li>
                <li><a href="products.html">Products</a></li>
                <li><a href="cart.html">Shopping Cart</a></li>
                <li><a href="account.html">My Account</a></li>
              </ul>
            </div>
            <div class="col-lg-4">
              <h6 class="fw-bold">Contact Us</h6>
              <ul class="list-unstyled small mb-0">
                <li><i class="bi bi-envelope me-2"></i>support@fazzimart.com</li>
                <li><i class="bi bi-telephone me-2"></i>+63 917 123 4567</li>
                <li><i class="bi bi-geo-alt me-2"></i>123 Pet Street, Manila, Philippines</li>
              </ul>
            </div>
          </div>
          <hr>
          <p class="text-center small mb-0">© 2026 FAZZI MART. All rights reserved. Made with ❤️ for happy pets.</p>
        </div>
      </footer>`;
}

/* ---------------- Small UI helpers ---------------- */

function spinnerHTML() {
    return `<div class="spinner-wrap">
      <div class="spinner-border text-fm" role="status"></div>
      <p class="mt-2 text-muted">Loading...</p>
    </div>`;
}

function emptyStateHTML(icon, title, subtitle, btnHtml) {
    return `<div class="empty-state">
      <div class="empty-icon">${icon}</div>
      <h5 class="fw-bold mt-3">${title}</h5>
      <p class="mb-3">${subtitle}</p>
      ${btnHtml || ''}
    </div>`;
}