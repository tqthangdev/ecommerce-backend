package com.dev.ecommerce.support;

import org.testcontainers.DockerClientFactory;

public final class DockerConditions {

    private DockerConditions() {
    }

    public static boolean isAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
