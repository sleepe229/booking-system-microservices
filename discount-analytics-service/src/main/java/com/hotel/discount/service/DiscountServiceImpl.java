package com.hotel.discount.service;

import com.hotel.grpc.discount.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

@GrpcService
public class DiscountServiceImpl extends DiscountServiceGrpc.DiscountServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(DiscountServiceImpl.class);
    private final Random random = new Random();

    @Override
    public void calculateDiscount(DiscountRequest request,
                                  StreamObserver<DiscountResponse> responseObserver) {
        log.info("Получен запрос на расчет скидки для booking_id: {}", request.getBookingId());

        try {
            if (request.getBasePrice() <= 0 || request.getNights() <= 0) {
                log.error(" Невалидные входные данные: basePrice={}, nights={}",
                        request.getBasePrice(), request.getNights());
                responseObserver.onError(
                        io.grpc.Status.INVALID_ARGUMENT
                                .withDescription("basePrice и nights должны быть > 0")
                                .asException()
                );
                return;
            }

            double discountPercentage = 0.0;
            String discountReason = "Скидка не применена";
            boolean applied = false;

            if (request.getIsLoyalCustomer()) {
                discountPercentage = 10.0 + random.nextDouble() * 5.0;
                discountReason = "Скидка для лояльного клиента";
                applied = true;
            } else if (request.getNights() >= 7) {
                discountPercentage = 5.0;
                discountReason = "Скидка за длительное проживание";
                applied = true;
            } else if (random.nextDouble() < 0.1) {
                discountPercentage = 7.0;
                discountReason = "Специальное промо-предложение";
                applied = true;
            }

            discountPercentage = Math.min(discountPercentage, 100.0);

            double finalPrice = request.getBasePrice();
            if (applied) {
                finalPrice = request.getBasePrice() * (1 - discountPercentage / 100.0);
            }

            finalPrice = Math.max(finalPrice, 0.0);

            DiscountResponse response = DiscountResponse.newBuilder()
                    .setBookingId(request.getBookingId())
                    .setDiscountPercentage(discountPercentage)
                    .setFinalPrice(finalPrice)
                    .setDiscountReason(discountReason)
                    .setApplied(applied)
                    .build();

            log.info(" Скидка рассчитана: {}% ({}), финальная цена: {}",
                    discountPercentage, discountReason, finalPrice);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error(" Ошибка при расчёте скидки", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Внутренняя ошибка при расчёте скидки")
                            .asException()
            );
        }
    }

    @Override
    public void getRecommendations(RecommendationRequest request,
                                   StreamObserver<RecommendationResponse> responseObserver) {
        log.info("Получен запрос рекомендаций для user_id: {}", request.getUserId());

        try {
            request.getUserId();
            if (request.getUserId().isEmpty()) {
                responseObserver.onError(
                        io.grpc.Status.INVALID_ARGUMENT
                                .withDescription("userId обязателен")
                                .asException()
                );
                return;
            }

            List<String> recommendations = List.of(
                    "hotel_" + random.nextInt(100),
                    "hotel_" + random.nextInt(100),
                    "hotel_" + random.nextInt(100)
            );

            RecommendationResponse response = RecommendationResponse.newBuilder()
                    .addAllRecommendedHotelIds(recommendations)
                    .setMessage("На основе вашей истории, мы рекомендуем эти отели")
                    .build();

            log.info(" Возвращены рекомендации: {}", recommendations);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error(" Ошибка при получении рекомендаций", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Внутренняя ошибка при получении рекомендаций")
                            .asException()
            );
        }
    }
}
