package com.vetautet.app.application.ordersdemo.usecase;

import com.vetautet.app.domain.ordersdemo.model.Order;
import com.vetautet.app.domain.ordersdemo.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * Produces realistic-looking, non-sensitive synthetic {@link Order} rows for
 * the dev seeder (see {@code SeedOrdersUseCaseImpl}). No external data
 * source - small in-memory sample arrays, no new Faker-style dependency.
 */
@Component
public class FakeOrderDataFactory {

    private static final String[] FIRST_NAMES = {"An", "Binh", "Chi", "Dung", "Giang", "Hoa", "Khanh", "Lan", "Minh", "Nga"};
    private static final String[] LAST_NAMES = {"Nguyen", "Tran", "Le", "Pham", "Hoang", "Vu", "Dang", "Bui", "Do", "Ho"};
    private static final String[] CITIES = {"Ha Noi", "Ho Chi Minh", "Da Nang", "Hai Phong", "Can Tho", "Nha Trang", "Hue", "Vinh", "Bien Hoa", "Vung Tau"};
    private static final String[] PAYMENT_METHODS = {"CREDIT_CARD", "BANK_TRANSFER", "COD", "E_WALLET"};
    private static final String[] CURRENCIES = {"VND", "USD"};
    private static final OrderStatus[] STATUSES = OrderStatus.values();

    private final Random random = new Random();

    public Order generate(UUID id, Instant createdAt) {
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String customerName = lastName + " " + firstName;
        String city = CITIES[random.nextInt(CITIES.length)];
        String emailLocalPart = (firstName + "." + lastName).toLowerCase().replace(" ", "");

        return Order.builder()
                .id(id)
                // Derived from the FULL id, not a slice of it: a UUIDv7's first 8 hex
                // chars are pure timestamp bits (no randomness at all - see
                // Uuid7Generator), so slicing them collided constantly once the
                // seeder generated multiple rows within the same ~65-second window.
                // The full id is guaranteed unique (it's the primary key), so
                // order_code derived from all of it is too.
                .orderCode("ORD-" + id.toString().replace("-", "").toUpperCase())
                .customerName(customerName)
                .customerEmail(emailLocalPart + "@example.com")
                .customerPhone("09" + String.format("%08d", random.nextInt(100_000_000)))
                .status(STATUSES[random.nextInt(STATUSES.length)])
                .totalAmount(BigDecimal.valueOf(10_000 + random.nextInt(4_990_000)).setScale(2, RoundingMode.UNNECESSARY))
                .currency(CURRENCIES[random.nextInt(CURRENCIES.length)])
                .quantity(1 + random.nextInt(5))
                .paymentMethod(PAYMENT_METHODS[random.nextInt(PAYMENT_METHODS.length)])
                .shippingAddress((10 + random.nextInt(990)) + " Le Loi Street")
                .shippingCity(city)
                .notes(null)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
