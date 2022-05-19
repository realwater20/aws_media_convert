package com.example.aws_media_convert.config.property;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Getter
@Setter
@ConfigurationProperties("aws.mediaconvert")
public class MediaConvertProperties {

    private String inputBucket;

    private String inputBucketPath;

    private String outputBucket;

    private String outputBucketPath;

    private String roleArn;

    private String accessKey;

    private String secretKey;

}
