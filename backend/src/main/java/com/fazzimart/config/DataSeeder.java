package com.fazzimart.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fazzimart.entity.Product;
import com.fazzimart.entity.User;
import com.fazzimart.repository.ProductRepository;
import com.fazzimart.repository.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      ProductRepository productRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedProducts();
    }

    private void seedAdmin() {
        if (!userRepository.existsByEmail("admin@fazzimart.com")) {
            User admin = new User();
            admin.setName("FAZZI Admin");
            admin.setPhone("09171234567");
            admin.setEmail("admin@fazzimart.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("[Seeder] Admin user created: admin@fazzimart.com / Admin@123");
        }
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        createProduct("Premium Dog Food (Chicken & Rice)", "Nutritious dry dog food packed with real chicken, brown rice, vitamins and omega fatty acids for a shiny coat and strong immunity.", "Pet Food", "1299.00", "images/dog-food.svg", 35, "4.8");
        createProduct("Deluxe Cat Food (Salmon Recipe)", "Premium salmon-based cat food rich in taurine, proteins and DHA to support healthy eyes, heart and digestion.", "Pet Food", "1199.00", "images/cat-food.svg", 28, "4.7");
        createProduct("Puppy Starter Food", "Specially formulated growth food for puppies up to 12 months with calcium, DHA and antioxidants for bone and brain development.", "Pet Food", "1450.00", "images/puppy-food.svg", 22, "4.6");
        createProduct("Adult Dog Food (Beef Formula)", "High-protein beef formula for adult dogs that provides lasting energy, lean muscle support and healthy skin.", "Pet Food", "1399.00", "images/adult-dog-food.svg", 30, "4.5");
        createProduct("Pet Shampoo (Anti-Dandruff)", "Gentle, hypoallergenic shampoo with oatmeal and aloe vera that cleans, deodorizes and soothes itchy skin.", "Pet Products", "249.00", "images/pet-shampoo.svg", 40, "4.4");
        createProduct("Grooming Brush Set", "Professional 3-piece grooming set with deshedding brush, slicker brush and comb for a tangle-free shiny coat.", "Pet Products", "299.00", "images/grooming-brush.svg", 25, "4.6");
        createProduct("Pet Dental Kit (Toothbrush & Toothpaste)", "Double-sided toothbrush and chicken-flavored toothpaste that fights plaque, tartar and bad breath.", "Pet Products", "349.00", "images/pet-toothbrush.svg", 18, "4.3");
        createProduct("Pet Nail Clipper", "Safezone nail clipper with a quick-stop guard to prevent over-cutting - perfect for dogs and cats.", "Pet Products", "199.00", "images/nail-clipper.svg", 0, "4.2");
        createProduct("Adjustable Dog Collar", "Durable, comfortable nylon collar with quick-release buckle and reflective stitching for night walks.", "Pet Accessories", "259.00", "images/dog-collar.svg", 32, "4.7");
        createProduct("Stainless Steel Pet Bowl", "Non-toxic, rust-free steel bowl with anti-slip rubber base and non-spill rim - available in two sizes.", "Pet Accessories", "349.00", "images/pet-bowl.svg", 45, "4.6");
        createProduct("Dog Leash (Heavy Duty)", "Strong 1.2m nylon leash with padded handle and heavy-duty metal clasp for full control on every walk.", "Pet Accessories", "249.00", "images/dog-leash.svg", 38, "4.5");
        createProduct("Deluxe Pet Bed", "Ultra-soft plush bed with removable, machine-washable cover and non-slip bottom for cozy naps.", "Pet Accessories", "899.00", "images/pet-bed.svg", 12, "4.8");
        createProduct("Rubber Ball (Sturdy)", "Durable, bite-resistant rubber ball that bounces high and floats in water - great for fetch at the park.", "Pet Toys", "149.00", "images/rubber-ball.svg", 50, "4.5");
        createProduct("Rope Tug Toy", "Braided cotton rope toy that cleans teeth and massages gums while your dog plays tug-of-war.", "Pet Toys", "179.00", "images/rope-toy.svg", 42, "4.4");
        createProduct("Squeaky Duck Toy", "Soft plush duck with a built-in squeaker that keeps your pet entertained for hours.", "Pet Toys", "159.00", "images/squeaky-toy.svg", 36, "4.3");
        createProduct("Plush Catnip Mouse", "Irresistible plush mouse stuffed with 100% organic catnip that drives your cat wild with fun.", "Pet Toys", "129.00", "images/plush-mouse.svg", 26, "4.6");

        System.out.println("[Seeder] 16 sample products created");
    }

    private void createProduct(String name, String description, String category,
                               String price, String imageUrl, int stock, String rating) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        product.setImageUrl(imageUrl);
        product.setStock(stock);
        product.setRating(new BigDecimal(rating));
        productRepository.save(product);
    }
}