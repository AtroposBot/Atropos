package dev.laarryy.atropos.managers;

import dev.laarryy.atropos.listeners.EventListener;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.utils.ErrorHandler;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class ListenerManager {
    private final Logger logger = LogManager.getLogger(this);

    public Mono<Void> registerListeners(GatewayDiscordClient client) {
        // Register event listeners
        Reflections reflections2 = new Reflections("dev.laarryy.atropos.listeners",
                new MethodAnnotationsScanner());
        Flux<Method> listenersToRegister = Flux.fromIterable(reflections2.getMethodsAnnotatedWith(EventListener.class));

        Mono<Void> registerListeners = Flux.from(listenersToRegister)
                .flatMap(listenerMethod -> {
                    Object listener;
                    if (listenerMethod.getDeclaringClass().isInstance(LoggingListener.class)) {
                        listener = LoggingListenerManager.getManager().getLoggingListener();
                    } else {
                        try {
                            listener = listenerMethod.getDeclaringClass().getDeclaredConstructor().newInstance();
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                 NoSuchMethodException e) {
                            e.printStackTrace();
                            return Mono.empty();
                        }
                    }

                    Parameter[] params = listenerMethod.getParameters();

                    if (params.length == 0) {
                        logger.error("You have a listener with no parameters!");
                        return Mono.empty();
                    }

                    logger.info("Registered Listeners!");

                    Class<? extends Event> type = (Class<? extends Event>) params[0].getType();

                    return client.getEventDispatcher().on(type)
                            .flatMap(event -> {
                                try {
                                    return ((Mono<Void>) listenerMethod.invoke(listener, event))
                                            .onErrorResume(e -> ErrorHandler.handleListenerError(e, event));
                                } catch (Exception e) {
                                    logger.error("Error in Listener");
                                    return Flux.error(new RuntimeException(e));
                                }
                                //.log()
                                //.doFinally(signalType -> logger.info("Done Listener"));

                            });
                })
                .then();

        return Mono.when(registerListeners);
    }
}
