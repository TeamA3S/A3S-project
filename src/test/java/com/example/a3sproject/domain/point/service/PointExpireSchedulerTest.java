package com.example.a3sproject.domain.point.service;

import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PointExpireSchedulerTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointExpireBatchService pointExpireBatchService;

    @InjectMocks
    private PointExpireScheduler pointExpireScheduler;

    @Test
    @DisplayName("만료 대상이 10건 미만이면 한 번만 처리하고 종료한다")
    void expirePoints_processOnceAndStop() {
        // given
        List<Long> ids = List.of(1L, 2L, 3L);

        given(pointRepository.findExpiredTransactionIds(
                eq(PointTransactionType.EARN),
                any(LocalDateTime.class),
                eq(0),
                any(Pageable.class)
        )).willReturn(ids);

        given(pointExpireBatchService.processChunk(ids)).willReturn(3);

        // when
        pointExpireScheduler.expirePoints();

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        then(pointRepository).should(times(1)).findExpiredTransactionIds(
                eq(PointTransactionType.EARN),
                any(LocalDateTime.class),
                eq(0),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);

        then(pointExpireBatchService).should(times(1)).processChunk(ids);
    }

    @Test
    @DisplayName("만료 대상이 없으면 배치 서비스를 호출하지 않는다")
    void expirePoints_noTargets() {
        // given
        given(pointRepository.findExpiredTransactionIds(
                eq(PointTransactionType.EARN),
                any(LocalDateTime.class),
                eq(0),
                any(Pageable.class)
        )).willReturn(List.of());

        // when
        pointExpireScheduler.expirePoints();

        // then
        then(pointExpireBatchService).should(never()).processChunk(anyList());
    }
}