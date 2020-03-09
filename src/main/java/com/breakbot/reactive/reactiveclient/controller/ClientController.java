package com.breakbot.reactive.reactiveclient.controller;

import com.breakbot.reactive.reactiveclient.constants.Constants;
import com.breakbot.reactive.reactiveclient.domain.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ClientController {
    WebClient webClient = WebClient.create("http://localhost:8080");

    @GetMapping("/client/retreive")
    public Flux<Item> getAllItems(){
        return webClient.get().uri(Constants.ITEM_END_POINT_V1).retrieve().bodyToFlux(Item.class).log("Items on the db");
    }
    @GetMapping("/client/retreiveExeption")
    public Flux<Item> getItemsWithException(){
        return webClient.get().uri(Constants.RUNTIME_EXCEPTION).retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> {
            Mono<String> mono = clientResponse.bodyToMono(String.class);
            return mono.flatMap(errorMesage -> {
                log.error(errorMesage);
                throw new RuntimeException(errorMesage);
            } );
        }).bodyToFlux(Item.class);
    }
    @GetMapping("/client/exchangeExeption")
    public Flux<Item> getItemsWithException(){
        return webClient.get().uri(Constants.RUNTIME_EXCEPTION).exchange().flatMapMany(
                clientResponse -> {
                    clientResponse.statusCode().is5xxServerError(){
                        return clientResponse.bodyToMono(String.class).flatMap(errorMsg -> {log.error("eeeee=" + errorMsg);
                        throw new RuntimeException()})
                    }
                }
        );
    }

    @GetMapping("/client/exchange")
    public Flux<Item> getAllItemsExchange(){
        return webClient.get().uri(Constants.ITEM_END_POINT_V1).exchange().flatMapMany(clientResponse -> clientResponse.bodyToFlux(Item.class)).log("Items on the db");
    }

    @GetMapping("/client/retreive/singleItem")
    public Mono<Item> getOneItem(@PathVariable String item){
        return webClient.get().uri(Constants.ITEM_END_POINT_V1 + "/{id}",item).retrieve().bodyToMono(Item.class).log("item returned: ");
    }

    @PostMapping("/client/addItem")
    public Mono<Item> addItem(@RequestBody Item item){
        System.out.println("logging...");
        Mono<Item> monoItem = Mono.just(item);
        return webClient.post().uri(Constants.LOAD_ONE_ITEM_V1).contentType(MediaType.APPLICATION_JSON).body(monoItem, Item.class).retrieve()
                .bodyToMono(Item.class).log("Created item: ");

    }

}
