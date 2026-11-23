package org.covoiturage.testutil;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class ApiAssert {

    private ApiAssert() {
    }

    public static void assertFails(ThrowingCallable call, HttpStatus status, String messagePart) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(status);
                    assertThat(ex.getReason()).contains(messagePart);
                });
    }
}
