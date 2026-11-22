package com.pragma.franquicias.support;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

// springdoc scans the classpath (reads class files) while it generates the OpenAPI document. That is a cost of a
// third-party library that is only published in local, dev and staging, so generating the document is the one
// place where blocking calls are allowed; BlockHound keeps watching everything else.
public class SpringdocBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder.allowBlockingCallsInside("org.springdoc.api.AbstractOpenApiResource", "getOpenApi");
    }
}
