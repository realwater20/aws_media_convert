package com.example.aws_media_convert;

import com.example.aws_media_convert.config.property.MediaConvertProperties;
import com.example.aws_media_convert.type.VodRatioType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AWSMediaConvert {

    private final MediaConvertProperties mediaConvertProperties;

    private final MediaConvertClient mediaConvertClient;

    /**
     * MediaConvert 특정 Job 조회
     * @param jobId
     * @return
     */
    public GetJobResponse getConvertJob(String jobId) {
        GetJobRequest jobRequest = GetJobRequest.builder()
                                                .id(jobId)
                                                .build();

        return mediaConvertClient.getJob(jobRequest);
    }

    /**
     * MediaConvert 상태별 Job 리스트 조회
     * @param jobStatus
     * @param limit
     * @return
     */
    public ListJobsResponse getConvertJobList(JobStatus jobStatus, int limit) {
        ListJobsRequest jobsRequest = ListJobsRequest.builder()
                                                    .maxResults(limit)
                                                    .status(jobStatus)
                                                    .build();

        return mediaConvertClient.listJobs(jobsRequest);
    }

    /**
     * MediaConvert CreateJob 생성
     * @param fileName
     * @return
     */
    public CreateJobResponse createConvertJob(String fileName, VodRatioType vodRatioType, boolean excludeAudio) {
        String fileInput = mediaConvertProperties.getInputBucket() + "/" + mediaConvertProperties.getInputBucketPath() + "/" + fileName;
        String fileOutput = mediaConvertProperties.getOutputBucket() + "/" + mediaConvertProperties.getOutputBucketPath() + "/" + fileName.split("\\.")[0];

        Output hlsLow = null;
        Output hlsMedium = null;
        Output hlsHigh = null;

        // 트랜스코딩시 영상 비율이 가로(HORIZONTAL) 또는 세로(VERTICAL)인지 체크
        // 1080p (4000kbps), 720p (2000kbps), 180p (270kbps)
        if (vodRatioType.equals(VodRatioType.VERTICAL)) {
            hlsLow = createOutput("_180",270000,7,180,320, excludeAudio);
            hlsMedium = createOutput( "_720",2000000,7,720,1280, excludeAudio);
            hlsHigh = createOutput( "_1080",4000000,9,1080,1920, excludeAudio);
        } else {
            hlsLow = createOutput("_180",270000,7,320,180, excludeAudio);
            hlsMedium = createOutput( "_720",2000000,7,1280,720, excludeAudio);
            hlsHigh = createOutput( "_1080",4000000,9,1920,1080, excludeAudio);
        }

        OutputGroup appleHLS = OutputGroup.builder().name("Apple HLS").customName("HLS")
                .outputGroupSettings(OutputGroupSettings.builder().type(OutputGroupType.HLS_GROUP_SETTINGS)
                        .hlsGroupSettings(HlsGroupSettings.builder()
                                .directoryStructure(HlsDirectoryStructure.SINGLE_DIRECTORY)
                                .manifestDurationFormat(HlsManifestDurationFormat.INTEGER)
                                .streamInfResolution(HlsStreamInfResolution.INCLUDE)
                                .clientCache(HlsClientCache.ENABLED)
                                .captionLanguageSetting(HlsCaptionLanguageSetting.OMIT)
                                .manifestCompression(HlsManifestCompression.NONE)
                                .codecSpecification(HlsCodecSpecification.RFC_4281)
                                .outputSelection(HlsOutputSelection.MANIFESTS_AND_SEGMENTS)
                                .programDateTime(HlsProgramDateTime.EXCLUDE).programDateTimePeriod(600)
                                .timedMetadataId3Frame(HlsTimedMetadataId3Frame.PRIV).timedMetadataId3Period(10)
                                .destination(fileOutput).segmentControl(HlsSegmentControl.SEGMENTED_FILES)
                                .minFinalSegmentLength((double) 0).segmentLength(4).minSegmentLength(0).build())
                        .build())
                .outputs(hlsLow, hlsMedium, hlsHigh).build();

        Map<String, AudioSelector> audioSelectors = new HashMap<String, AudioSelector>();
        audioSelectors.put("Audio Selector 1",
                AudioSelector.builder().defaultSelection(AudioDefaultSelection.DEFAULT).offset(0).build());

        JobSettings jobSettings = JobSettings.builder().inputs(Input.builder().audioSelectors(audioSelectors)
                        .videoSelector(VideoSelector.builder().colorSpace(ColorSpace.FOLLOW).rotate(InputRotate.DEGREE_0).build())
                        .filterEnable(InputFilterEnable.AUTO).filterStrength(0).deblockFilter(InputDeblockFilter.DISABLED)
                        .denoiseFilter(InputDenoiseFilter.DISABLED).psiControl(InputPsiControl.USE_PSI)
                        .timecodeSource(InputTimecodeSource.EMBEDDED).fileInput(fileInput).build())
                .outputGroups(appleHLS).build();

        CreateJobRequest createJobRequest = CreateJobRequest.builder()
                                                        .role(mediaConvertProperties.getRoleArn())
                                                        .settings(jobSettings).build();

        return mediaConvertClient.createJob(createJobRequest);
    }

    private final static Output createOutput(String nameModifier,
                                             int bitrate,
                                             int qvbrQualityLevel,
                                             int targetWidth,
                                             int targetHeight,
                                             boolean excludeAudio) {
        Output.Builder output = null;
        output = Output.builder().nameModifier(nameModifier).outputSettings(OutputSettings.builder()
                        .hlsSettings(HlsSettings.builder().audioGroupId("program_audio")
                                .iFrameOnlyManifest(HlsIFrameOnlyManifest.EXCLUDE).build())
                        .build())
                        .containerSettings(ContainerSettings.builder().container(ContainerType.M3_U8)
                        .m3u8Settings(M3u8Settings.builder().audioFramesPerPes(4)
                                .pcrControl(M3u8PcrControl.PCR_EVERY_PES_PACKET).pmtPid(480).privateMetadataPid(503)
                                .programNumber(1).patInterval(0).pmtInterval(0).scte35Source(M3u8Scte35Source.NONE)
                                .scte35Pid(500).nielsenId3(M3u8NielsenId3.NONE).timedMetadata(TimedMetadata.NONE)
                                .timedMetadataPid(502).videoPid(481)
                                .audioPids(482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492).build())
                        .build())
                        .videoDescription(
                            VideoDescription.builder().width(targetWidth).height(targetHeight)
                                    .scalingBehavior(ScalingBehavior.DEFAULT).sharpness(50).antiAlias(AntiAlias.ENABLED)
                                    .timecodeInsertion(VideoTimecodeInsertion.DISABLED)
                                    .colorMetadata(ColorMetadata.INSERT).respondToAfd(RespondToAfd.NONE)
                                    .afdSignaling(AfdSignaling.NONE).dropFrameTimecode(DropFrameTimecode.ENABLED)
                                    .codecSettings(VideoCodecSettings.builder().codec(VideoCodec.H_264)
                                            .h264Settings(H264Settings.builder()
                                                    .rateControlMode(H264RateControlMode.QVBR)
                                                    .parControl(H264ParControl.INITIALIZE_FROM_SOURCE)
                                                    .qualityTuningLevel(H264QualityTuningLevel.SINGLE_PASS)
                                                    .qvbrSettings(H264QvbrSettings.builder()
                                                            .qvbrQualityLevel(qvbrQualityLevel).build())
                                                    .codecLevel(H264CodecLevel.AUTO)
                                                    .codecProfile(H264CodecProfile.MAIN)
                                                    .maxBitrate(bitrate)
//                                                    .bitrate(bitrate)
                                                    .framerateControl(H264FramerateControl.INITIALIZE_FROM_SOURCE)
                                                    .gopSize(2.0)
                                                    .gopSizeUnits(H264GopSizeUnits.SECONDS)
                                                    .numberBFramesBetweenReferenceFrames(2).gopClosedCadence(1)
                                                    .gopBReference(H264GopBReference.DISABLED)
                                                    .slowPal(H264SlowPal.DISABLED).syntax(H264Syntax.DEFAULT)
                                                    .numberReferenceFrames(3)
                                                    .dynamicSubGop(H264DynamicSubGop.STATIC)
                                                    .fieldEncoding(H264FieldEncoding.PAFF)
                                                    .sceneChangeDetect(H264SceneChangeDetect.ENABLED).minIInterval(0)
                                                    .telecine(H264Telecine.NONE)
                                                    .framerateConversionAlgorithm(
                                                            H264FramerateConversionAlgorithm.DUPLICATE_DROP)
                                                    .entropyEncoding(H264EntropyEncoding.CABAC).slices(1)
                                                    .unregisteredSeiTimecode(H264UnregisteredSeiTimecode.DISABLED)
                                                    .repeatPps(H264RepeatPps.DISABLED)
                                                    .adaptiveQuantization(H264AdaptiveQuantization.HIGH)
                                                    .spatialAdaptiveQuantization(
                                                            H264SpatialAdaptiveQuantization.ENABLED)
                                                    .temporalAdaptiveQuantization(
                                                            H264TemporalAdaptiveQuantization.ENABLED)
                                                    .flickerAdaptiveQuantization(
                                                            H264FlickerAdaptiveQuantization.DISABLED)
                                                    .softness(0).interlaceMode(H264InterlaceMode.PROGRESSIVE).build())
                                            .build())
                                    .build())
                        ;
        if (excludeAudio == true) {
            return output.build();
        } else {
            return output.audioDescriptions(AudioDescription.builder().audioTypeControl(AudioTypeControl.FOLLOW_INPUT)
                            .languageCodeControl(AudioLanguageCodeControl.FOLLOW_INPUT)
                            .codecSettings(AudioCodecSettings.builder().codec(AudioCodec.AAC).aacSettings(AacSettings
                                            .builder().codecProfile(AacCodecProfile.LC).rateControlMode(AacRateControlMode.CBR)
                                            .codingMode(AacCodingMode.CODING_MODE_2_0).sampleRate(44100).bitrate(128000)
                                            .rawFormat(AacRawFormat.NONE).specification(AacSpecification.MPEG4)
                                            .audioDescriptionBroadcasterMix(AacAudioDescriptionBroadcasterMix.NORMAL).build())
                                    .build())
                            .build())
                    .build();
        }
    }

}
