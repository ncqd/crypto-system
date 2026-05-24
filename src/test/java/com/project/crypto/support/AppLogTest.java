package com.project.crypto.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class AppLogTest {

    @Test
    void formatsClassMethodAndEntity() {
        Logger logger = (Logger) LoggerFactory.getLogger(AppLogTest.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        AppLog.info(logger, AppLogTest.class, "sample", "User id=1");

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage())
                .isEqualTo("AppLogTest :: sample :: User id=1");
    }
}
