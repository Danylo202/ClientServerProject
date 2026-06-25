package com.danil.app.server.HTTP.homework;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LoginPageHandler implements HttpHandler {
    private static final String HTML = """
            <!DOCTYPE html>
            <html lang="uk">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Магазин — Склад</title>
              <style>
                :root { --bg:#eef2f7; --panel:#fff; --text:#111827; --muted:#64748b; --line:#e5e7eb;
                        --brand:#2563eb; --brand2:#1d4ed8; --danger:#dc2626; --ok:#15803d; }
                * { box-sizing:border-box; }
                body { margin:0; min-height:100vh; font-family:Segoe UI,Arial,sans-serif; color:var(--text);
                       background:linear-gradient(135deg,#dbeafe 0%,#f8fafc 48%,#eef2ff 100%); }
                button,input,select { font:inherit; }
                button { border:0; border-radius:10px; padding:10px 14px; cursor:pointer; font-weight:600;
                         background:var(--brand); color:white; box-shadow:0 8px 18px rgba(37,99,235,.18); }
                button:hover { background:var(--brand2); }
                button.secondary { background:#f1f5f9; color:#0f172a; box-shadow:none; border:1px solid var(--line); }
                button.danger { background:var(--danger); }
                button.ghost { background:transparent; color:var(--brand); box-shadow:none; }
                input,select { width:100%; border:1px solid var(--line); border-radius:10px; padding:10px 12px; background:#fff; }
                label { display:block; color:var(--muted); font-size:13px; margin:0 0 6px; }
                table { width:100%; border-collapse:collapse; background:#fff; border-radius:14px; overflow:hidden; }
                th,td { padding:12px 14px; border-bottom:1px solid var(--line); text-align:left; font-size:14px; }
                th { background:#f8fafc; color:#475569; font-size:12px; text-transform:uppercase; letter-spacing:.04em; }
                tr:hover td { background:#f8fafc; }
                .login-wrap { min-height:100vh; display:grid; place-items:center; padding:24px; }
                .login-card { width:min(420px,100%); background:rgba(255,255,255,.92); backdrop-filter:blur(16px);
                              border:1px solid rgba(255,255,255,.7); border-radius:24px; padding:32px;
                              box-shadow:0 24px 70px rgba(15,23,42,.14); }
                .logo { width:54px; height:54px; border-radius:16px; display:grid; place-items:center;
                        color:#fff; background:linear-gradient(135deg,#2563eb,#7c3aed); font-size:26px; margin:0 auto 16px; }
                .title { text-align:center; margin:0 0 6px; font-size:26px; }
                .subtitle { text-align:center; color:var(--muted); margin:0 0 26px; }
                .hint { background:#f8fafc; border:1px solid var(--line); border-radius:14px; padding:12px; color:#475569;
                        font-size:13px; line-height:1.45; margin-top:18px; }
                .field { margin-bottom:14px; }
                .status { min-height:22px; margin-top:12px; color:var(--danger); text-align:center; }
                .app { display:none; min-height:100vh; }
                .shell { display:grid; grid-template-columns:260px 1fr; min-height:100vh; }
                .sidebar { background:#0f172a; color:#fff; padding:24px; }
                .brand { font-weight:800; font-size:22px; margin-bottom:28px; }
                .nav-btn { width:100%; text-align:left; margin-bottom:10px; background:rgba(255,255,255,.08); box-shadow:none; }
                .nav-btn.active { background:#2563eb; }
                .content { padding:28px; }
                .topbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:22px; gap:16px; }
                .badge { display:inline-flex; align-items:center; gap:8px; background:#e0f2fe; color:#075985;
                         padding:7px 11px; border-radius:999px; font-size:13px; font-weight:700; }
                .grid { display:grid; gap:16px; }
                .card { background:var(--panel); border:1px solid var(--line); border-radius:20px; padding:18px;
                        box-shadow:0 16px 40px rgba(15,23,42,.06); }
                .card-title { margin:0 0 14px; font-size:18px; }
                .filters { display:grid; grid-template-columns:repeat(5,1fr); gap:12px; align-items:end; }
                .form-grid { display:grid; grid-template-columns:repeat(5,1fr); gap:12px; align-items:end; }
                .actions { display:flex; gap:8px; flex-wrap:wrap; }
                .readonly-note { color:var(--muted); background:#fff7ed; border:1px solid #fed7aa;
                                 border-radius:14px; padding:12px; }
                .hidden { display:none !important; }
                @media (max-width:900px) { .shell{grid-template-columns:1fr}.sidebar{position:static}.filters,.form-grid{grid-template-columns:1fr 1fr}.content{padding:18px} }
              </style>
            </head>
            <body>
              <section id="login-screen" class="login-wrap">
                <div class="login-card">
                  <div class="logo">S</div>
                  <h1 class="title">Store</h1>
                  <p class="subtitle">Склад товарів</p>
                  <div class="field">
                    <label for="username">Логін</label>
                    <input id="username" type="text" autocomplete="username" placeholder="admin або employee">
                  </div>
                  <div class="field">
                    <label for="password">Пароль</label>
                    <input id="password" type="password" autocomplete="current-password" placeholder="Ваш пароль">
                  </div>
                  <button id="loginBtn" type="button" style="width:100%">Увійти</button>
                  <div id="login-status" class="status"></div>
                </div>
              </section>

              <section id="app" class="app">
                <div class="shell">
                  <aside class="sidebar">
                    <div class="brand">Store Manager</div>
                    <button id="productsNav" class="nav-btn active">Товари</button>
                    <button id="categoriesNav" class="nav-btn">Категорії</button>
                    <button id="logoutBtn" class="nav-btn">Вийти</button>
                  </aside>
                  <main class="content">
                    <div class="topbar">
                      <div>
                        <h1 id="pageTitle" style="margin:0">Товари</h1>
                      </div>
                      <div class="badge"><span id="currentUser"></span><span id="currentRole"></span></div>
                    </div>

                    <section id="products-view" class="grid">
                      <div class="card">
                        <h2 class="card-title">Пошук товарів</h2>
                        <div class="filters">
                          <div><label>Назва</label><input id="pNameFilter"></div>
                          <div><label>Категорія</label><input id="pCategoryFilter"></div>
                          <div><label>Ціна від</label><input id="pMinFilter" type="number" step="0.01"></div>
                          <div><label>Ціна до</label><input id="pMaxFilter" type="number" step="0.01"></div>
                          <div class="actions">
                            <button id="searchProductsBtn">Шукати</button>
                            <button id="resetProductsBtn" class="secondary">Скинути</button>
                          </div>
                        </div>
                      </div>

                      <div id="productAdminCard" class="card admin-only">
                        <h2 class="card-title">Створити товар</h2>
                        <div class="form-grid">
                          <div><label>Назва</label><input id="createProductName"></div>
                          <div><label>Категорія</label><select id="createProductCategory"></select></div>
                          <div><label>Ціна</label><input id="createProductPrice" type="number" step="0.01"></div>
                          <div><label>Кількість</label><input id="createProductQuantity" type="number"></div>
                          <div class="actions">
                            <button id="createProductBtn">Створити</button>
                            <button id="clearCreateProductBtn" class="secondary">Очистити</button>
                          </div>
                        </div>
                      </div>

                      <div id="productEditCard" class="card admin-only hidden">
                        <h2 class="card-title">Редагувати товар</h2>
                        <div class="form-grid">
                          <input id="editProductId" type="hidden">
                          <div><label>Назва</label><input id="editProductName"></div>
                          <div><label>Категорія</label><select id="editProductCategory"></select></div>
                          <div><label>Ціна</label><input id="editProductPrice" type="number" step="0.01"></div>
                          <div><label>Кількість</label><input id="editProductQuantity" type="number"></div>
                          <div class="actions">
                            <button id="updateProductBtn">Зберегти зміни</button>
                            <button id="cancelEditProductBtn" class="secondary">Скасувати</button>
                          </div>
                        </div>
                      </div>

                      <div id="productReadonly" class="readonly-note hidden">Вам доступний тільки перегляд інформації.</div>

                      <div class="card">
                        <div class="topbar" style="margin-bottom:10px">
                          <h2 class="card-title" style="margin:0">Список товарів</h2>
                          <div id="productsCount" style="color:var(--muted)"></div>
                        </div>
                        <table>
                          <thead><tr><th>ID</th><th>Назва</th><th>Категорія</th><th>Ціна</th><th>Кількість</th><th class="admin-only">Дії</th></tr></thead>
                          <tbody id="productsBody"></tbody>
                        </table>
                      </div>
                    </section>

                    <section id="categories-view" class="grid hidden">
                      <div id="categoryAdminCard" class="card admin-only">
                        <h2 class="card-title">Створення категорії</h2>
                        <div class="form-grid">
                          <div><label>Назва категорії</label><input id="createCategoryName"></div>
                          <div class="actions">
                            <button id="createCategoryBtn">Створити</button>
                            <button id="clearCreateCategoryBtn" class="secondary">Очистити</button>
                          </div>
                        </div>
                      </div>
                      <div id="categoryEditCard" class="card admin-only hidden">
                        <h2 class="card-title">Редагувати категорію</h2>
                        <div class="form-grid">
                          <input id="editCategoryOldName" type="hidden">
                          <div><label>Нова назва категорії</label><input id="editCategoryName"></div>
                          <div class="actions">
                            <button id="updateCategoryBtn">Зберегти зміни</button>
                            <button id="cancelEditCategoryBtn" class="secondary">Скасувати</button>
                          </div>
                        </div>
                      </div>
                      <div id="categoryReadonly" class="readonly-note hidden">Вам доступний тільки перегляд категорій.</div>
                      <div class="card">
                        <div class="topbar" style="margin-bottom:10px">
                          <h2 class="card-title" style="margin:0">Категорії</h2>
                          <button id="refreshCategoriesBtn" class="secondary">Оновити</button>
                        </div>
                        <table>
                          <thead><tr><th>Назва</th><th>Кількість товарів</th><th>Середня ціна</th><th class="admin-only">Дії</th></tr></thead>
                          <tbody id="categoriesBody"></tbody>
                        </table>
                      </div>
                    </section>
                    <div id="app-status" class="status"></div>
                  </main>
                </div>
              </section>

              <script>
                let token = null;
                let currentUser = null;
                let currentRole = null;
                let categories = [];

                const $ = id => document.getElementById(id);
                const isAdmin = () => currentRole === 'admin';
                const headers = () => ({ 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' });

                function setStatus(text, danger = true) {
                  $('app-status').textContent = text || '';
                  $('app-status').style.color = danger ? '#dc2626' : '#15803d';
                }

                async function api(url, options = {}) {
                  const res = await fetch(url, { ...options, headers: { ...headers(), ...(options.headers || {}) } });
                  const text = await res.text();
                  const data = text ? JSON.parse(text) : null;
                  if (!res.ok) throw new Error(data?.error || 'Помилка запиту');
                  return data;
                }

                async function login() {
                  const username = $('username').value.trim();
                  const password = $('password').value;
                  const status = $('login-status');
                  if (!username || !password) {
                    status.textContent = 'Заповніть усі поля';
                    return;
                  }
                  $('loginBtn').disabled = true;
                  status.style.color = '#64748b';
                  status.textContent = 'Перевірка облікових даних…';
                  try {
                    const res = await fetch('/login', {
                      method: 'POST',
                      headers: {'Content-Type': 'application/json'},
                      body: JSON.stringify({username, password})
                    });
                    if (res.ok) {
                      const data = await res.json();
                      token = data.token;
                      currentUser = data.username;
                      currentRole = data.role;
                      openApp();
                    } else {
                      status.style.color = '#b91c1c';
                      status.textContent = 'Неправильне ім\\'я користувача або пароль';
                    }
                  } catch (e) {
                    status.style.color = '#b91c1c';
                    status.textContent = 'Не вдалося з\\'єднатися з сервером';
                  } finally {
                    $('loginBtn').disabled = false;
                  }
                }

                function openApp() {
                  $('login-screen').style.display = 'none';
                  $('app').style.display = 'block';
                  $('currentUser').textContent = currentUser;
                  $('currentRole').textContent = currentRole === 'admin' ? 'Адміністратор' : 'Працівник';
                  document.querySelectorAll('.admin-only').forEach(el => el.classList.toggle('hidden', !isAdmin()));
                  $('productReadonly').classList.toggle('hidden', isAdmin());
                  $('categoryReadonly').classList.toggle('hidden', isAdmin());
                  loadCategories().then(loadProducts);
                }

                function switchView(view) {
                  const products = view === 'products';
                  $('products-view').classList.toggle('hidden', !products);
                  $('categories-view').classList.toggle('hidden', products);
                  $('productsNav').classList.toggle('active', products);
                  $('categoriesNav').classList.toggle('active', !products);
                  $('pageTitle').textContent = products ? 'Товари' : 'Категорії';
                  setStatus('');
                  if (products) loadProducts(); else loadCategories();
                }

                async function loadProducts() {
                  const params = new URLSearchParams();
                  if ($('pNameFilter').value.trim()) params.set('name', $('pNameFilter').value.trim());
                  if ($('pCategoryFilter').value.trim()) params.set('category', $('pCategoryFilter').value.trim());
                  if ($('pMinFilter').value) params.set('minPrice', $('pMinFilter').value);
                  if ($('pMaxFilter').value) params.set('maxPrice', $('pMaxFilter').value);
                  const products = await api('/products?' + params.toString());
                  $('productsCount').textContent = 'Знайдено: ' + products.length;
                  $('productsBody').innerHTML = products.map(p => `
                    <tr>
                      <td>${p.id}</td><td>${escapeHtml(p.name)}</td><td>${escapeHtml(p.category)}</td>
                      <td>${p.price}</td><td>${p.quantity}</td>
                      ${isAdmin() ? `<td class="actions"><button class="secondary" onclick='editProduct(${JSON.stringify(p)})'>Редагувати</button><button class="danger" onclick='deleteProduct(${p.id})'>Видалити</button></td>` : ''}
                    </tr>`).join('');
                }

                async function loadCategories() {
                  categories = await api('/categories');
                  renderCategoryOptions();
                  $('categoriesBody').innerHTML = categories.map(c => `
                    <tr>
                      <td>${escapeHtml(c.name)}</td><td>${c.productCount}</td><td>${Number(c.averagePrice).toFixed(2)}</td>
                      ${isAdmin() ? `<td class="actions"><button class="secondary" onclick='editCategory(${JSON.stringify(c.name)})'>Редагувати</button><button class="danger" onclick='deleteCategory(${JSON.stringify(c.name)})'>Видалити</button></td>` : ''}
                    </tr>`).join('');
                }

                function renderCategoryOptions() {
                  const options = categories.map(c => `<option value="${escapeAttr(c.name)}">${escapeHtml(c.name)}</option>`).join('');
                  $('createProductCategory').innerHTML = options;
                  $('editProductCategory').innerHTML = options;
                }

                async function createProduct() {
                  const product = {
                    id: 0,
                    name: $('createProductName').value.trim(),
                    category: $('createProductCategory').value,
                    price: Number($('createProductPrice').value),
                    quantity: Number($('createProductQuantity').value)
                  };
                  if (!product.name || !product.category) return setStatus('Заповніть назву і категорію');
                  await api('/products', { method: 'PUT', body: JSON.stringify(product) });
                  clearCreateProductForm();
                  await loadProducts();
                  setStatus('Товар створено', false);
                }

                async function updateProduct() {
                  const id = $('editProductId').value;
                  const product = {
                    id: Number(id),
                    name: $('editProductName').value.trim(),
                    category: $('editProductCategory').value,
                    price: Number($('editProductPrice').value),
                    quantity: Number($('editProductQuantity').value)
                  };
                  if (!id) return setStatus('Оберіть товар для редагування');
                  if (!product.name || !product.category) return setStatus('Заповніть назву і категорію');
                  await api('/products/' + id, { method: 'POST', body: JSON.stringify(product) });
                  clearEditProductForm();
                  await loadProducts();
                  setStatus('Товар оновлено', false);
                }

                function editProduct(p) {
                  $('editProductId').value = p.id;
                  $('editProductName').value = p.name;
                  $('editProductCategory').value = p.category;
                  $('editProductPrice').value = p.price;
                  $('editProductQuantity').value = p.quantity;
                  $('productEditCard').classList.remove('hidden');
                  $('productEditCard').scrollIntoView({ behavior: 'smooth', block: 'center' });
                }

                async function deleteProduct(id) {
                  if (!confirm('Видалити товар?')) return;
                  await api('/products/' + id, { method: 'DELETE' });
                  await loadProducts();
                  setStatus('Товар видалено', false);
                }

                function clearCreateProductForm() {
                  ['createProductName','createProductPrice','createProductQuantity'].forEach(id => $(id).value = '');
                  if ($('createProductCategory').options.length) $('createProductCategory').selectedIndex = 0;
                }

                function clearEditProductForm() {
                  ['editProductId','editProductName','editProductPrice','editProductQuantity'].forEach(id => $(id).value = '');
                  if ($('editProductCategory').options.length) $('editProductCategory').selectedIndex = 0;
                  $('productEditCard').classList.add('hidden');
                }

                async function createCategory() {
                  const name = $('createCategoryName').value.trim();
                  if (!name) return setStatus('Введіть назву категорії');
                  await api('/categories', { method: 'PUT', body: JSON.stringify({ name }) });
                  clearCreateCategoryForm();
                  await loadCategories();
                  await loadProducts();
                  setStatus('Категорію створено', false);
                }

                async function updateCategory() {
                  const oldName = $('editCategoryOldName').value;
                  const newName = $('editCategoryName').value.trim();
                  if (!oldName) return setStatus('Оберіть категорію для редагування');
                  if (!newName) return setStatus('Введіть нову назву категорії');
                  await api('/categories', { method: 'POST', body: JSON.stringify({ oldName, newName }) });
                  clearEditCategoryForm();
                  await loadCategories();
                  await loadProducts();
                  setStatus('Категорію оновлено', false);
                }

                async function createQuickCategory() {
                  const name = $('quickCategoryName').value.trim();
                  if (!name) return setStatus('Введіть назву категорії');
                  await api('/categories', { method: 'PUT', body: JSON.stringify({ name }) });
                  $('quickCategoryName').value = '';
                  await loadCategories();
                  $('createProductCategory').value = name;
                  setStatus('Категорію створено', false);
                }

                function editCategory(name) {
                  $('editCategoryOldName').value = name;
                  $('editCategoryName').value = name;
                  $('categoryEditCard').classList.remove('hidden');
                  $('categoryEditCard').scrollIntoView({ behavior: 'smooth', block: 'center' });
                }

                async function deleteCategory(name) {
                  if (!confirm('Видалити категорію "' + name + '"? Це можливо тільки якщо в ній немає товарів.')) return;
                  await api('/categories/' + encodeURIComponent(name), { method: 'DELETE' });
                  await loadCategories();
                  setStatus('Категорію видалено', false);
                }

                function clearCreateCategoryForm() {
                  $('createCategoryName').value = '';
                }

                function clearEditCategoryForm() {
                  $('editCategoryOldName').value = '';
                  $('editCategoryName').value = '';
                  $('categoryEditCard').classList.add('hidden');
                }

                function escapeHtml(value) {
                  return String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[ch]));
                }

                function escapeAttr(value) {
                  return escapeHtml(value).replace(/`/g, '&#096;');
                }

                $('loginBtn').addEventListener('click', login);
                $('password').addEventListener('keydown', e => {
                  if (e.key === 'Enter') login();
                });
                $('productsNav').addEventListener('click', () => switchView('products'));
                $('categoriesNav').addEventListener('click', () => switchView('categories'));
                $('logoutBtn').addEventListener('click', () => location.reload());
                $('searchProductsBtn').addEventListener('click', loadProducts);
                $('resetProductsBtn').addEventListener('click', () => {
                  ['pNameFilter','pCategoryFilter','pMinFilter','pMaxFilter'].forEach(id => $(id).value = '');
                  loadProducts();
                });
                $('createProductBtn').addEventListener('click', () => createProduct().catch(e => setStatus(e.message)));
                $('clearCreateProductBtn').addEventListener('click', clearCreateProductForm);
                $('updateProductBtn').addEventListener('click', () => updateProduct().catch(e => setStatus(e.message)));
                $('cancelEditProductBtn').addEventListener('click', clearEditProductForm);
                $('quickCategoryBtn').addEventListener('click', () => createQuickCategory().catch(e => setStatus(e.message)));
                $('createCategoryBtn').addEventListener('click', () => createCategory().catch(e => setStatus(e.message)));
                $('clearCreateCategoryBtn').addEventListener('click', clearCreateCategoryForm);
                $('updateCategoryBtn').addEventListener('click', () => updateCategory().catch(e => setStatus(e.message)));
                $('cancelEditCategoryBtn').addEventListener('click', clearEditCategoryForm);
                $('refreshCategoriesBtn').addEventListener('click', () => loadCategories().catch(e => setStatus(e.message)));
              </script>
            </body>
            </html>
            """;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        byte[] body = HTML.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
