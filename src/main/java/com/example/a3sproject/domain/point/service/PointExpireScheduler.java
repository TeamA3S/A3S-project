package com.example.a3sproject.domain.point.service;


import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import com.example.a3sproject.global.common.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpireScheduler {

    private final PointRepository pointRepository;
    private final PointExpireBatchService pointExpireBatchService;

    private static final int CHUNK_SIZE = AppConstants.Point.BATCH_CHUNK_SIZE;
    private static final int MAX_RECORDS = AppConstants.Point.BATCH_MAX_RECORDS;
    private static final long CHUNK_DELAY_MS = AppConstants.Point.BATCH_DELAY_MS;

    @Scheduled(cron = "0 0 0 * * *")
    public void expirePoints() {
        log.info("포인트 소멸 스케줄러 시작");

        int totalProcessed = 0;

        // 한 번 실행의 기준 시각을 고정
        LocalDateTime cutoff = LocalDateTime.now();

        while (totalProcessed < MAX_RECORDS) {
            int remain = MAX_RECORDS - totalProcessed;
            int currentChunkSize = Math.min(CHUNK_SIZE, remain);

            Pageable pageable = PageRequest.of(0, currentChunkSize);

            // 항상 0페이지의 상위 10건만 조회
            List<Long> expiredTxIds = pointRepository.findExpiredTransactionIds(
                    PointTransactionType.EARN,
                    cutoff,
                    0,
                    pageable
            );
            // 더 이상 처리할 게 없으면 종료
            if (expiredTxIds.isEmpty()) {
                log.info("더 이상 소멸할 포인트가 없습니다.");
                break;
            }
            log.info("포인트 소멸 배치 시작 - batchSize: {}, totalProcessedBefore: {}, txIds={}",
                    expiredTxIds.size(), totalProcessed, expiredTxIds);

            int processedCount = pointExpireBatchService.processChunk(expiredTxIds);
            totalProcessed += processedCount;

            log.info("포인트 소멸 배치 완료 - processed: {}, totalProcessedAfter: {}",
                    processedCount, totalProcessed);

            // 현재 배치가 꽉 차지 않았다면 더 이상 남은 데이터가 거의 없다는 뜻
            if (expiredTxIds.size() < currentChunkSize) {
                break;
            }
            // 배치 간 간격
            try {
                Thread.sleep(CHUNK_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("포인트 소멸 스케줄러 sleep interrupted");
                break;
            }
        }
        log.info("[포인트 소멸 스케줄러] 완료 - 총 처리 건수: {}", totalProcessed);
    }
}
