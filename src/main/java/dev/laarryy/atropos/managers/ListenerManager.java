package dev.laarryy.atropos.managers;

import dev.laarryy.atropos.listeners.EventListener;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

public class ListenerManager {
    private final Logger logger = LogManager.getLogger(this);

    public Mono<Void> registerListeners(GatewayDiscordClient client) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // Register event listeners
        Reflections reflections2 = new Reflections("dev.laarryy.atropos.listeners",
                new MethodAnnotationsScanner());
        Flux<Method> listenersToRegister = Flux.fromIterable(reflections2.getMethodsAnnotatedWith(EventListener.class));

        Mono<Void> registerListeners = Flux.from(listenersToRegister)
                .mapNotNull(listenerMethod -> {
                    Object listener;
                    if (listenerMethod.getDeclaringClass().isInstance(LoggingListener.class)) {
                        listener = LoggingListenerManager.getManager().getLoggingListener();
                    } else {
                        try {
                            listener = listenerMethod.getDeclaringClass().getDeclaredConstructor().newInstance();
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    Parameter[] params = listenerMethod.getParameters();

                    if (params.length == 0) {
                        logger.error("You have a listener with no parameters!");
                        return null;
                    }

                    Class<? extends Event> type = (Class<? extends Event>) params[0].getType();

                    return client.getEventDispatcher().on(type)
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(event -> {
                                try {
                                    Mono<Void> voidMono = Mono.from((Mono<Void>) listenerMethod.invoke(listener, event))
                                            .onErrorResume(e -> {
                                                logger.error(e.getMessage());
                                                logger.error("Error in Listener: ", e);
                                                return Mono.empty();
                                            });
                                    //.log()
                                    //.doFinally(signalType -> logger.info("Done Listener"));
                                    return voidMono;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return Mono.empty();
                            });
                })
                .then();


        logger.info("Registered Listeners!");

        return Mono.when(registerListeners);
    }
}
