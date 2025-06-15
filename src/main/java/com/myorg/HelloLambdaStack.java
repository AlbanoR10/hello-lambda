package com.myorg;

import software.amazon.awscdk.BundlingOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import software.amazon.awscdk.BundlingOptions;

import software.amazon.awscdk.services.s3.assets.AssetOptions;

import java.util.List;
import java.nio.file.Path;

public class HelloLambdaStack extends Stack {
    public HelloLambdaStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public HelloLambdaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        LayerVersion commonLayer = LayerVersion.Builder.create(this, "CommonJavaLibs")
                .layerVersionName("java-libs")
                .compatibleRuntimes(List.of(Runtime.JAVA_21))
                .code(Code.fromAsset("lambda-layer",      // dir donde está tu pom.xml del layer
                        AssetOptions.builder()
                                .bundling(BundlingOptions.builder()
                                        .image(Runtime.JAVA_21.getBundlingImage()) // usa Corretto 21
                                        .user("root")                              // permite escribir en /asset-output
                                        .command(List.of(
                                                "bash","-c", String.join(" && ",
                                                        // 1. Construir el uber-jar
                                                        "mvn -q clean install",
                                                        // 2. Crear la ruta esperada y copiarlo
                                                        "mkdir -p /asset-output/java/lib",
                                                        "cp target/layer-java-layer-*.jar /asset-output/java/lib/"
                                                )))
                                        .outputType(BundlingOutput.NOT_ARCHIVED)   // CDK se ocupa del .zip
                                        .build())
                                .build()))
                .build();

        Function hola = Function.Builder.create(this, "HolaFn")
                .runtime(Runtime.JAVA_21)
                .architecture(Architecture.ARM_64)
                .memorySize(512)
                .handler("albano.Handler::handleRequest")
                .code(Code.fromAsset(
                        // CDK copiará TODO el directorio lambda-java al contenedor de build
                        Path.of("lambda-java").toString(),
                        AssetOptions.builder()
                                .bundling(BundlingOptions.builder()
                                        .image(Runtime.JAVA_21.getBundlingImage())  // imagen oficial de build
                                        .user("root")
                                        .command(List.of(
                                                "bash", "-c",
                                                // compila y deja el JAR en /asset-output
                                                "mvn -q package && cp target/lambda-java-*.jar /asset-output/"
                                        ))
                                        .build())
                                .build()))
                .layers(List.of(commonLayer))
                .build();

        Function adios = Function.Builder.create(this, "AdiosFn")
                .runtime(Runtime.JAVA_21)
                .architecture(Architecture.ARM_64)
                .memorySize(512)
                .handler("albano.AdiosHandler::handleRequest")
                .code(Code.fromAsset(
                        // CDK copiará TODO el directorio lambda-java al contenedor de build
                        Path.of("lambda-java").toString(),
                        AssetOptions.builder()
                                .bundling(BundlingOptions.builder()
                                        .image(Runtime.JAVA_21.getBundlingImage())  // imagen oficial de build
                                        .user("root")
                                        .command(List.of(
                                                "bash", "-c",
                                                // compila y deja el JAR en /asset-output
                                                "mvn -q package && cp target/lambda-java-*.jar /asset-output/"
                                        ))
                                        .build())
                                .build()))
                .layers(List.of(commonLayer))
                .build();

        RestApi restApi = RestApi.Builder.create(this, "HelloApi")
                .restApiName("HelloApi")
                .deployOptions(StageOptions.builder().stageName("prod").build())
                .build();

        restApi.getRoot()
                .addResource("hola")
                .addMethod("ANY", new LambdaIntegration(hola));

        restApi.getRoot()
                .addResource("adios")
                .addMethod("ANY", new LambdaIntegration(adios));


    }
}
