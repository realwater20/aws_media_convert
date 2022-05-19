package com.example.aws_media_convert.config;

import com.example.aws_media_convert.config.property.MediaConvertProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.DescribeEndpointsRequest;
import software.amazon.awssdk.services.mediaconvert.model.DescribeEndpointsResponse;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaConvertConfig {

    private final MediaConvertProperties mediaConvertProperties;

    @Bean
    public AwsCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(mediaConvertProperties.getAccessKey(),
                                                                           mediaConvertProperties.getSecretKey()));
    }

    @Bean
    public MediaConvertClient mediaConvertClient(AwsCredentialsProvider credentialsProvider) {
        // media convert에 대한 endpoint를 취득한다. (필수!)
        DescribeEndpointsResponse describeEndpoints = MediaConvertClient.builder()
                                                            .credentialsProvider(credentialsProvider)
                                                            .region(Region.AP_NORTHEAST_2)
                                                            .build()
                                                            .describeEndpoints(DescribeEndpointsRequest.builder().maxResults(20).build());

        return MediaConvertClient.builder()
                                .credentialsProvider(credentialsProvider)
                                .endpointOverride(URI.create(describeEndpoints.endpoints().get(0).url()))
                                .region(Region.AP_NORTHEAST_2)
                                .build();
    }

}
