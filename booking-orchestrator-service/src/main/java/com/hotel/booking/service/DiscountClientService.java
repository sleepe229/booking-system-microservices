package com.hotel.booking.service;

import com.hotel.grpc.discount.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class DiscountClientService {

    private static final Logger log = LoggerFactory.getLogger(DiscountClientService.class);
    private static final long GRPC_DEADLINE_SECONDS = 5;

    @GrpcClient("discount-service")
    private DiscountServiceGrpc.DiscountServiceBlockingStub discountServiceStub;

    @CircuitBreaker(name = "discount-service", fallbackMethod = "calculateDiscountFallback")
    public DiscountResponse calculateDiscount(DiscountRequest request) {
        log.debug(" gRPC вызов calculateDiscount для booking: {}", request.getBookingId());

        return discountServiceStub
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .calculateDiscount(request);
    }

    @CircuitBreaker(name = "discount-service", fallbackMethod = "getRecommendationsFallback")
    public RecommendationResponse getRecommendations(RecommendationRequest request) {
        log.debug(" gRPC вызов getRecommendations для user: {}", request.getUserId());

        return discountServiceStub
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getRecommendations(request);
    }

}
