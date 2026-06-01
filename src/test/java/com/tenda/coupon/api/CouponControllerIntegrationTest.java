package com.tenda.coupon.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenda.coupon.infrastructure.persistence.CouponEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CouponControllerIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-05-31T12:00:00Z");
    private static final LocalDate FUTURE = LocalDate.of(2099, 12, 31);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @PersistenceContext
    EntityManager em;

    @BeforeEach
    void cleanDatabase() {
        em.createQuery("delete from CouponEntity").executeUpdate();
    }

    @Test
    void POST_coupon_cria_cupom_com_201_e_Location() throws Exception {
        Map<String, Object> body = Map.of(
            "code", "ABC-123",
            "description", "Boas-vindas",
            "discountValue", "10.00",
            "expirationDate", FUTURE.toString(),
            "published", false
        );

        mvc.perform(post("/coupon").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", notNullValue()))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.code", equalTo("ABC123")))
            .andExpect(jsonPath("$.description", equalTo("Boas-vindas")))
            .andExpect(jsonPath("$.discountValue", equalTo(10.00)))
            .andExpect(jsonPath("$.expirationDate", equalTo(FUTURE.toString())))
            .andExpect(jsonPath("$.published", equalTo(false)))
            .andExpect(jsonPath("$.deleted", equalTo(false)));
    }

    @Test
    void POST_coupon_cria_publicado_quando_published_true() throws Exception {
        Map<String, Object> body = Map.of(
            "code", "ABC123",
            "description", "x",
            "discountValue", "5",
            "expirationDate", FUTURE.toString(),
            "published", true
        );
        mvc.perform(post("/coupon").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.published", equalTo(true)));
    }

    @Test
    void POST_coupon_rejeita_codigo_invalido_com_400() throws Exception {
        Map<String, Object> body = Map.of(
            "code", "AB",
            "description", "x",
            "discountValue", "5",
            "expirationDate", FUTURE.toString()
        );
        mvc.perform(post("/coupon").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", equalTo(400)))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void POST_coupon_rejeita_desconto_abaixo_do_minimo_com_400() throws Exception {
        Map<String, Object> body = Map.of(
            "code", "ABC123",
            "description", "x",
            "discountValue", "0.10",
            "expirationDate", FUTURE.toString()
        );
        mvc.perform(post("/coupon").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", equalTo("discountValue must be greater than or equal to 0.5 (got 0.10)")));
    }

    @Test
    void POST_coupon_rejeita_data_passada_com_400() throws Exception {
        LocalDate past = LocalDate.now(Clock.fixed(NOW, ZoneOffset.UTC)).minusDays(1);
        Map<String, Object> body = Map.of(
            "code", "ABC123",
            "description", "x",
            "discountValue", "1",
            "expirationDate", past.toString()
        );
        mvc.perform(post("/coupon").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", equalTo("expirationDate cannot be in the past (got " + past + ", today is 2026-05-31)")));
    }

    @Test
    void POST_coupon_rejeita_payload_invalido_com_violations() throws Exception {
        Map<String, Object> body = Map.of();
        mvc.perform(post("/coupon").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.violations", hasSize(4)));
    }

    @Test
    void POST_coupon_rejeita_description_acima_de_255_chars() throws Exception {
        String longDescription = "x".repeat(256);
        Map<String, Object> body = Map.of(
            "code", "ABC123",
            "description", longDescription,
            "discountValue", "1",
            "expirationDate", FUTURE.toString()
        );
        mvc.perform(post("/coupon").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.violations[0].field", equalTo("description")));
    }

    @Test
    void POST_coupon_rejeita_code_acima_de_60_chars() throws Exception {
        String longCode = "A".repeat(61);
        Map<String, Object> body = Map.of(
            "code", longCode,
            "description", "x",
            "discountValue", "1",
            "expirationDate", FUTURE.toString()
        );
        mvc.perform(post("/coupon").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.violations[0].field", equalTo("code")));
    }

    @Test
    void POST_coupon_rejeita_body_mal_formado_com_400() throws Exception {
        mvc.perform(post("/coupon").contentType(APPLICATION_JSON).content("{not-json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", equalTo("malformed request body")));
    }

    @Test
    void GET_coupon_lista_em_ordem_decrescente_de_id() throws Exception {
        Long id1 = persistedActiveCoupon("AAA111");
        Long id2 = persistedActiveCoupon("BBB222");
        Long id3 = persistedDeletedCoupon();

        mvc.perform(get("/coupon"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].id", equalTo(id3.intValue())))
            .andExpect(jsonPath("$[1].id", equalTo(id2.intValue())))
            .andExpect(jsonPath("$[2].id", equalTo(id1.intValue())))
            .andExpect(jsonPath("$[0].deleted", equalTo(true)));
    }

    @Test
    void GET_coupon_lista_vazia_quando_sem_cupons() throws Exception {
        mvc.perform(get("/coupon"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void GET_coupon_por_id_retorna_o_cupom() throws Exception {
        Long id = persistedActiveCoupon("ABC123");
        mvc.perform(get("/coupon/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", equalTo(id.intValue())))
            .andExpect(jsonPath("$.code", equalTo("ABC123")));
    }

    @Test
    void GET_coupon_por_id_retorna_404_quando_nao_existe() throws Exception {
        mvc.perform(get("/coupon/{id}", 9_999_999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", equalTo("Coupon not found: id=9999999")));
    }

    @Test
    void DELETE_coupon_realiza_soft_delete_com_204() throws Exception {
        Long id = persistedActiveCoupon("ABC123");
        mvc.perform(delete("/coupon/{id}", id))
            .andExpect(status().isNoContent());

        CouponEntity reloaded = em.find(CouponEntity.class, id);
        assert reloaded.isDeleted();
        assert reloaded.getDeletedAt() != null;
    }

    @Test
    void DELETE_coupon_retorna_404_quando_nao_existe() throws Exception {
        mvc.perform(delete("/coupon/{id}", 9_999_999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", equalTo(404)))
            .andExpect(jsonPath("$.message", equalTo("Coupon not found: id=9999999")));
    }

    @Test
    void DELETE_coupon_retorna_409_quando_ja_deletado() throws Exception {
        Long id = persistedDeletedCoupon();
        mvc.perform(delete("/coupon/{id}", id))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", equalTo(409)))
            .andExpect(jsonPath("$.message", equalTo("Coupon already deleted: id=" + id)));
    }

    @Test
    void DELETE_coupon_retorna_400_quando_id_invalido() throws Exception {
        mvc.perform(delete("/coupon/{id}", "not-a-number"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    private Long persistedActiveCoupon(String code) {
        CouponEntity e = new CouponEntity(
            null, code, "Desc", new BigDecimal("10.00"),
            FUTURE, false, false, NOW, null
        );
        em.persist(e);
        em.flush();
        return e.getId();
    }

    private Long persistedDeletedCoupon() {
        CouponEntity e = new CouponEntity(
            null, "DEF456", "Desc", new BigDecimal("10.00"),
            FUTURE, false, true, NOW, NOW
        );
        em.persist(e);
        em.flush();
        return e.getId();
    }
}
