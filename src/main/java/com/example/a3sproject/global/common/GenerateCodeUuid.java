package com.example.a3sproject.global.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class GenerateCodeUuid {
    // 주문번호 발급 (UUID)
    public static String generateCodeUuid(String code) {
        return code + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    public static String generateUnderCodeUuid(String code) {
        return code + "_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "_" + UUID.randomUUID().toString().replace("_", "").substring(0, 10).toUpperCase();
    }
}
