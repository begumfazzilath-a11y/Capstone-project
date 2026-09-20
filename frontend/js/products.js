/* ============================================================
   FAZZI MART - Products page script (search / filter / sort)
   ============================================================ */

const urlParams = new URLSearchParams(window.location.search);

const state = {
    query: '',
    category: urlParams.get('category') || 'All',
    sort: urlParams.get('sort') || 'default'
};

document.addEventListener('DOMContentLoaded', () => {
    requireAuth();
    renderNavbar('products');
    renderFooter();

    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');
    const sortSelect = document.getElementById('sortSelect');

    renderCategoryPills();
    sortSelect.value = state.sort;
    searchInput.value = state.query;
    fetchProducts();

    searchBtn.addEventListener('click', () => {
        state.query = searchInput.value.trim();
        fetchProducts();
    });

    searchInput.addEventListener('keyup', debounce((e) => {
        state.query = searchInput.value.trim();
        fetchProducts();
    }, 450));

    sortSelect.addEventListener('change', () => {
        state.sort = sortSelect.value;
        fetchProducts();
    });
});

function renderCategoryPills() {
    const pills = ['All', ...CATEGORIES];
    const container = document.getElementById('categoryPills');
    container.innerHTML = pills.map(cat => `
        <button class="btn btn-sm rounded-pill ${state.category === cat ? 'btn-fm' : 'btn-outline-secondary'}"
                data-cat="${cat}">${cat}</button>`).join('');

    container.querySelectorAll('[data-cat]').forEach(btn => {
        btn.addEventListener('click', () => {
            state.category = btn.getAttribute('data-cat');
            renderCategoryPills();
            fetchProducts();
        });
    });
}

function syncUrl() {
    const params = new URLSearchParams();
    if (state.query) params.set('query', state.query);
    if (state.category !== 'All') params.set('category', state.category);
    if (state.sort !== 'default') params.set('sort', state.sort);
    const qs = params.toString();
    history.replaceState(null, '', 'products.html' + (qs ? '?' + qs : ''));
}

async function fetchProducts() {
    syncUrl();
    const grid = document.getElementById('productGrid');
    const info = document.getElementById('resultsInfo');
    grid.innerHTML = spinnerHTML().replace('Loading...', 'Loading products...');

    const params = new URLSearchParams();
    if (state.query) params.set('query', state.query);
    if (state.category !== 'All') params.set('category', state.category);
    if (state.sort !== 'default') params.set('sort', state.sort);
    const qs = params.toString();

    try {
        const products = await apiFetch('/products' + (qs ? '?' + qs : ''));
        info.textContent = 'Showing ' + products.length + ' product' + (products.length === 1 ? '' : 's')
            + (state.category !== 'All' ? ' in "' + state.category + '"' : '');

        if (!products.length) {
            grid.innerHTML = emptyStateHTML('🔍', 'No products found',
                'Try a different search term or category.',
                '<a href="products.html" class="btn btn-fm">Clear Filters</a>');
            return;
        }
        grid.innerHTML = products.map(productCard).join('');
        bindAddToCart();
    } catch (err) {
        if (err.status === 401) {
            window.location.href = 'index.html';
        } else {
            info.textContent = '';
            grid.innerHTML = emptyStateHTML('😿', 'Could not load products', escapeHtml(err.message),
                '<a href="products.html" class="btn btn-fm">Retry</a>');
        }
    }
}

function bindAddToCart() {
    document.querySelectorAll('[data-add-cart]').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.preventDefault();
            const productId = btn.getAttribute('data-add-cart');
            btn.disabled = true;
            try {
                await addToCart(parseInt(productId, 10), 1);
                showToast('Added to cart! 🛒');
            } catch (err) {
                showToast(err.message, 'error');
            } finally {
                btn.disabled = false;
            }
        });
    });
}

function debounce(fn, delay) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
}