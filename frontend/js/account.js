/* ============================================================
   FAZZI MART - My Account page script
   ============================================================ */

document.addEventListener('DOMContentLoaded', () => {
    requireAuth();
    renderNavbar('account');
    renderFooter();
    loadProfile();
    loadOrders();
});

async function loadProfile() {
    const container = document.getElementById('profileCard');

    try {
        const profile = await apiFetch('/users/me');

        const initials = profile.name.split(/\s+/)
            .map(w => w.charAt(0)).join('').slice(0, 2).toUpperCase();

        container.innerHTML = `
          <div class="profile-card">
            <div class="d-flex align-items-center gap-3 mb-3">
              <div class="profile-avatar">${escapeHtml(initials)}</div>
              <div>
                <h5 class="fw-bold mb-0">${escapeHtml(profile.name)}</h5>
                <span class="badge bg-fm mt-1">${escapeHtml(profile.role)}</span>
              </div>
            </div>
            <div class="border-top border-white border-opacity-25 pt-3 small">
              <div class="d-flex justify-content-between mb-2">
                <span class="opacity-75"><i class="bi bi-envelope me-2"></i>Email</span>
                <span class="fw-semibold text-end">${escapeHtml(profile.email)}</span>
              </div>
              <div class="d-flex justify-content-between mb-2">
                <span class="opacity-75"><i class="bi bi-telephone me-2"></i>Phone</span>
                <span class="fw-semibold">${escapeHtml(profile.phone)}</span>
              </div>
              <div class="d-flex justify-content-between">
                <span class="opacity-75"><i class="bi bi-person-badge me-2"></i>Member ID</span>
                <span class="fw-semibold">FM-${profile.id}</span>
              </div>
            </div>
          </div>`;

        } catch (err) {
        if (err.status === 401) {
            window.location.href = 'index.html';
        } else {
            container.innerHTML = '<div class="alert alert-danger">Could not load profile: ' + escapeHtml(err.message) + '</div>';
        }
    }
}

async function loadOrders() {
    const container = document.getElementById('orderHistory');
    try {
        const orders = await apiFetch('/orders');

        if (!orders.length) {
            container.innerHTML = emptyStateHTML(
                '📦', 'No orders yet',
                'When you place an order, it will appear here.',
                '<a href="products.html" class="btn btn-fm">Start Shopping</a>');
            return;
        }

        container.innerHTML = orders.map(orderCard).join('');
    } catch (err) {
        if (err.status === 401) {
            window.location.href = 'index.html';
        } else {
            container.innerHTML = '<div class="alert alert-danger">Could not load orders: ' + escapeHtml(err.message) + '</div>';
        }
    }
}

function orderCard(order) {
    const items = order.items.map(item =>
        `<div class="d-flex justify-content-between small mb-1">
           <span>${escapeHtml(item.productName)} <span class="text-muted">× ${item.quantity}</span></span>
           <span>${formatPrice(item.subtotal)}</span>
         </div>`).join('');

    return `
    <div class="order-card mb-3">
      <div class="order-head">
        <div>
          <div class="order-id">Order #FM-${order.id}</div>
          <small class="text-muted"><i class="bi bi-calendar3 me-1"></i>${formatDate(order.orderDate)}</small>
        </div>
        <div class="text-end">
          <span class="status-badge">${escapeHtml(order.status)}</span>
          <div class="price mt-1">${formatPrice(order.totalAmount)}</div>
        </div>
      </div>
      <div class="p-3">
        ${items}
        <div class="small text-muted mt-2 pt-2 border-top">
          <i class="bi bi-geo-alt me-1"></i>${escapeHtml(order.address)}, ${escapeHtml(order.city)} ${escapeHtml(order.postalCode)}
        </div>
      </div>
    </div>`;
}