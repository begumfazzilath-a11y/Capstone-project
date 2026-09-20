/* ============================================================
   FAZZI MART - Shopping cart page script
   ============================================================ */

document.addEventListener('DOMContentLoaded', () => {
    requireAuth();
    renderNavbar('cart');
    renderFooter();
    loadCart();
});

let cartData = null;

async function loadCart() {
    try {
        cartData = await apiFetch('/cart');
        setCartCount(cartData.itemCount);
        updateNavCartBadge();
        renderCart();
    } catch (err) {
        if (err.status === 401) {
            window.location.href = 'index.html';
        } else {
            container.innerHTML = emptyStateHTML('😿', 'Could not load your cart', escapeHtml(err.message),
                '<a href="products.html" class="btn btn-fm">Browse Products</a>');
        }
    }
}

function renderCart() {
    const container = document.getElementById('cartItems');

    if (!cartData.items.length) {
        container.innerHTML = emptyStateHTML('🛒', 'Your cart is empty',
            'Looks like you have not added anything yet.',
            '<a href="products.html" class="btn btn-fm">Start Shopping</a>');
        document.getElementById('cartSummary').innerHTML = '';
        syncBadge();
        return;
    }

    container.innerHTML = cartData.items.map(item => `
      <div class="cart-item-card p-3 mb-3">
        <div class="row g-3 align-items-center">
          <div class="col-4 col-sm-3">
            <a href="product-details.html?id=${item.productId}">
              <img src="${escapeHtml(item.imageUrl)}" alt="${escapeHtml(item.name)}" class="cart-item-img"
                   onerror="this.onerror=null;this.src='images/dog-food.svg';">
            </a>
          </div>
          <div class="col-8 col-sm-9">
            <div class="d-flex flex-wrap justify-content-between align-items-start">
              <div>
                <h5 class="fw-bold mb-1"><a href="product-details.html?id=${item.productId}" class="text-reset">${escapeHtml(item.name)}</a></h5>
                <span class="badge bg-light text-dark border mb-2">${escapeHtml(item.category)}</span>
                <div class="small text-muted">${formatPrice(item.price)} each</div>
              </div>
              <button class="btn btn-sm btn-outline-danger" data-remove="${item.productId}" title="Remove item">
                <i class="bi bi-trash"></i> Remove
              </button>
            </div>
            <div class="d-flex flex-wrap align-items-center justify-content-between mt-2">
              <div class="qty-stepper">
                <button type="button" data-minus="${item.productId}" ${item.quantity > 1 ? '' : 'disabled'}>
                  <i class="bi bi-dash"></i>
                </button>
                <input type="number" value="${item.quantity}" min="1" max="${item.stock}" readonly>
                <button type="button" data-plus="${item.productId}" ${item.quantity < item.stock ? '' : 'disabled'}>
                  <i class="bi bi-plus"></i>
                </button>
              </div>
              <div class="text-end">
                <div class="small text-muted">Subtotal</div>
                <div class="price">${formatPrice(item.subtotal)}</div>
              </div>
            </div>
            ${item.quantity >= item.stock ? '<div class="small text-danger mt-1">Maximum available stock reached</div>' : ''}
          </div>
        </div>
      </div>`).join('');

    syncBadge();

    document.querySelectorAll('[data-remove]').forEach(btn => {
        btn.addEventListener('click', () => removeItem(btn.getAttribute('data-remove')));
    });
    document.querySelectorAll('[data-plus]').forEach(btn => {
        btn.addEventListener('click', () => changeQty(btn.getAttribute('data-plus'), 1));
    });
    document.querySelectorAll('[data-minus]').forEach(btn => {
        btn.addEventListener('click', () => changeQty(btn.getAttribute('data-minus'), -1));
    });

    renderSummary(cartData);
}

function renderSummary(data) {
    const shipping = 0; // free delivery for this demo
    const total = data.total;

    document.getElementById('cartSummary').innerHTML = `
      <h5 class="fw-bold mb-3">Order Summary</h5>
      <div class="summary-row"><span>Subtotal (${data.itemCount} item${data.itemCount === 1 ? '' : 's'})</span><span>${formatPrice(data.total)}</span></div>
      <div class="summary-row"><span>Shipping</span><span class="text-success">FREE</span></div>
      <div class="summary-row"><span>Tax</span><span>PHP 0.00</span></div>
      <div class="summary-row total"><span>Total</span><span>${formatPrice(total)}</span></div>
      <button class="btn btn-fm w-100 mt-3 py-2" id="checkoutBtn">
        <i class="bi bi-credit-card"></i> Proceed to Checkout
      </button>
      <a href="products.html" class="btn btn-outline-fm w-100 mt-2">Continue Shopping</a>
    `;

    document.getElementById('checkoutBtn').addEventListener('click', () => {
        window.location.href = 'checkout.html';
    });
}

function syncBadge() {
    setCartCount(cartData ? cartData.itemCount : 0);
    updateNavCartBadge();
}

async function changeQty(productId, delta) {
    const item = cartData.items.find(i => i.productId === parseInt(productId, 10));
    if (!item) return;
    const newQty = item.quantity + delta;
    if (newQty < 1) return;
    await callUpdate(productId, newQty);
}

async function callUpdate(productId, quantity) {
    try {
        const data = await apiFetch('/cart/update/' + productId, {
            method: 'PUT',
            body: JSON.stringify({ quantity })
        });
        cartData = data;
        renderCart();
    } catch (err) {
        showToast(err.message, 'error');
        loadCart();
    }
}

async function removeItem(productId) {
    try {
        const data = await apiFetch('/cart/remove/' + productId, { method: 'DELETE' });
        cartData = data;
        renderCart();
        showToast('Item removed from cart');
    } catch (err) {
        showToast(err.message, 'error');
        loadCart();
    }
}