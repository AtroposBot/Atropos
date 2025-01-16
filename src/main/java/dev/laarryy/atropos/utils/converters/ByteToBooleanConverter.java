package dev.laarryy.atropos.utils.converters;

import org.jooq.impl.AbstractConverter;

public class ByteToBooleanConverter extends AbstractConverter<Byte, Boolean> {
    public ByteToBooleanConverter() {
        super(Byte.class, Boolean.class);
    }

    @Override
    public Byte to(Boolean bool) {
        return bool ? Byte.valueOf((byte) 1) : Byte.valueOf((byte) 0);
    }

    @Override
    public Boolean from(Byte byteValue) {
        return byteValue == 1;
    }
}
