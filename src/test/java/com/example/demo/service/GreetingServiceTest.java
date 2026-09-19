package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GreetingServiceTest {

    private final GreetingService greetingService = new GreetingService();

    @Test
    void greetsGivenName() {
        assertThat(greetingService.greet("Bach")).isEqualTo("Hello, Bach!");
    }

    @Test
    void greetsWorldWhenNameIsNull() {
        assertThat(greetingService.greet(null)).isEqualTo("Hello, world!");
    }

    @Test
    void greetsWorldWhenNameIsBlank() {
        assertThat(greetingService.greet("   ")).isEqualTo("Hello, world!");
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(greetingService.greet("  Bach  ")).isEqualTo("Hello, Bach!");
    }

}
