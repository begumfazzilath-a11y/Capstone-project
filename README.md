# 🐾 FAZZI MART

**Happy Pets. Better Care. Easy Shopping.**

FAZZI MART is a complete full-stack online pet shop (college capstone project).

| Layer     | Technology |
|-----------|------------|
| Frontend  | HTML5, CSS3, JavaScript, Bootstrap 5 |
| Backend   | Java 17, Spring Boot 3.x (Web, Security) |
| Build     | Maven |
| Storage   | MySQL 8 database via JDBC (`com.mysql:mysql-connector-j`) |
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
├── Dockerfile                           # Production image (build + run JRE 17)
├── docker-entrypoint.sh                 # Uses $PORT, starts the JAR
├── docker-compose.yml                   # MySQL 8 + app in one command
├── render.yaml                          # Render.com deployment blueprint
├── database/fazzi_mart.sql                  # MySQL 8 schema (run once by hand)
├── backend/                                # Spring Boot REST API
│   ├── pom.xml
│   └── src/main/
│       ├── resources/
│       │   ├── application.properties
│       │   ├── db.properties               # MySQL username / password / URL
│       │   ├── db.properties.example       # template - copy to db.properties
│       │   └── sql/fazzi_mart_schema.sql   # same schema, auto-run on first connect
│       └── java/com/fazzimart/
│           ├── FazziMartApplication.java
│           ├── config/
│           │   ├── SecurityConfig.java     # Spring Security + CORS
│           │   └── DataSeeder.java         # Seeds admin + 16 products
│           ├── controller/                 # Auth, Product, Cart, Order, User
│           ├── dto/                        # Register/Login/Product/Cart/Order DTOs
│           ├── dao/                        # JDBC DAOs (User, Product, Cart, Order)
│           ├── exception/                  # ApiException + global handler
│           ├── model/                      # User, Product, Order, OrderItem, CartItem
│           ├── security/                   # JwtTokenProvider + JwtAuthenticationFilter
│           ├── service/                    # Auth, Product, Cart, Order services
│           └── util/DBConnection.java      # MySQL JDBC connection + schema (re)init
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

> The frontend is automatically copied into the JAR at build time
> (`classpath:/static/`), so a deployed JAR serves the pages by itself.
> During local dev, `file:../frontend/` is used first so live edits show up
> without rebuilding.

All data lives in the **MySQL 8** database `fazzimart` (created and
connected by `util/DBConnection.java`):

| Table         | Contains                          |
|---------------|-----------------------------------|
| `users`       | Accounts (BCrypt-hashed passwords)|
| `products`    | Catalogue (16 seeded products)    |
| `cart_items`  | Shopping carts (FK → users, products) |
| `orders`      | Customer orders (FK → users)      |
| `order_items` | Items inside each order (snapshot)|

The schema is `database/fazzi_mart.sql`. It is also auto-created on the
first connection by `DBConnection` (idempotent `CREATE TABLE IF NOT EXISTS`),
so running the script by hand is optional.

---

## 🛠 VS Code Setup (Step by Step)

### 0. Prerequisites
Install on the machine where it will run:

1. **Java JDK 17** — https://adoptium.net
2. **Maven 3.8+** — https://maven.apache.org (or use the bundled Maven in VS Code)
3. **MySQL 8** (8.0+ / 8.4 LTS) — https://dev.mysql.com/downloads/mysql/
4. **VS Code** — https://code.visualstudio.com

> The app needs MySQL 8 running locally and the `fazzimart` database
> available. The tables are created automatically on first connect (or
> by running `database/fazzi_mart.sql` by hand).

### 1. Configure MySQL username / password

1. Start your **MySQL 8** server.
2. Create `backend/src/main/resources/db.properties` from the template
   (the real `db.properties` is git-ignored — never commit your password):
   ```
   copy backend\src\main\resources\db.properties.example backend\src\main\resources\db.properties
   ```
3. Set your MySQL credentials in `db.properties` (the only file you usually edit):
   ```properties
   db.url=jdbc:mysql://localhost:3306/fazzimart?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
   db.user=root
   db.password=YOUR_MYSQL_PASSWORD
   ```
   Overrides (highest first): JVM system properties `fazzimart.db.url / db.user /
   db.password`, or the environment variables `DB_URL / DB_USER / DB_PASSWORD`
   (also `FAZZIMART_DB_URL` etc.).
3. *(Optional but recommended for the review)* create the schema by hand:
   ```
   mysql -u root -p < database/fazzi_mart.sql
   ```
   If you skip this, `DBConnection.java` runs the identical script
   automatically on the app's first database connection.

### 2. Install VS Code Extensions
Open VS Code → Extensions (Ctrl+Shift+X) and install:
- **Extension Pack for Java** (vscjava.vscode-java-pack)
- **Maven for Java** (vscjava.vscode-maven)
- **Live Server** (ritwickdey.liveserver)

### 3. Run the Backend (Spring Boot)
1. In VS Code: **File → Open Folder** → select the `backend` folder.
2. Wait for Maven to finish downloading dependencies (bottom-right progress).
3. Open `FazziMartApplication.java` and press **Run** (▶) — or use the
   **Spring Boot Dashboard**, or in the terminal:
   ```
   cd backend
   mvn spring-boot:run
   ```
4. The whole store runs at **http://localhost:9090** — the backend serves
   the frontend pages *and* the REST API from this single URL (embedded
   Tomcat — no external Tomcat server needed).
   - First launch creates the DB tables (if missing) and seeds the **admin user**
     and **16 sample products**.

### 4. Login
- Register a new account, **or** use the seeded admin:
  - 📧 `admin@fazzimart.com`
  - 🔑 `Admin@123`

After login you land on the **Home page**.

> Optional: if you prefer to develop the frontend with Live Server instead,
> open the `frontend` folder, right-click `index.html` → **Open with Live
> Server** (port 5500). The frontend auto-detects this and calls the API
> on 9090.

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

## 🚀 Deployment

The JAR is self-contained (frontend bundled at `classpath:/static/`). Database
credentials are read from the environment variables `DB_URL` / `DB_USER` /
`DB_PASSWORD` (highest priority), so no config file is needed on a server.

### Option A — Docker Compose (MySQL 8 + app, one command)

```bash
docker compose up --build
# admin@fazzimart.com / Admin@123  at http://localhost:9090
```

### Option B — Run the built JAR anywhere

```bash
cd backend
mvn clean package -DskipTests
DB_URL='jdbc:mysql://HOST:3306/fazzimart?createDatabaseIfNotExist=true&useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8' \
DB_USER=root \
DB_PASSWORD=... \
java -jar target/fazzi-mart-backend-1.0.0.jar
```

### Option C — Railpack / Railway (free)

The repo ships with `railpack.json`, which tells Railpack exactly how to build
(a Maven build of `backend/pom.xml` + Deploy a JRE 17 image that runs the
JAR). Because `pom.xml` lives in the `backend/` subfolder, Railpack would not
auto-detect Java — the config file makes it explicit:

```bash
git add railpack.json
git commit -m "Add railpack.json build config"
git push
```

Then import the repo in Railway; add the **MySQL** plugin and set `DB_URL`,
`DB_USER`, `DB_PASSWORD` from the plugin's connect values. Railway auto-detect
or explicit service using this repo will build with Railpack and serve
`https://<name>.up.railway.app` (health path `/index.html`).

> Note: a root `Dockerfile` is also present. Platforms that prioritize a
> Dockerfile (e.g. Render) will use it instead; both produce the same JAR.

### Option D — Render.com (free)

1. Push this repo to GitHub (origin is already set to
   `begumfazzilath-a11y/Capstone-project`).
2. Render dashboard → **New → Blueprint instance** → pick the repo
   (uses `render.yaml` + `Dockerfile`).
3. Render has no managed MySQL, so use a free MySQL 8 host (e.g. Aiven,
   Railway, Planetscale) and set these in the service's **Environment**
   after import (`render.yaml` marks them `sync: false`, so they never get
   overwritten on redeploys):
   - `DB_URL` — e.g. `jdbc:mysql://HOST:3306/fazzimart?createDatabaseIfNotExist=true&useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8`
   - `DB_USER`, `DB_PASSWORD`
   - `FAZZI_JWT_SECRET` (optional): any long random signing key

> The app boots even if MySQL is still starting up — the seeder retries
> in the background (idempotent), so the first deploy goes healthy even
> when the database needs a minute. The frontend calls the API via a
> relative `/api` path on deployed hosts, so no CORS/port config is needed.

---

## ❓ Troubleshooting

| Problem | Fix |
|---------|-----|
| `Failed to connect to MySQL` / `Access denied for user` | MySQL 8 is not running or `db.properties` has the wrong username/password |
| `Unknown database 'fazzimart'` | Run `mysql -u root -p < database/fazzi_mart.sql`, or make sure the MySQL user has `CREATE DATABASE` privilege (auto-created on first connect) |
| Nothing loads at `http://localhost:9090` | Backend must be running (`cd backend && mvn spring-boot:run`) |
| Port 9090 in use | Change `server.port` in `application.properties` and update `API_BASE` in `frontend/js/common.js` |
| 401 on cart/checkout | You must be logged in first |
| API works but pages 404 | Start from the `backend` folder so `../frontend/` resolves (the JAR also contains a bundled copy at `classpath:/static/` as a fallback) |
| Two tabs/instances fighting over rows | No issue — MySQL handles concurrent requests itself |