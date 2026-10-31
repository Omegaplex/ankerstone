package io.github.omegaplex.ankerstone;

import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;

final class TestConsoleLogging {
    private static final Pattern PROGRESS_BAR =
            Pattern.compile("\\[[+Xx_ ]+\\]");

    static void initialize() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);

        for (var appender : context.getConfiguration().getAppenders().values()) {
            if (appender instanceof ConsoleAppender console) {
                console.addFilter(new AbstractFilter() {
                    @Override
                    public Result filter(LogEvent event) {
                        boolean progress = event.getLoggerName().equals(
                                "net.minecraft.gametest.framework.GameTestServer")
                                && PROGRESS_BAR.matcher(
                                        event.getMessage().getFormattedMessage()).matches();

                        return progress ? Result.DENY : Result.NEUTRAL;
                    }
                });
            }
        }
    }
}