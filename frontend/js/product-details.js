/* ============================================================
   FAZZI MART - Product details page script
   ============================================================ */

let currentProduct = null;

document.addEventListener('DOMContentLoaded', async () => {
    requireAuth();
    renderNavbar('products');
    renderFooter();

    const params = new URLSearchParams(window.location.search);
    const productId = params.get('id');

    if (!productId) {
        document.getElementById('productDetail').innerHTML = emptyStateHTML(
            '🔍', 'No product selected', 'Please choose a product from the catalog.',
            '<a href="products.html" class="btn btn-fm">Browse Products</a>');
        return;
    }

    const container = document.getElementById('productDetail');

    try {
        const product = await apiFetch('/products/' + productId);
        currentProduct = product;
        document.getElementById('crumbProduct').textContent = product.name;
        document.title = product.name + ' | FAZZI MART';
        renderProduct(product);
        loadRelated(product);
    } catch (err) {
        if (err.status === 401) {
            window.location.href = 'index.html';
        } else if (err.status === 404) {
            container.innerHTML = emptyStateHTML('😿', 'Product not found', '',
                '<a href="products.html" class="btn btn-fm">Browse Products</a>');
        } else {
            container.innerHTML = emptyStateHTML('😿', 'Could not load product', escapeHtml(err.message),
                '<a href="products.html" class="btn btn-fm">Browse Products</a>');
        }
    }
});

function renderProduct(p) {
    const inStock = p.stock > 0;

    document.getElementById('productDetail').innerHTML = `
      <div class="row g-4">
        <div class="col-lg-6 text-center">
          <img src="${escapeHtml(p.imageUrl)}" alt="${escapeHtml(p.name)}" class="product-detail-img shadow-sm"
               onerror="this.onerror=null;this.src='images/dog-food.svg';">
        </div>
        <div class="col-lg-6">
          <span class="badge bg-fm mb-2">${escapeHtml(p.category)}</span>
          <h1 class="fw-bold mb-1">${escapeHtml(p.name)}</h1>
          <div class="mb-2">${renderStars(p.rating)}</div>

          <h2 class="price fs-1 mb-2">${formatPrice(p.price)}</h2>

          ${inStock
            ? '<p class="text-success fw-semibold"><i class="bi bi-check-circle"></i> In Stock (' + p.stock + ' available)</p>'
            : '<p class="text-danger fw-semibold"><i class="bi bi-x-circle"></i> Out of Stock</p>'}

          <p class="lead mt-3">${escapeHtml(p.description)}</p>

          <div class="d-flex flex-wrap align-items-center gap-3 mt-4">
            <div class="qty-stepper">
              <button type="button" id="qtyMinus" ${inStock ? '' : 'disabled'}><i class="bi bi-dash"></i></button>
              <input type="number" id="qtyInput" value="1" min="1" max="${inStock ? p.stock : 1}" readonly>
              <button type="button" id="qtyPlus" ${inStock ? '' : 'disabled'}><i class="bi bi-plus"></i></button>
            </div>
            <button class="btn btn-outline-fm btn-lg" id="addToCartBtn" ${inStock ? '' : 'disabled'}>
              <i class="bi bi-cart-plus"></i> Add to Cart
            </button>
            <button class="btn btn-fm btn-lg" id="buyNowBtn" ${inStock ? '' : 'disabled'}>
              <i class="bi bi-lightning-charge"></i> Buy Now
            </button>
          </div>

          <div class="mt-4 small text-muted">
            <div class="d-flex align-items-center gap-2 mb-1"><i class="bi bi-truck text-fm"></i> Free delivery for orders over PHP 500</div>
            <div class="d-flex align-items-center gap-2 mb-1"><i class="bi bi-arrow-repeat text-fm"></i> 7-day easy returns</div>
            <div class="d-flex align-items-center gap-2"><i class="bi bi-shield-lock text-fm"></i> Secure checkout with FAZZI MART</div>
          </div>
        </div>
      </div>`;

    const qtyInput = document.getElementById('qtyInput');
    const qtyPlus = document.getElementById('qtyPlus');
    const qtyMinus = document.getElementById('qtyMinus');

    qtyPlus.addEventListener('click', () => {
        let q = parseInt(qtyInput.value, 10);
        if (q < p.stock) { qtyInput.value = q + 1; }
        else showToast('Only ' + p.stock + ' units available', 'error');
    });
    qtyMinus.addEventListener('click', () => {
        let q = parseInt(qtyInput.value, 10);
        if (q > 1) qtyInput.value = q - 1;
    });

    document.getElementById('addToCartBtn').addEventListener('click', async () => {
        await handleCartAction(false);
    });
    document.getElementById('buyNowBtn').addEventListener('click', async () => {
        await handleCartAction(true);
    });
}

async function handleCartAction(buyNow) {
    const btn = buyNow ? document.getElementById('buyNowBtn') : document.getElementById('addToCartBtn');
    const quantity = parseInt(document.getElementById('qtyInput').value, 10);
    btn.disabled = true;
    try {
        await addToCart(currentProduct.id, quantity);
        showToast(quantity + ' × ' + currentProduct.name + ' added to cart 🛒');
        if (buyNow) {
            setTimeout(() => { window.location.href = 'cart.html'; }, 1100);
        } else {
            btn.disabled = false;
            document.getElementById('qtyInput').value = '1';
        }
    } catch (err) {
        showToast(err.message, 'error');
        btn.disabled = false;
    }
}

async function loadRelated(p) {
    const container = document.getElementById('relatedProducts');
    try {
        const products = await apiFetch('/products?category=' + encodeURIComponent(p.category) + '&sort=rating_desc');
        const related = products.filter(x => x.id !== p.id).slice(0, 4);
        if (!related.length) {
            container.innerHTML = '';
            return;
        }
        container.innerHTML = related.map(productCard).join('');
        document.querySelectorAll('#relatedProducts [data-add-cart]').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.preventDefault();
                btn.disabled = true;
                try {
                    await addToCart(parseInt(btn.getAttribute('data-add-cart'), 10), 1);
                    showToast('Added to cart! 🛒');
                } catch (err) {
                    showToast(err.message, 'error');
                } finally {
                    btn.disabled = false;
                }
            });
        });
    } catch (err) {
        container.innerHTML = '';
    }
}