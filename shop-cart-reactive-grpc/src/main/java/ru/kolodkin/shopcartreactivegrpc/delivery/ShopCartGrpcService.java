package ru.kolodkin.shopcartreactivegrpc.delivery;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.devh.boot.grpc.server.service.GrpcService;
import reactor.core.publisher.Mono;
import ru.grpc.shop.cart.service.*;
import ru.kolodkin.shopcartreactivegrpc.interceptors.LogGrpcInterceptor;
import ru.kolodkin.shopcartreactivegrpc.service.ShopCartService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@GrpcService(interceptors = {LogGrpcInterceptor.class})
@Slf4j
@RequiredArgsConstructor
public class ShopCartGrpcService extends ReactorShopCartServiceGrpc.ShopCartServiceImplBase {
    private final ShopCartService shopCartService;
    private static final Long TIMEOUT_MILLIS = 5000L;

    @Override
    public Mono<AddProductInShopCartResponse> addProductInShopCart(Mono<AddProductInShopCartRequest> request) {
        return request.flatMap(req -> shopCartService.addProductInShopCart(req.getUserId(), req.getProductId()))
                .doOnError(error -> log.error("ERROR ADD IN DB: " + error.getMessage()))
                .map(shopCart -> AddProductInShopCartResponse.newBuilder()
                        .setShopCart(ShopCart.newBuilder()
                                .setProductId(shopCart.getProductId())
                                .setQuantity(shopCart.getQuantity())
                                .setUserId(shopCart.getUserId())
                                .build())
                        .build())
                .timeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .doOnError(error -> log.error("ERROR ADD: " + error.getMessage()));
    }

    @Override
    public Mono<GetUserShopCartResponse> getAllShopCart(Mono<GetUserShopCartRequest> request) {
        return request.flatMap(req -> shopCartService.getAllShopCart(req.getUserId())
                .doOnError(error -> log.error("ERROR FROM GET ALL DB: " + error.getMessage()))
                        .collectList()
                        .map(shopCartList -> {
                            List<ShopCartDTO> shopCartDTOList = new ArrayList<>();
                            for (ru.kolodkin.shopcartreactivegrpc.bean.ShopCartDTO shopCartDTO : shopCartList) {
                                shopCartDTOList.add(
                                        ShopCartDTO.newBuilder()
                                                .setProduct(toProductInfoForProto(shopCartDTO.getProduct()))
                                                .setQuantity(shopCartDTO.getQuantity())
                                                .setUserId(shopCartDTO.getUserId())
                                                .build()
                                );
                            }
                            return GetUserShopCartResponse.newBuilder()
                                    .addAllShopCart(shopCartDTOList)
                                    .build();
                        })
                .doOnError(error -> log.error("ERROR CREATE RESPONSE GET ALL: " + error.getMessage())))
                .doOnSuccess(success -> log.info("Получены товары из корзины пользователя"))
                .timeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .doOnError(error -> log.error("ERROR GET ALL PRODUCT: " + error.getMessage()));
    }

    @Override
    public Mono<UpdateProductQuantityInShopCartResponse> updateProductQuantityInShopCart(Mono<UpdateProductQuantityInShopCartRequest> request) {
        return request.flatMap(req -> shopCartService.updateProductQuantityInShopCart(
                                req.getShopCart().getUserId(),
                                req.getShopCartOrBuilder().getProductId(),
                                req.getShopCart().getQuantity())
                        .doOnError(error -> log.error("ERROR UPDATE IN DB: " + error.getMessage()))
                        .then(Mono.fromCallable(() -> UpdateProductQuantityInShopCartResponse.newBuilder()
                                .setStatus("OK")
                                .build()))
                .doOnError(error -> log.error("ERROR UPDATE CREATE RESPONSE: " + error.getMessage())))
                .timeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .doOnError(error -> log.error("ERROR UPDATE: " + error.getMessage()));
    }

    @Override
    public Mono<GetShopCartItemResponse> getShopCartProduct(Mono<GetShopCartItemRequest> request) {
        return request.flatMap(req ->
                shopCartService.getShopCartProduct(req.getUserId(), req.getProductId())
                        .doOnError(error -> log.error("ERROR FROM CATALOG: " + error.getMessage()))
                        .flatMap(shopCartDTOMono ->
                                shopCartDTOMono.map(shopCartDTO ->
                                        GetShopCartItemResponse.newBuilder()
                                                .setShopCart(ShopCartDTO.newBuilder()
                                                        .setProduct(toProductInfoForProto(shopCartDTO.getProduct()))
                                                        .setUserId(shopCartDTO.getUserId())
                                                        .setQuantity(shopCartDTO.getQuantity())
                                                        .build())
                                                .build())
                                        .doOnError(error -> log.error("ERROR CREATE RESPONSE GET PROD: " + error.getMessage()))
                        )
        ).timeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .doOnError(error -> log.error("ERROR PRODUCT: " + error.getMessage()));
    }

    @Override
    public Mono<DeleteResponse> deleteProductFromShopCart(Mono<DeleteProductFromShopCartRequest> request) {
        return request.flatMap(req -> shopCartService.deleteProductFromShopCart(req.getUserId(), req.getProductId()))
                .doOnError(error -> log.error("ERROR DELETE IN DB: " + error.getMessage()))
                .then(Mono.just(DeleteResponse.newBuilder()
                        .setStatus("OK")
                        .build()))
                .doOnError(error -> log.error("ERROR DELETE PRODUCT: " + error.getMessage()));
    }

    @Override
    public Mono<DeleteResponse> deleteProductsFromShopCart(Mono<DeleteProductsFromShopCartRequest> request) {
        return request.flatMap(req -> shopCartService.deleteProductsFromShopCart(req.getUserId()))
                .doOnError(error -> log.error("ERROR DELETE ALL IN DB: " + error.getMessage()))
                .then(Mono.just(DeleteResponse.newBuilder()
                        .setStatus("OK")
                        .build()))
                .doOnError(error -> log.error("ERROR DELETE ALL: " + error.getMessage()));
    }

    private ProductState switchState(ru.kolodkin.shopcartreactivegrpc.bean.ProductInfo.ProductState state) {
        if (state == null) {
            return ProductState.NOT_AVAILABLE;
        }

        return ProductState.AVAILABLE.name().equals(state.name())
                ? ProductState.AVAILABLE : ProductState.NOT_AVAILABLE;
    }

    private ProductInfo toProductInfoForProto(ru.kolodkin.shopcartreactivegrpc.bean.ProductInfo productInfo) {
        val product = ProductInfo.newBuilder()
                .setAmount(productInfo.getAmount())
                .setCategory(CategoryInfo.newBuilder()
                        .setId(productInfo.getId())
                        .setName(productInfo.getName())
                        .build())
                .setId(productInfo.getId())
                .setName(productInfo.getName())
                .setDescription(productInfo.getDescription())
                .setState(switchState(productInfo.getState()))
                .setPrice(productInfo.getPrice().toString());

        productInfo.getAttributes()
                .stream().map(attribute -> AttributeInfo.newBuilder()
                        .setId(attribute.getId())
                        .setName(attribute.getName())
                        .setDescription(attribute.getDescription())
                        .build())
                .forEach(product::addAttributes);

        productInfo.getImages()
                .stream().map(image -> ImageInfo.newBuilder()
                        .setPath(ByteString.copyFrom(image.getPath()))
                        .setSize(image.getSize())
                        .setId(image.getId())
                        .setName(image.getName())
                        .build())
                .forEach(product::addImages);

        return product.build();
    }

    private ShopCartDTO toShopCartDTOForProto(ru.kolodkin.shopcartreactivegrpc.bean.ShopCartDTO shopCartDTO) {
        return ShopCartDTO.newBuilder()
                .setProduct(toProductInfoForProto(shopCartDTO.getProduct()))
                .setQuantity(shopCartDTO.getQuantity())
                .setUserId(shopCartDTO.getUserId())
                .build();
    }
}

