package com.example.demo.controller;

import com.example.demo.service.GreetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
@Import(GreetingService.class)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsGreetingWithoutName() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, world!"));
    }

    @Test
    void returnsGreetingWithName() throws Exception {
        mockMvc.perform(get("/api/hello").param("name", "Bach"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Bach!"));
    }

}
