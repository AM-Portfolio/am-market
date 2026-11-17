package com.am.marketdata.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InfluxConfigurationValidatorTest {

    private InfluxConfigurationValidator createValidator(String url, String token, String org, String bucket) {
        InfluxConfigurationValidator validator = new InfluxConfigurationValidator();
        ReflectionTestUtils.setField(validator, "url", url);
        ReflectionTestUtils.setField(validator, "token", token);
        ReflectionTestUtils.setField(validator, "org", org);
        ReflectionTestUtils.setField(validator, "bucket", bucket);
        return validator;
    }

    @Test
    void validate_withStandardHttpsUrlWithoutExplicitPort_shouldSucceed() {
        // Dev environment Vault secret injection: "https://influx.asrax.in"
        InfluxConfigurationValidator validator = createValidator(
                "https://influx.asrax.in",
                "test-token",
                "test-org",
                "test-bucket"
        );
        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_withStandardHttpUrlWithoutExplicitPort_shouldSucceed() {
        InfluxConfigurationValidator validator = createValidator(
                "http://influx.internal",
                "test-token",
                "test-org",
                "test-bucket"
        );
        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_withExplicitPort_shouldSucceed() {
        // Preprod environment Vault secret injection: "https://influxdb.asrax.in:443"
        InfluxConfigurationValidator validator = createValidator(
                "https://influxdb.asrax.in:443",
                "test-token",
                "test-org",
                "test-bucket"
        );
        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_withCustomPort_shouldSucceed() {
        InfluxConfigurationValidator validator = createValidator(
                "http://localhost:8086",
                "test-token",
                "test-org",
                "test-bucket"
        );
        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_withMissingOrBlankUrl_shouldThrowIllegalStateException() {
        InfluxConfigurationValidator validator = createValidator(
                "",
                "test-token",
                "test-org",
                "test-bucket"
        );
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validate_withMissingToken_shouldThrowIllegalStateException() {
        InfluxConfigurationValidator validator = createValidator(
                "https://influx.asrax.in",
                "",
                "test-org",
                "test-bucket"
        );
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validate_withMissingOrg_shouldThrowIllegalStateException() {
        InfluxConfigurationValidator validator = createValidator(
                "https://influx.asrax.in",
                "test-token",
                "",
                "test-bucket"
        );
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validate_withMissingBucket_shouldThrowIllegalStateException() {
        InfluxConfigurationValidator validator = createValidator(
                "https://influx.asrax.in",
                "test-token",
                "test-org",
                ""
        );
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validate_withMalformedUrl_shouldThrowIllegalStateException() {
        InfluxConfigurationValidator validator = createValidator(
                "://bad-url",
                "test-token",
                "test-org",
                "test-bucket"
        );
        assertThrows(IllegalStateException.class, validator::validate);
    }
}
