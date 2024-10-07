package com.co.fundmanagement.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryTest {

    @Test
    public void testCategoryBuilder() {
        Category category = Category.builder()
                .id("123")
                .name("Equity Fund")
                .acronym("EF")
                .build();

        assertNotNull(category);
        assertEquals("123", category.getId());
        assertEquals("Equity Fund", category.getName());
        assertEquals("EF", category.getAcronym());
    }

    @Test
    public void testCategoryNoArgsConstructor() {
        Category category = new Category();

        assertNotNull(category);
        assertNull(category.getId());
        assertNull(category.getName());
        assertNull(category.getAcronym());
    }

    @Test
    public void testCategoryAllArgsConstructor() {
        Category category = new Category("123", "Equity Fund", "EF");

        assertNotNull(category);
        assertEquals("123", category.getId());
        assertEquals("Equity Fund", category.getName());
        assertEquals("EF", category.getAcronym());
    }
}
