/* ============================================================
   FAZZI MART - Home page script
   ============================================================ */

document.addEventListener('DOMContentLoaded', async () => {
    requireAuth();
    renderNavbar('home');
    renderFooter();

    const container = document.getElementById('featuredProducts');

    try {
        const products = await apiFetch('/products/featured');
        if (!products.length) {
            container.innerHTML = emptyStateHTML('🐾', 'No products yet', 'Products will appear here soon.');
            return;
        }
        container.innerHTML = products.map(productCard).join('');
        bindAddToCart();
    } catch (err) {
        if (err.status === 401) {
            window.location.href = 'index.html';
        } else {
            container.innerHTML = emptyStateHTML('😿', 'Could not load products',
                escapeHtml(err.message),
                '<a href="products.html" class="btn btn-fm">Browse Products</a>');
        }
    }
});

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
                btn.blur();
            }
        });
    });
}