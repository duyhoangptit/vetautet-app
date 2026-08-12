package com.vetautet.app.presentation.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration
 * Presentation layer configuration
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Management API - Clean Architecture")
                        .version("1.0.0")
                        .description("RESTful API for User Management built with Clean Architecture principles")
                        .contact(new Contact()
                                .name("FSOFT Development Team")
                                .email("support@fsoft.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token for authentication")))
                /*
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))*/;
    }

    /**
     * Định nghĩa logic thêm header X-Portal (Tách thành 1 method bổ trợ, không dùng @Bean)
     * Thêm cơ chế check null an toàn tuyệt đối để tránh lỗi BeanCreationException.
     */
    private OpenApiCustomizer xPortalHeaderCustomizer() {
        return openApi -> {
//            if (openApi.getPaths() != null) {
//                openApi.getPaths().values().stream()
//                        .filter(pathItem -> pathItem.readOperations() != null)
//                        .flatMap(pathItem -> pathItem.readOperations().stream())
//                        .forEach(operation -> operation.addParametersItem(
//                                new HeaderParameter()
//                                        .name("X-Portal")
//                                        .description("Mã định danh Portal truy cập (Ví dụ: WEB, MOBILE)")
//                                        .required(true)
//                                        .schema(new StringSchema().example("A"))));
//            }
        };
    }

    // Nhóm v1
    @Bean
    public GroupedOpenApi apiV1() {
        return GroupedOpenApi.builder()
                .group("v1-apis")
                .displayName("API (V1)")
                .pathsToMatch("/api/v1/**")
                .addOpenApiCustomizer(xPortalHeaderCustomizer())
                .build();
    }

    // Nhóm v2
    @Bean
    public GroupedOpenApi apiV2() {
        return GroupedOpenApi.builder()
                .group("v2-apis")
                .displayName("API (V2)")
                .pathsToMatch("/api/v2/**")
                .addOpenApiCustomizer(xPortalHeaderCustomizer())
                .build();
    }

    // Nhóm internal
    @Bean
    public GroupedOpenApi apiInternal() {
        return GroupedOpenApi.builder()
                .group("internal-apis")
                .displayName("API (Internal)")
                .pathsToMatch("/api/internal/**")
                .addOpenApiCustomizer(xPortalHeaderCustomizer())
                .build();
    }
}

