package com.smart_plant.smart_plant.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtils() {
    }

    public static String now() {
        return LocalDateTime.now().format(DEFAULT_FORMATTER);
    }
}
