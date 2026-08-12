package com.vetautet.app.application.auth.dto;


import java.io.Serializable;

/**
 * DTO chứa thông tin API phục vụ việc so khớp quyền động tại bộ lọc Security.
 * Triển khai Serializable để có thể lưu trữ trực tiếp vào Redis Cache dễ dàng.
 */
public record EndpointDto(
        String httpMethod,  // Ví dụ: "GET", "POST", "*"
        String urlPattern   // Ví dụ: "/api/v1/orders/**"
) implements Serializable {
    // Khuyến nghị thêm serialVersionUID nếu bạn lưu cấu trúc này xuống Redis lâu dài
    private static final long serialVersionUID = 1L;
}