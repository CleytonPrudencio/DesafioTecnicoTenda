package com.tenda.coupon.api;

import com.tenda.coupon.api.dto.CouponResponse;
import com.tenda.coupon.api.dto.CreateCouponRequest;
import com.tenda.coupon.api.dto.ErrorResponse;
import com.tenda.coupon.application.CouponService;
import com.tenda.coupon.domain.Coupon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping(value = "/coupon", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Coupon", description = "Operações de gerenciamento de cupons (criação, consulta e soft delete)")
public class CouponController {

    private final CouponService service;

    public CouponController(CouponService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Cria um novo cupom",
        description = """
            Cria um cupom aplicando as regras de negócio:
            - `code` é alfanumérico; caracteres especiais são removidos antes de salvar e o resultado deve ter exatamente 6 caracteres;
            - `discountValue` é absoluto, com mínimo de 0,5 e sem máximo;
            - `expirationDate` não pode estar no passado;
            - `published=true` permite criar o cupom já publicado.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cupom criado",
            content = @Content(schema = @Schema(implementation = CouponResponse.class),
                examples = @ExampleObject(name = "criado", value = """
                    {
                      "id": 1,
                      "code": "ABC123",
                      "description": "Cupom de boas-vindas",
                      "discountValue": 10.00,
                      "expirationDate": "2099-12-31",
                      "published": false,
                      "deleted": false,
                      "createdAt": "2026-05-31T12:00:00Z"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Falha de validação ou de regra de negócio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CouponResponse> create(
        @Valid @RequestBody CreateCouponRequest request,
        UriComponentsBuilder uriBuilder
    ) {
        Coupon created = service.create(request.toCommand());
        var location = uriBuilder.path("/coupon/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(CouponResponse.from(created));
    }

    @GetMapping
    @Operation(
        summary = "Lista todos os cupons",
        description = "Retorna todos os cupons ordenados do mais recente para o mais antigo, incluindo os deletados (com a flag `deleted=true`)."
    )
    @ApiResponse(responseCode = "200", description = "Lista de cupons",
        content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = CouponResponse.class))))
    public List<CouponResponse> list() {
        return service.list().stream().map(CouponResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cupom pelo identificador")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cupom encontrado",
            content = @Content(schema = @Schema(implementation = CouponResponse.class))),
        @ApiResponse(responseCode = "404", description = "Cupom não encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CouponResponse getById(
        @Parameter(description = "Identificador numérico do cupom", example = "1")
        @PathVariable Long id
    ) {
        return CouponResponse.from(service.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Realiza o soft delete de um cupom",
        description = "Marca o cupom como deletado preservando todos os dados originais. Não é possível deletar um cupom já deletado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cupom deletado (soft delete)"),
        @ApiResponse(responseCode = "404", description = "Cupom não encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Cupom já foi deletado anteriormente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Identificador numérico do cupom", example = "1")
        @PathVariable Long id
    ) {
        service.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
