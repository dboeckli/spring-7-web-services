package ch.dboeckli.soap.service.log;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class OpenTelemetryAppenderInitializer implements InitializingBean {

    private final ObjectProvider<OpenTelemetry> openTelemetry;

    public OpenTelemetryAppenderInitializer(ObjectProvider<OpenTelemetry> openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetry instance = this.openTelemetry.getIfAvailable();
        if (instance != null) {
            OpenTelemetryAppender.install(instance);
        }
    }

}
