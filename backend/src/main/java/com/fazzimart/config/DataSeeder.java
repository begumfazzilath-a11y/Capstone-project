package com.fazzimart.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fazzimart.dao.ProductDao;
import com.fazzimart.dao.UserDao;
import com.fazzimart.model.Product;
import com.fazzimart.model.User;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserDao userDao;
    private final ProductDao productDao;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserDao userDao,
                      ProductDao productDao,
                      PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.productDao = productDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Seeds on a background thread with retries so that a MySQL instance that
     * is still starting up (Render / Railway / Docker healthcheck race) never
     * aborts the application boot. Both seed methods are idempotent, so retries
     * are safe. If the database can never be reached within MAX_ATTEMPTS, the
     * thread stops trying and logs a clear error - the web app still serves.
     */
    @Override
    public void run(String... args) {
        Thread seeder = new Thread(this::seedWithRetry, "fazzimart-seeder");
        seeder.setDaemon(true);
        seeder.start();
    }

    private void seedWithRetry() {
        final int maxAttempts = 60;
        final long retryDelayMs = 5000;
        for (int attempt = 1; ; attempt++) {
            try {
                seedAdmin();
                seedProducts();
                return;
            } catch (RuntimeException e) {
                if (attempt >= maxAttempts) {
                    System.err.println("[Seeder] Giving up after " + attempt
                            + " attempts - is MySQL reachable? Check DB_URL / DB_USER / DB_PASSWORD: "
                            + e.getMessage());
                    return;
                }
                if (attempt == 1 || attempt % 6 == 0) {
                    System.err.println("[Seeder] MySQL not ready yet (attempt " + attempt
                            + "/" + maxAttempts + "), retrying in " + (retryDelayMs / 1000)
                            + "s: " + e.getMessage());
                }
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void seedAdmin() {
        if (!userDao.existsByEmail("admin@fazzimart.com")) {
            User admin = new User();
            admin.setName("FAZZI Admin");
            admin.setPhone("09171234567");
            admin.setEmail("admin@fazzimart.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole("ADMIN");
            userDao.save(admin);
            System.out.println("[Seeder] Admin user created: admin@fazzimart.com / Admin@123");
        }
    }

    private void seedProducts() {
        if (productDao.count() > 0) {
            return;
        }

        productDao.seed(java.util.List.of(
                product("Premium Dog Food (Chicken & Rice)", "Nutritious dry dog food packed with real chicken, brown rice, vitamins and omega fatty acids for a shiny coat and strong immunity.", "Pet Food", "1299.00", "images/dog-food.svg", 35, "4.8"),
                product("Deluxe Cat Food (Salmon Recipe)", "Premium salmon-based cat food rich in taurine, proteins and DHA to support healthy eyes, heart and digestion.", "Pet Food", "1199.00", "images/cat-food.svg", 28, "4.7"),
                product("Puppy Starter Food", "Specially formulated growth food for puppies up to 12 months with calcium, DHA and antioxidants for bone and brain development.", "Pet Food", "1450.00", "images/puppy-food.svg", 22, "4.6"),
                product("Adult Dog Food (Beef Formula)", "High-protein beef formula for adult dogs that provides lasting energy, lean muscle support and healthy skin.", "Pet Food", "1399.00", "images/adult-dog-food.svg", 30, "4.5"),
                product("Pet Shampoo (Anti-Dandruff)", "Gentle, hypoallergenic shampoo with oatmeal and aloe vera that cleans, deodorizes and soothes itchy skin.", "Pet Products", "249.00", "images/pet-shampoo.svg", 40, "4.4"),
                product("Grooming Brush Set", "Professional 3-piece grooming set with deshedding brush, slicker brush and comb for a tangle-free shiny coat.", "Pet Products", "299.00", "images/grooming-brush.svg", 25, "4.6"),
                product("Pet Dental Kit (Toothbrush & Toothpaste)", "Double-sided toothbrush and chicken-flavored toothpaste that fights plaque, tartar and bad breath.", "Pet Products", "349.00", "images/pet-toothbrush.svg", 18, "4.3"),
                product("Pet Nail Clipper", "Safezone nail clipper with a quick-stop guard to prevent over-cutting - perfect for dogs and cats.", "Pet Products", "199.00", "images/nail-clipper.svg", 0, "4.2"),
                product("Adjustable Dog Collar", "Durable, comfortable nylon collar with quick-release buckle and reflective stitching for night walks.", "Pet Accessories", "259.00", "images/dog-collar.svg", 32, "4.7"),
                product("Stainless Steel Pet Bowl", "Non-toxic, rust-free steel bowl with anti-slip rubber base and non-spill rim - available in two sizes.", "Pet Accessories", "349.00", "images/pet-bowl.svg", 45, "4.6"),
                product("Dog Leash (Heavy Duty)", "Strong 1.2m nylon leash with padded handle and heavy-duty metal clasp for full control on every walk.", "Pet Accessories", "249.00", "images/dog-leash.svg", 38, "4.5"),
                product("Deluxe Pet Bed", "Ultra-soft plush bed with removable, machine-washable cover and non-slip bottom for cozy naps.", "Pet Accessories", "899.00", "images/pet-bed.svg", 12, "4.8"),
                product("Rubber Ball (Sturdy)", "Durable, bite-resistant rubber ball that bounces high and floats in water - great for fetch at the park.", "Pet Toys", "149.00", "images/rubber-ball.svg", 50, "4.5"),
                product("Rope Tug Toy", "Braided cotton rope toy that cleans teeth and massages gums while your dog plays tug-of-war.", "Pet Toys", "179.00", "images/rope-toy.svg", 42, "4.4"),
                product("Squeaky Duck Toy", "Soft plush duck with a built-in squeaker that keeps your pet entertained for hours.", "Pet Toys", "159.00", "images/squeaky-toy.svg", 36, "4.3"),
                product("Plush Catnip Mouse", "Irresistible plush mouse stuffed with 100% organic catnip that drives your cat wild with fun.", "Pet Toys", "129.00", "images/plush-mouse.svg", 26, "4.6")));

        System.out.println("[Seeder] 16 sample products created");
    }

    private Product product(String name, String description, String category,
                            String price, String imageUrl, int stock, String rating) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setCategory(category);
        p.setPrice(new BigDecimal(price));
        p.setImageUrl(imageUrl);
        p.setStock(stock);
        p.setRating(new BigDecimal(rating));
        return p;
    }
}