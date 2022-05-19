package com.example.aws_media_convert;

import com.example.aws_media_convert.type.VodRatioType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.mediaconvert.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AWSMediaConvertTest {

    @Autowired
    private AWSMediaConvert awsMediaConvert;

    @Test
    void getConvertJob() {
        // 특정 job id 조회한다.
        String jobId = "JOB ID";
        GetJobResponse convertJob = awsMediaConvert.getConvertJob(jobId);
        System.out.println("convertJob.job().status() = " + convertJob.job().status());
    }

    @Test
    void getConvertJobList() {
        // COMPLETE 상태인 작업 10개를 조회한다.
        ListJobsResponse listJobsResponse = awsMediaConvert.getConvertJobList(JobStatus.COMPLETE, 2);
        List<Job> jobs = listJobsResponse.jobs();
        for (Job job : jobs) {
            System.out.println("job.status() = " + job.status());
        }
    }

    @Test
    void createConvertJob() {
        // Job을 생성한다.
        String fileName = "fileName";
        VodRatioType ratioType = VodRatioType.HORIZONTAL; // 가로 또는 세로
        boolean exlcudeAudio = false; // 오디오 음향 제외 여부

        CreateJobResponse convertJob = awsMediaConvert.createConvertJob(fileName, ratioType, exlcudeAudio);
        System.out.println("convertJob.job().id() = " + convertJob.job().id());
    }
}
