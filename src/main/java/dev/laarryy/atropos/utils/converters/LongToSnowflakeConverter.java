package dev.laarryy.atropos.utils.converters;

import discord4j.common.util.Snowflake;
import org.jooq.impl.AbstractConverter;

public class LongToSnowflakeConverter extends AbstractConverter<Long, Snowflake> {
    public LongToSnowflakeConverter() {
        super(Long.class, Snowflake.class);
    }

    @Override
    public Snowflake from (Long aLong) {
        return Snowflake.of(aLong);
    }

    @Override
    public Long to (Snowflake snowflake) {
        return snowflake.asLong();
    }

}
