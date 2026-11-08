package com.pedropacheco.nutrition.backend.foodCatalog.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrandTest {
    @Test
    void trimsAndStoresBrandName() {
        assertEquals("Acme Foods", new Brand("  Acme Foods ").getValue());
    }

    @Test
    void rejectsNullOrBlankBrandName() {
        assertThrows(IllegalArgumentException.class, () -> new Brand(null));
        assertThrows(IllegalArgumentException.class, () -> new Brand("  "));
    }
}
