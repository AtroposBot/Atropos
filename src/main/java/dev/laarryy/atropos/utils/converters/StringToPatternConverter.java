package dev.laarryy.atropos.utils.converters;

import org.jooq.impl.AbstractConverter;

import java.util.regex.Pattern;

public class StringToPatternConverter extends AbstractConverter<String, Pattern> {
    public StringToPatternConverter() {
        super(String.class, Pattern.class);
    }

    @Override
    public String to(Pattern value) {
        return value.toString();
    }

    @Override
    public Pattern from(String value) {
        return Pattern.compile(value);
    }
}
