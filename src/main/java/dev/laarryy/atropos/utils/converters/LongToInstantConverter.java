package dev.laarryy.atropos.utils.converters;

import org.jooq.impl.AbstractConverter;

import java.time.Instant;

public class LongToInstantConverter extends AbstractConverter<Long, Instant> {
    public LongToInstantConverter() {
        super(Long.class, Instant.class);
    }

    @Override
    public Long to(Instant instant) {
        return instant.toEpochMilli();
    }

    @Override
    public Instant from(Long aLong) {
        return Instant.ofEpochMilli(aLong);
    }
}
