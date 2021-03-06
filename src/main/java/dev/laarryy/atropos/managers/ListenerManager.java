package dev.laarryy.atropos.managers;

import dev.laarryy.atropos.listeners.EventListener;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

public class ListenerManager {
    private final Logger logger = LogManager.getLogger(this);

    public void registerListeners(GatewayDiscordClient client) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // Register event listeners
        Reflections reflections2 = new Reflections("dev.laarryy.atropos.listeners",
                new MethodAnnotationsScanner());
        Set<Method> listenersToRegister = reflections2.getMethodsAnnotatedWith(EventListener.class);

        for (Method registerableListener : listenersToRegister) {
            Object listener;
            if (registerableListener.getDeclaringClass().isInstance(LoggingListener.class)) {
                listener = LoggingListenerManager.getManager().getLoggingListener();
            } else {
                listener = registerableListener.getDeclaringClass().getDeclaredConstructor().newInstance();
            }
            Parameter[] params = registerableListener.getParameters();

            if (params.length == 0) {
                logger.error("You have a listener with no parameters!");
                continue;
            }

            Class<? extends Event> type = (Class<? extends Event>) params[0].getType();

            client.getEventDispatcher().on(type)
                    .flatMap(event -> {
                        try {
                            Mono<Void> voidMono = Mono.from((Mono<Void>) registerableListener.invoke(listener, event))
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
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(logger::error);
        }

        logger.info("Registered Listeners!");
    }
}
