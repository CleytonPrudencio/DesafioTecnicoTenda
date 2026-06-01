package com.tenda.coupon.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tenda.coupon.domain.Coupon;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CouponResponse", description = "Representação de um cupom")
public record CouponResponse(
    @Schema(description = "Identificador numérico do cupom", example = "1") Long id,
    @Schema(description = "Código alfanumérico (6 caracteres) já sanitizado", example = "ABC123") String code,
    @Schema(description = "Descrição livre do cupom", example = "Cupom de boas-vindas") String description,
    @Schema(description = "Valor absoluto do desconto", example = "10.00") BigDecimal discountValue,
    @Schema(description = "Data de expiração (ISO-8601)", example = "2099-12-31") LocalDate expirationDate,
    @Schema(description = "Indica se o cupom está publicado") boolean published,
    @Schema(description = "Indica se o cupom foi deletado (soft delete)") boolean deleted,
    @Schema(description = "Instante de criação (ISO-8601, UTC)") Instant createdAt,
    @Schema(description = "Instante do soft delete; null se não deletado") Instant deletedAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
            coupon.id(),
            coupon.code().value(),
            coupon.description().value(),
            coupon.discountValue().value(),
            coupon.expirationDate().value(),
            coupon.isPublished(),
            coupon.isDeleted(),
            coupon.createdAt(),
            coupon.deletedAt()
        );
    }
}
