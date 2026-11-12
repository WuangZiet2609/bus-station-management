package com.busstation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Cors cors = new Cors();
    private Payment payment = new Payment();
    private Notification notification = new Notification();
    private Schedule schedule = new Schedule();

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
        private List<String> allowedMethods;
        private List<String> allowedHeaders;
        private boolean allowCredentials;
    }

    @Data
    public static class Payment {
        private Vnpay vnpay = new Vnpay();
        private Momo momo = new Momo();

        @Data
        public static class Vnpay {
            private String tmnCode;
            private String hashSecret;
            private String payUrl;
            private String returnUrl;
            private String version;
            private String command;
            private String orderType;
            private String locale;
        }

        @Data
        public static class Momo {
            private String partnerCode;
            private String accessKey;
            private String secretKey;
            private String endpoint;
            private String returnUrl;
            private String notifyUrl;
            private String requestType;
        }
    }

    @Data
    public static class Notification {
        private boolean emailEnabled;
        private boolean smsEnabled;
    }

    @Data
    public static class Schedule {
        private int reminderBeforeDepartureMinutes;
        private int autoCancelUnpaidMinutes;
    }
}