package com.sonarshowcase.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link SupplyChainScaController} — uses a benign payload only (no JNDI strings).
 */
@WebMvcTest(controllers = SupplyChainScaController.class)
class SupplyChainScaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void log4jEchoAcceptsBenignPlainText() throws Exception {
        mockMvc.perform(
                        post("/api/v1/sca-demo/log4j-echo")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("logged"));
    }

    @Test
    void log4jEchoEmptyBodyReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/sca-demo/log4j-echo").contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("logged"));
    }
}
