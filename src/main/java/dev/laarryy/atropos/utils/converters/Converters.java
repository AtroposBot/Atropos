package dev.laarryy.atropos.utils.converters;

import discord4j.common.util.Snowflake;
import org.jooq.Converter;

import java.time.Instant;
import java.util.regex.Pattern;

public interface Converters {

    // @formatter:off
    Converter<Long, Snowflake> SNOWFLAKE = Converter.of(Long.class, Snowflake.class, Snowflake::of, Snowflake::asLong);
    Converter<Long, Instant> INSTANT = Converter.of(Long.class, Instant.class, Instant::ofEpochMilli, Instant::toEpochMilli);
    Converter<String, Pattern> PATTERN = Converter.of(String.class, Pattern.class, Pattern::compile, Pattern::pattern);
    Converter<Byte, Boolean> BOOLEAN = Converter.of(Byte.class, Boolean.class, b -> b != (byte) 0, bl -> (byte) (bl ? 1 : 0));
    // @formatter:on
}