package com.orphanage.oms.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable pagination response matching the API contract (avoids serializing PageImpl).
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last,
        boolean empty
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty());
    }
}
