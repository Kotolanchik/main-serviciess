/*
package ru.kolodkin.shopcartreactivegrpc.service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ShopCartService {
    ShopCartRepository repository;
    Client client;

    public Flux<ShopCartDTO> getShopCartByUserId(final Long userId) {
        return shopCartListToDTOList(repository.findAllByUserId(userId));
    }

    public Mono<ShopCartDTO> getShopCartByUserIdAndProductId(final Long userId, final Long productId) {
        return shopCartToDTO(repository.findByUserIdAndProductId(userId, productId));
    }

    public Mono<ShopCart> addProductInCart(final Long userId, final Long productId){
        return repository.save(ShopCart.builder()
                .productId(productId)
                .userId(userId)
                .quantity(1L).build());
    }

    public Mono<ShopCartSimpleDTO> setShopCart(final Long userId, final Long productId, final Long quantity) {
 return repository.findByUserIdAndProductId(userId, productId)
                .flatMap(shopCart -> {

                    return repository.updateQuantity(userId, productId, quantity);
                })
                .defaultIfEmpty(ShopCart.builder()
                        .userId(userId)
                        .productId(productId)
                        .quantity(quantity)
                        .build()
                ).flatMap(repository::save)
                .map(ShopCart::getProductId);


        return repository.updateQuantityByUserIdAndProductId(userId, productId,quantity)
                .map(shopCart -> ShopCartSimpleDTO.builder()
                        .productId(shopCart.getProductId())
                        .userId(shopCart.getUserId())
                        .quantity(shopCart.getQuantity())
                        .build());

  return repository.findByUserIdAndProductId(userId, productId)
                .flatMap(shopCart -> {
                    shopCart.setQuantity(quantity);
                    return repository.save(shopCart);
                });

    }

    public Mono<Void> deleteProductFromCart(final Long userId, final Long productId) {
        return repository.deleteByUserIdAndProductId(userId, productId);
    }

    public Mono<Void> deleteProductsFromCart(final Long userId) {
        return repository.deleteAllByUserId(userId);
    }

    private Flux<ShopCartDTO> shopCartListToDTOList(final Flux<ShopCart> shopCartFlux) {
        return client.getProductsByIds(shopCartFlux
                        .map(ShopCart::getProductId))
                .collectMap(ProductInfo::getId, productInfo -> productInfo)
                .flatMapMany(productMap ->
                        shopCartFlux.flatMap(shopCart -> {
                            ProductInfo productInfo = productMap.get(shopCart.getProductId());

                            if (productInfo != null) {
                                return Mono.just(ShopCartDTO.builder()
                                        .product(productInfo)
                                        .quantity(shopCart.getQuantity())
                                        .userId(shopCart.getUserId())
                                        .build());
                            } else {
                                return Mono.empty();
                            }
                        })
                );
    }

    private Mono<ShopCartDTO> shopCartToDTO(final Mono<ShopCart> shopCartMono) {
        return shopCartMono.flatMap(shopCart ->
                client.getProductById(shopCart.getProductId())
                        .flatMap(productInfo ->
                                Mono.just(ShopCartDTO.builder()
                                        .product(productInfo)
                                        .userId(shopCart.getUserId())
                                        .quantity(shopCart.getQuantity())
                                        .build())
                        )
        );
    }
}
*/