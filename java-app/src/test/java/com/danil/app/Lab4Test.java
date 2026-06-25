package com.danil.app;

import com.danil.app.domain.Product;
import com.danil.app.server.databases.Db;
import com.danil.app.server.databases.SQLiteDb;

import org.junit.jupiter.api.*;
import java.io.File;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Lab4Test {
    private Db db;
    private final String TEST_DB = "jdbc:sqlite:test.db";

    @BeforeAll
    void setup() {
        db = new SQLiteDb(TEST_DB);
        db.addGroup("Food");
        db.addGroup("Gadgets");
    }

    @AfterAll
    void cleanup() {
        new File("test.db").delete();
    }

    @Test
    @DisplayName("Тест основних операцій")
    void testCrud() {
        Product p = new Product(0, "iPhone", "Gadgets", 1000.0, 5);
        int id = db.create(p);
        
        // тест зчитування
        Product found = db.getById(id).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("iPhone");

        // тест редагування
        found.setPrice(900.0);
        db.edit(found);
        assertThat(db.getById(id).get().getPrice()).isEqualTo(900.0);

        // тест видалення
        db.delete(id);
        assertThat(db.getById(id)).isEmpty();
    }

    @Test
    @DisplayName("Тест динамічних фільтрів та пагінації")
    void testSearchAndPagination() {
        db.create(new Product(0, "Apple", "Food", 10.0, 100));
        db.create(new Product(0, "Apricot", "Food", 20.0, 50));
        db.create(new Product(0, "Banana", "Food", 30.0, 30));
        db.create(new Product(0, "Bread", "Food", 15.0, 20));
        db.create(new Product(0, "TV", "Gadgets", 500.0, 5));
        db.create(new Product(0, "Smartwatch", "Gadgets", 200.0, 15));

        // тест "м'якого" пошуку (за назвою, через LIKE)
        List<Product> byName = db.search("Ap", null, null, null, 1, 10);
        assertThat(byName).hasSize(2); // Apple, Apricot

        // пошук за категорією та ціною
        List<Product> byPrice = db.search(null, "Gadgets", 300.0, null, 1, 10);
        assertThat(byPrice).hasSize(1);

        // тест пагінації
        List<Product> page2 = db.search(null, "Food", null, null, 2, 2);
        assertThat(page2).hasSize(2);
        
        List<Product> page1 = db.search(null, "Food", null, null, 1, 2);
        assertThat(page1.get(0).getId()).isNotEqualTo(page2.get(0).getId());
    }
}