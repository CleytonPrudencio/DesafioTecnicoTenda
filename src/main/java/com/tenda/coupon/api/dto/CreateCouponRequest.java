package com.tenda.coupon.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tenda.coupon.application.CreateCouponCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CreateCouponRequest", description = "Payload de criação de cupom")
public record CreateCouponRequest(
    @Schema(
        description = "Código alfanumérico. Caracteres especiais são removidos pela aplicação antes de salvar; após a remoção deve restar exatamente 6 caracteres. Máximo de 60 caracteres na entrada bruta.",
        example = "ABC123",
        minLength = 1,
        maxLength = 60,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "code is required")
    @Size(max = 60, message = "code must have at most 60 characters")
    String code,

    @Schema(
        description = "Descrição livre do cupom. Máximo de 255 caracteres.",
        example = "Cupom de boas-vindas",
        minLength = 1,
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "description is required")
    @Size(max = 255, message = "description must have at most 255 characters")
    String description,

    @Schema(
        description = "Valor absoluto do desconto. Mínimo 0,5; sem máximo.",
        example = "10.00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "discountValue is required")
    BigDecimal discountValue,

    @Schema(
        description = "Data de expiração no formato ISO-8601 (YYYY-MM-DD). Não pode estar no passado.",
        example = "2099-12-31",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "expirationDate is required")
    LocalDate expirationDate,

    @Schema(
        description = "Indica se o cupom já é criado publicado. Default: false.",
        example = "false",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        defaultValue = "false"
    )
    Boolean published
) {
    public CreateCouponCommand toCommand() {
        return new CreateCouponCommand(
            code,
            description,
            discountValue,
            expirationDate,
            Boolean.TRUE.equals(published)
        );
    }
}
