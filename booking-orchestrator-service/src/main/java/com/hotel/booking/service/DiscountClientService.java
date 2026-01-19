package com.hotel.booking.service;

import com.hotel.grpc.discount.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * ✅ КЛИЕНТСКАЯ обёртка для gRPC вызовов с Circuit Breaker
 * Создаём в booking-orchestrator-service
 */
@Service
public class DiscountClientService {

    private static final Logger log = LoggerFactory.getLogger(DiscountClientService.class);
    private static final long GRPC_DEADLINE_SECONDS = 5;

    @GrpcClient("discount-service")
    private DiscountServiceGrpc.DiscountServiceBlockingStub discountServiceStub;

    @CircuitBreaker(name = "discount-service", fallbackMethod = "calculateDiscountFallback")
    public DiscountResponse calculateDiscount(DiscountRequest request) {
        log.debug("🔄 gRPC вызов calculateDiscount для booking: {}", request.getBookingId());

        return discountServiceStub
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .calculateDiscount(request);
    }

    private DiscountResponse calculateDiscountFallback(DiscountRequest request, Exception ex) {
        log.warn("🔄 Circuit Breaker FALLBACK для booking: {}. Причина: {}",
                request.getBookingId(), ex.getMessage());

        return DiscountResponse.newBuilder()
                .setBookingId(request.getBookingId())
                .setDiscountPercentage(0.0)
                .setFinalPrice(request.getBasePrice())
                .setDiscountReason("Сервис скидок временно недоступен")
                .setApplied(false)
                .build();
    }

    @CircuitBreaker(name = "discount-service", fallbackMethod = "getRecommendationsFallback")
    public RecommendationResponse getRecommendations(RecommendationRequest request) {
        log.debug("🔄 gRPC вызов getRecommendations для user: {}", request.getUserId());

        return discountServiceStub
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getRecommendations(request);
    }

    private RecommendationResponse getRecommendationsFallback(RecommendationRequest request, Exception ex) {
        log.warn("🔄 Recommendations FALLBACK для user: {}", request.getUserId());

        return RecommendationResponse.newBuilder()
                .addAllRecommendedHotelIds(Collections.emptyList())
                .setMessage("Рекомендации временно недоступны")
                .build();
    }
}
