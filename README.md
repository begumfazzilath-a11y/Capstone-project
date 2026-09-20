# 🐾 FAZZI MART

**Happy Pets. Better Care. Easy Shopping.**

FAZZI MART is a complete full-stack online pet shop (college capstone project).

| Layer     | Technology |
|-----------|------------|
| Frontend  | HTML5, CSS3, JavaScript, Bootstrap 5 |
| Backend   | Java 17, Spring Boot 3.x (Web, Security) |
| Build     | Maven |
| Storage   | Excel (.xlsx) files via Apache POI — no database needed |
| Auth      | JWT + BCrypt |

---

## ✨ Features

- 🔐 JWT-based authentication with BCrypt password hashing
- 👤 Registration + Login pages with validation and show/hide password
- 🏠 Home page: hero, categories, featured products
- 🛍️ Products page: live search, category filter, price/rating sorting
- 🔎 Product Details: quantity, Add to Cart, Buy Now
- 🛒 Shopping Cart: add / update / remove items, order summary
- 💳 Checkout: customer + delivery details, order success with Order ID
- 📦 My Account: profile + order history
- 🛡️ Protected REST APIs (cart, orders, account) via Spring Security + JWT

---

## 📁 Project Structure

```
fazzi capstone/
├── README.md
├── backend/                                # Spring Boot REST API
│   ├── pom.xml
│   └── src/main/
│       ├── resources/
│       │   └── application.properties
│       └── java/com/fazzimart/
│           ├── FazziMartApplication.java
│           ├── config/
│           │   ├── SecurityConfig.java     # Spring Security + CORS
│           │   └── DataSeeder.java         # Seeds admin + 16 products
│           ├── controller/                 # Auth, Product, Cart, Order, User
│           ├── dto/                        # Register/Login/Product/Cart/Order DTOs
│           ├── dao/                        # Excel DAOs (User, Product, Cart, Order)
│           ├── exception/                  # ApiException + global handler
│           ├── model/                      # User, Product, Order, OrderItem, CartItem
│           ├── security/                   # JwtTokenProvider + JwtAuthenticationFilter
│           ├── service/                    # Auth, Product, Cart, Order services
│           └── util/ExcelUtil.java         # Apache POI .xlsx read/write helpers
└── frontend/                               # Static frontend
    ├── index.html                          # LOGIN (first page)
    ├── register.html
    ├── home.html                           # Home (after login)
    ├── products.html                       # Search / filter / sort
    ├── product-details.html
    ├── cart.html
    ├── checkout.html
    ├── account.html
    ├── css/style.css
    ├── js/  (common.js + one script per page)
    └── images/  (SVG product placeholders)
```

All data is stored in Excel files created automatically in:
`<user home>/fazzimart-data/`

| File            | Contains                          |
|-----------------|-----------------------------------|
| `users.xlsx`    | Accounts (BCrypt-hashed passwords)|
| `products.xlsx` | Catalogue (16 seeded products)    |
| `cart_items.xlsx`| Shopping carts                  |
| `orders.xlsx`   | Customer orders                   |
| `order_items.xlsx`| Items inside each order         |

> To use a different data folder, set the system property `fazzi.data.dir`
> or the environment variable `FAZZIMART_DATA_DIR`.

---

## 🛠 VS Code Setup (Step by Step)

### 0. Prerequisites
Install on the machine where it will run:

1. **Java JDK 17** — https://adoptium.net
2. **Maven 3.8+** — https://maven.apache.org (or use the bundled Maven in VS Code)
3. **VS Code** — https://code.visualstudio.com

> No database is required. All data lives in Excel files.

### 1. Install VS Code Extensions
Open VS Code → Extensions (Ctrl+Shift+X) and install:
- **Extension Pack for Java** (vscjava.vscode-java-pack)
- **Maven for Java** (vscjava.vscode-maven)
- **Live Server** (ritwickdey.liveserver)

### 2. Run the Backend (Spring Boot)
1. In VS Code: **File → Open Folder** → select the `backend` folder.
2. Wait for Maven to finish downloading dependencies (bottom-right progress).
3. Open `FazziMartApplication.java` and press **Run** (▶) — or use the
   **Spring Boot Dashboard**, or in the terminal:
   ```
   cd backend
   mvn spring-boot:run
   ```
4. Backend runs at **http://localhost:9090** (embedded Tomcat — it does NOT
   need an external Tomcat server).
   - First launch creates the Excel data files and seeds the **admin user**
     and **16 sample products**.

### 4. Run the Frontend
1. In VS Code: **File → Open Folder** → select the `frontend` folder.
2. Right-click `index.html` → **Open with Live Server**.
   (or use any static server: `python -m http.server 5500` in `frontend/`)
3. Frontend opens at **http://localhost:5500/index.html** → the **Login page**.

### 5. Login
- Register a new account, **or** use the seeded admin:
  - 📧 `admin@fazzimart.com`
  - 🔑 `Admin@123`

After login you land on the **Home page**.

---

## 📡 REST API Summary

| Method | Endpoint                 | Auth       | Description                          |
|--------|--------------------------|------------|--------------------------------------|
| POST   | /api/auth/register       | Public     | Register + return JWT                |
| POST   | /api/auth/login          | Public     | Login + return JWT                   |
| GET    | /api/products            | Public     | List (params: query, category, sort) |
| GET    | /api/products/featured   | Public     | Featured products                    |
| GET    | /api/products/{id}       | Public     | Product details                      |
| POST   | /api/products            | Admin      | Add product                          |
| PUT    | /api/products/{id}       | Admin      | Update product                       |
| DELETE | /api/products/{id}       | Admin      | Delete product                       |
| GET    | /api/cart                | JWT        | View cart                            |
| POST   | /api/cart/add            | JWT        | Add item                             |
| PUT    | /api/cart/update/{id}    | JWT        | Update quantity                      |
| DELETE | /api/cart/remove/{id}    | JWT        | Remove item                          |
| DELETE | /api/cart/clear          | JWT        | Clear cart                           |
| POST   | /api/orders              | JWT        | Place order (clears cart)            |
| GET    | /api/orders              | JWT        | My orders                            |
| GET    | /api/orders/{id}         | JWT        | Order details                        |
| GET    | /api/users/me            | JWT        | Logged-in user profile               |

Sort values: `price_asc`, `price_desc`, `name_asc`, `rating_desc`, `default`.

---

## 🐾 Sample Products (16, 4 per category)

Pet Food: Premium Dog Food, Deluxe Cat Food, Puppy Food, Adult Dog Food
Pet Products: Shampoo, Grooming Brush, Dental Kit, Nail Clipper
Pet Accessories: Collar, Bowl, Leash, Pet Bed
Pet Toys: Rubber Ball, Rope Toy, Squeaky Duck, Catnip Mouse

---

## ❓ Troubleshooting

| Problem | Fix |
|---------|-----|
| `fazzi-mart-data` folder doesn't exist | It is created automatically on first launch |
| Frontend can't reach backend | Backend must be running on port 9090 (CORS is already enabled) |
| Port 9090 in use | Change `server.port` in `application.properties` and update `API_BASE` in `frontend/js/common.js` |
| 401 on cart/checkout | You must be logged in first |
| Excel data files getting torn | Run only one backend instance at a time (files are locked per request) |
| Product images not showing | Open the frontend via Live Server (not just double-clicking the HTML) |