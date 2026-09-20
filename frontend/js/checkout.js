/* ============================================================
   FAZZI MART - Checkout page script
   ============================================================ */

document.addEventListener('DOMContentLoaded', () => {
    requireAuth();
    renderNavbar('cart');
    renderFooter();
    initCheckout();
});

let cartData = null;
let orderSuccessModal = null;

async function initCheckout() {
    const user = getStoredUser();
    if (user) {
        document.getElementById('customerName').value = user.name || '';
        document.getElementById('phone').value = user.phone || '';
    }

    try {
        cartData = await apiFetch('/cart');
        setCartCount(cartData.itemCount);
        updateNavCartBadge();

        if (!cartData.items.length) {
            document.getElementById('placeOrderBtn').disabled = true;
            document.getElementById('orderSummaryItems').innerHTML = emptyStateHTML(
                '🛒', 'Your cart is empty',
                'Add some products before checking out.',
                '<a href="products.html" class="btn btn-fm">Browse Products</a>');
            return;
        }

        renderSummary(cartData);

        document.getElementById('placeOrderBtn').addEventListener('click', placeOrder);
    } catch (err) {
        if (err.status === 401) {
            window.location.href = 'index.html';
        } else {
            showToast(err.message, 'error');
            window.location.href = 'cart.html';
        }
    }
}

function renderSummary(data) {
    const itemsWrap = document.getElementById('orderSummaryItems');
    itemsWrap.innerHTML = data.items.map(item => `
      <div class="d-flex justify-content-between align-items-center mb-2 small">
        <div>
          <span class="fw-semibold">${escapeHtml(item.name)}</span>
          <span class="text-muted"> × ${item.quantity}</span>
        </div>
        <span>${formatPrice(item.subtotal)}</span>
      </div>`).join('');

    document.getElementById('summarySubtotal').textContent = formatPrice(data.total);
    document.getElementById('summaryTotal').textContent = formatPrice(data.total);
}

function setError(input, errorEl, message) {
    input.classList.add('is-invalid');
    errorEl.textContent = message;
}

function clearErrors() {
    document.querySelectorAll('#checkoutForm .is-invalid').forEach(el => el.classList.remove('is-invalid'));
}

async function placeOrder() {
    clearErrors();

    const customerName = document.getElementById('customerName').value.trim();
    const phone = document.getElementById('phone').value.trim();
    const address = document.getElementById('address').value.trim();
    const city = document.getElementById('city').value.trim();
    const postalCode = document.getElementById('postalCode').value.trim();

    let valid = true;

    if (!customerName) { setError(document.getElementById('customerName'), document.getElementById('customerNameError'), 'Name is required'); valid = false; }
    if (!phone) { setError(document.getElementById('phone'), document.getElementById('phoneError'), 'Phone number is required'); valid = false; }
    else if (!/^[0-9+\- ]{7,20}$/.test(phone)) { setError(document.getElementById('phone'), document.getElementById('phoneError'), 'Enter a valid phone number'); valid = false; }
    if (!address) { setError(document.getElementById('address'), document.getElementById('addressError'), 'Delivery address is required'); valid = false; }
    if (!city) { setError(document.getElementById('city'), document.getElementById('cityError'), 'City is required'); valid = false; }
    if (!postalCode) { setError(document.getElementById('postalCode'), document.getElementById('postalCodeError'), 'Postal code is required'); valid = false; }

    if (!valid) {
        showToast('Please complete the required fields', 'error');
        return;
    }

    const btn = document.getElementById('placeOrderBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Placing order...';

    try {
        const order = await apiFetch('/orders', {
            method: 'POST',
            body: JSON.stringify({ customerName, phone, address, city, postalCode })
        });

        setCartCount(0);
        updateNavCartBadge();

        document.getElementById('orderIdDisplay').textContent = '#FM-' + order.id;
        document.getElementById('orderTotalDisplay').textContent =
            'Total amount paid: ' + formatPrice(order.totalAmount) + ' • Est. delivery in 3-5 days';

        orderSuccessModal = new bootstrap.Modal(document.getElementById('orderSuccessModal'));
        orderSuccessModal.show();
    } catch (err) {
        showToast(err.message, 'error');
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-circle"></i> Place Order';
    }
}