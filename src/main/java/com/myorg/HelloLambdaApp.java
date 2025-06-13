package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;


public class HelloLambdaApp {
    public static void main(final String[] args) {
        App app = new App();

        new HelloLambdaStack(app, "HelloLambdaStack", StackProps.builder()
                .build());

        app.synth();
    }
}

