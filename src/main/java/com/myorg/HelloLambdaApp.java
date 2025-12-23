package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;


public class HelloLambdaApp {
    public static void main(final String[] args) {
        App app = new App();

        new HelloLambdaStack(app, "HelloLambdaStack", StackProps.builder()
                .env(Environment.builder()
                        .account("549487691798")
                        .region("us-east-1")
                        .build())
                .build());

        app.synth();
    }
}

