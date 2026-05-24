package com.project.crypto.support;

import org.slf4j.Logger;

public final class AppLog {

    private AppLog() {}

    public static void info(Logger logger, Class<?> clazz, String functionName, String entityLog) {
        logger.info("{} :: {} :: {}", clazz.getSimpleName(), functionName, entityLog);
    }

    public static void warn(Logger logger, Class<?> clazz, String functionName, String entityLog) {
        logger.warn("{} :: {} :: {}", clazz.getSimpleName(), functionName, entityLog);
    }

    public static void debug(Logger logger, Class<?> clazz, String functionName, String entityLog) {
        logger.debug("{} :: {} :: {}", clazz.getSimpleName(), functionName, entityLog);
    }

    public static void error(Logger logger, Class<?> clazz, String functionName, String entityLog) {
        logger.error("{} :: {} :: {}", clazz.getSimpleName(), functionName, entityLog);
    }
}
