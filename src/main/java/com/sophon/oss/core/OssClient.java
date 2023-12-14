package com.sophon.oss.core;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.IOUtils;
import com.sophon.oss.constant.OssConstant;
import com.sophon.oss.entity.ObjectProgress;
import com.sophon.oss.entity.UploadResult;
import com.sophon.oss.enumd.AccessPolicyType;
import com.sophon.oss.enumd.PolicyType;
import com.sophon.oss.enumd.ProgressStatusEnum;
import com.sophon.oss.exception.OssException;
import com.sophon.oss.properties.OssProperties;
import com.sophon.oss.service.ObjectProgressService;
import com.sophon.oss.utils.DateUtils;
import com.sophon.oss.utils.FileUtils;
import com.sophon.oss.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * S3 存储协议 所有兼容S3协议的云厂商均支持
 * 阿里云 腾讯云 七牛云 minio
 *
 * @author Lion Li
 */
@Slf4j
public class OssClient {

    private final String configKey;

    private final OssProperties properties;

    private final AmazonS3 client;

    public OssClient(String configKey, OssProperties ossProperties) {
        this.configKey = configKey;
        this.properties = ossProperties;
        try {
            AwsClientBuilder.EndpointConfiguration endpointConfig =
                new AwsClientBuilder.EndpointConfiguration(properties.getEndpoint(), properties.getRegion());

            AWSCredentials credentials = new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey());
            AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
            ClientConfiguration clientConfig = new ClientConfiguration();
            if (OssConstant.IS_HTTPS.equals(properties.getIsHttps())) {
                clientConfig.setProtocol(Protocol.HTTPS);
            } else {
                clientConfig.setProtocol(Protocol.HTTP);
            }
            AmazonS3ClientBuilder build = AmazonS3Client.builder()
                .withEndpointConfiguration(endpointConfig)
                .withClientConfiguration(clientConfig)
                .withCredentials(credentialsProvider)
                .disableChunkedEncoding();
            if (!StringUtils.containsAny(properties.getEndpoint(), OssConstant.CLOUD_SERVICE)) {
                // minio 使用https限制使用域名访问 需要此配置 站点填域名
                build.enablePathStyleAccess();
            }
            this.client = build.build();

            createBucket();
        } catch (Exception e) {
            if (e instanceof OssException) {
                throw e;
            }
            throw new OssException("配置错误! 请检查系统配置:[" + e.getMessage() + "]");
        }
    }

    public void createBucket() {
        try {
            String bucketName = properties.getBucketName();
            if (client.doesBucketExistV2(bucketName)) {
                return;
            }
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            AccessPolicyType accessPolicy = getAccessPolicy();
            createBucketRequest.setCannedAcl(accessPolicy.getAcl());
            client.createBucket(createBucketRequest);
            client.setBucketPolicy(bucketName, getPolicy(bucketName, accessPolicy.getPolicyType()));
        } catch (Exception e) {
            throw new OssException("创建Bucket失败, 请核对配置信息:[" + e.getMessage() + "]");
        }
    }

    public UploadResult upload(byte[] data, String path, String contentType) {
        return upload(new ByteArrayInputStream(data), path, contentType);
    }

    public UploadResult upload(InputStream inputStream, String path, String contentType) {
        if (!(inputStream instanceof ByteArrayInputStream)) {
            inputStream = new ByteArrayInputStream(IoUtil.readBytes(inputStream));
        }
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(inputStream.available());
            PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getBucketName(), path, inputStream, metadata);
            // 设置上传对象的 Acl 为公共读
            putObjectRequest.setCannedAcl(getAccessPolicy().getAcl());
            client.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new OssException("上传文件失败，请检查配置信息:[" + e.getMessage() + "]");
        }
        return UploadResult.builder().url(getUrl() + "/" + path).filename(path).build();
    }

    public UploadResult upload(File file, String path) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getBucketName(), path, file);
            // 设置上传对象的 Acl 为公共读
            putObjectRequest.setCannedAcl(getAccessPolicy().getAcl());
            client.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new OssException("上传文件失败，请检查配置信息:[" + e.getMessage() + "]");
        }
        return UploadResult.builder().url(getUrl() + "/" + path).filename(path).build();
    }

    public void delete(String path) {
        path = path.replace(getUrl() + "/", "");
        try {
            client.deleteObject(properties.getBucketName(), path);
        } catch (Exception e) {
            throw new OssException("删除文件失败，请检查配置信息:[" + e.getMessage() + "]");
        }
    }

    public UploadResult uploadSuffix(byte[] data, String suffix, String contentType) {
        return upload(data, getPath(properties.getPrefix(), suffix), contentType);
    }

    public UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType) {
        return upload(inputStream, getPath(properties.getPrefix(), suffix), contentType);
    }

    public UploadResult uploadSuffix(File file, String suffix) {
        return upload(file, getPath(properties.getPrefix(), suffix));
    }

    /**
     * 获取文件元数据
     *
     * @param path 完整文件路径
     */
    public ObjectMetadata getObjectMetadata(String path) {
        path = path.replace(getUrl() + "/", "");
        S3Object object = client.getObject(properties.getBucketName(), path);
        return object.getObjectMetadata();
    }

    public InputStream getObjectContent(String path) {
        path = path.replace(getUrl() + "/", "");
        S3Object object = client.getObject(properties.getBucketName(), path);
        return object.getObjectContent();
    }

    public String getUrl() {
        String domain = properties.getDomain();
        String endpoint = properties.getEndpoint();
        String header = OssConstant.IS_HTTPS.equals(properties.getIsHttps()) ? "https://" : "http://";
        // 云服务商直接返回
        if (StringUtils.containsAny(endpoint, OssConstant.CLOUD_SERVICE)) {
            if (StringUtils.isNotBlank(domain)) {
                return header + domain;
            }
            return header + properties.getBucketName() + "." + endpoint;
        }
        // minio 单独处理
        if (StringUtils.isNotBlank(domain)) {
            return header + domain + "/" + properties.getBucketName();
        }
        return header + endpoint + "/" + properties.getBucketName();
    }

    public String getPath(String prefix, String suffix) {
        // 生成uuid
        String uuid = IdUtil.fastSimpleUUID();
        // 文件路径
        String path = DateUtils.datePath() + "/" + uuid;
        if (StringUtils.isNotBlank(prefix)) {
            path = prefix + "/" + path;
        }
        return path + suffix;
    }


    public String getConfigKey() {
        return configKey;
    }

    /**
     * 获取私有URL链接
     *
     * @param objectKey 对象KEY
     * @param second    授权时间
     */
    public String getPrivateUrl(String objectKey, Integer second) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
            new GeneratePresignedUrlRequest(properties.getBucketName(), objectKey)
                .withMethod(HttpMethod.GET)
                .withExpiration(new Date(System.currentTimeMillis() + 1000L * second));
        URL url = client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    /**
     * 检查配置是否相同
     */
    public boolean checkPropertiesSame(OssProperties properties) {
        return this.properties.equals(properties);
    }

    /**
     * 获取当前桶权限类型
     *
     * @return 当前桶权限类型code
     */
    public AccessPolicyType getAccessPolicy() {
        return AccessPolicyType.getByType(properties.getAccessPolicy());
    }

    private static String getPolicy(String bucketName, PolicyType policyType) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n\"Statement\": [\n{\n\"Action\": [\n");
        builder.append(switch (policyType) {
            case WRITE -> "\"s3:GetBucketLocation\",\n\"s3:ListBucketMultipartUploads\"\n";
            case READ_WRITE -> "\"s3:GetBucketLocation\",\n\"s3:ListBucket\",\n\"s3:ListBucketMultipartUploads\"\n";
            default -> "\"s3:GetBucketLocation\"\n";
        });
        builder.append("],\n\"Effect\": \"Allow\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
        builder.append(bucketName);
        builder.append("\"\n},\n");
        if (policyType == PolicyType.READ) {
            builder.append("{\n\"Action\": [\n\"s3:ListBucket\"\n],\n\"Effect\": \"Deny\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
            builder.append(bucketName);
            builder.append("\"\n},\n");
        }
        builder.append("{\n\"Action\": ");
        builder.append(switch (policyType) {
            case WRITE -> "[\n\"s3:AbortMultipartUpload\",\n\"s3:DeleteObject\",\n\"s3:ListMultipartUploadParts\",\n\"s3:PutObject\"\n],\n";
            case READ_WRITE -> "[\n\"s3:AbortMultipartUpload\",\n\"s3:DeleteObject\",\n\"s3:GetObject\",\n\"s3:ListMultipartUploadParts\",\n\"s3:PutObject\"\n],\n";
            default -> "\"s3:GetObject\",\n";
        });
        builder.append("\"Effect\": \"Allow\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
        builder.append(bucketName);
        builder.append("/*\"\n}\n],\n\"Version\": \"2012-10-17\"\n}\n");
        return builder.toString();
    }

    public CompletableFuture<UploadResult> putObjectAsyncBatch(MultipartFile file, String batchNo, Executor executor) {

        CompletableFuture<UploadResult> future;
        if (null == executor) {
            future = CompletableFuture.supplyAsync(() -> {
                return highLevelTrackMultipartUpload(file, batchNo);
            });
        } else {
            future = CompletableFuture.supplyAsync(() -> {
                return highLevelTrackMultipartUpload(file, batchNo);
            }, executor);
        }

        future.whenComplete((result, exception) -> {
            if (null != exception) {
                System.err.println("Exception thrown from previous task: " + exception.getMessage());
                future.obtrudeValue(result);
                future.obtrudeException(exception);
            }
        });

        return future;
    }


    public UploadResult highLevelTrackMultipartUpload(MultipartFile file, String batchNo) {

        if (file.isEmpty()) {
            throw new OssException("File文件缺失");
        }

        ObjectProgressService objectProgressService = SpringUtils.getBean(ObjectProgressService.class);

        //此处采用TransferManager上传单个文件，不采用官方多线程，使用自定义线程池控制上传并发数
        //后期再做进一步封装，将监听器剥离
        TransferManager tm = TransferManagerBuilder.standard()
                .withS3Client(client)
                .withMinimumUploadPartSize((long) 500 * 1024 * 1024)
                //todo,本次最大文件限制5G,为了不影响最小个数，容量限制，造成403空间不足
                .withMultipartUploadThreshold((long) 6 * 1024 * 1024 * 1024)
                .build();

        ObjectProgress objectProgress = new ObjectProgress();
        int status = ProgressStatusEnum.SUCCESS.getCode();

        String path = null;
        String nameSuffix = null;
        String streamMD5 = null;
        try {

            String keyName = file.getOriginalFilename();
            long filesize = file.getSize();
            double toatl = Double.longBitsToDouble(filesize);
            log.info("文件大小：" + FileUtils.getNetFileSizeDescription(filesize));

            nameSuffix = keyName.substring(keyName.lastIndexOf("."));

            // 生成uuid
            String uuid = IdUtil.fastSimpleUUID();
            // 文件路径
            path = DateUtils.datePath() + "/" + uuid;
            if (StringUtils.isNotBlank(properties.getPrefix())) {
                path = properties.getPrefix() + "/" + path;
            }
            String ossName = uuid + nameSuffix;
            path += nameSuffix;

            byte[] in = FileUtils.readBytesFromInputStream(file.getInputStream(), 999);
            streamMD5 = DigestUtil.md5Hex(in);

            //获取当前线程名
            Thread threadNow = Thread.currentThread();
            String threadName = threadNow.getName();
            //初始化进度,带线程
            objectProgress.setBucketName(properties.getBucketName());
            objectProgress.setFileName(keyName);
            objectProgress.setPrefix(properties.getPrefix());
            objectProgress.setBatchNo(batchNo);
            objectProgress.setSecRate(0.00);
            objectProgress.setStreamMd5(streamMD5);
            objectProgress.setFileSize(filesize);
            objectProgress.setStatus(ProgressStatusEnum.WAIT.getCode());
            objectProgress.setThreadName(threadName);

            objectProgressService.insertObjectProgress(objectProgress);

            ObjectMetadata metadata = new ObjectMetadata();
            // 必须设置ContentLength
            metadata.setContentLength(filesize);

            //https://stackoverflow.com/questions/46360321/unable-to-reset-stream-after-calculating-aws4-signature
            //此处有坑 不能直接用file.getInputStream()
            PutObjectRequest request = new PutObjectRequest(properties.getBucketName(), path,
                    new ByteArrayInputStream(IOUtils.toByteArray(file.getInputStream())), metadata);
            // 设置上传对象的 Acl 为公共读
            request.setCannedAcl(getAccessPolicy().getAcl());

            Upload upload = null;

            //监听进度
            final String[] finalTossName = {ossName};
            long id = objectProgress.getId();
            String finalStreamMD = streamMD5;
            request.setGeneralProgressListener(new com.amazonaws.event.ProgressListener() {

                long sec = 0;
                double rate = 0.00;

                Date beginTime = DateUtil.date();

                public void progressChanged(ProgressEvent progressEvent) {

                    sec += progressEvent.getBytesTransferred();

                    System.out.println("上传字节：" + sec);

                    Date endTime = DateUtil.date();

                    if (DateUtil.between(beginTime, endTime, DateUnit.SECOND) >= 0.5) {

                        rate = NumberUtil.round((Double.longBitsToDouble(sec) / toatl) * 100, 2).doubleValue();

                        log.info(rate + "%");

                        beginTime = endTime;

                        ObjectProgress objectProgress = new ObjectProgress();
                        objectProgress.setId(id);
                        objectProgress.setBucketName(properties.getBucketName());
                        objectProgress.setPrefix(properties.getPrefix());
                        objectProgress.setFileName(keyName);
                        objectProgress.setBatchNo(batchNo);
                        objectProgress.setFileSize(filesize);
                        objectProgress.setSecRate(rate);
                        objectProgress.setTossName(finalTossName[0]);
                        objectProgress.setStreamMd5(finalStreamMD);
                        objectProgress.setThreadName(threadName);
                        objectProgress.setStatus(ProgressStatusEnum.ING.getCode());

                        objectProgressService.updateObjectProgress(objectProgress);
                    }
                }
            });

            upload = tm.upload(request);

           /* //循环探测 标志
            int oldFlag = 0;
            boolean isDone = upload.isDone();
//            操作标识 0 正常 1挂起 2中断取消
            while (!isDone){

                //TODO 性能瓶颈 后续改为缓存方式
                objectProgress = objectProgressService.getObjectProgressDispById(objectProgress.getId());
                if(null == objectProgress){
                    break;
                }
                int operFlag = objectProgress.getOperateFlag();
                if(oldFlag != operFlag){
                    if(operFlag == OperateTypeEnum.WAIT.getCode()){
                        System.out.println("上传等待");
                        upload.tryPause(true);
                        status = ProgressStatusEnum.WAIT.getCode();
                    }else if(operFlag == OperateTypeEnum.RUN.getCode()){
                        System.out.println("继续上传");
                        upload.tryPause(false);
                        status = ProgressStatusEnum.ING.getCode();
                    }else if(operFlag == OperateTypeEnum.INTERRUPT.getCode()){
                        System.out.println("上传中断");
                        //可以采用抛出异常返回，麻烦，写入中断操作队列，待上传完删除。
                        upload.abort();
                        status = ProgressStatusEnum.INTERRUPT.getCode();
                        break;
                    }
                    oldFlag = operFlag;
                }
              }*/

            upload.waitForCompletion();
            log.info("Object upload complete,进度：100.00%");

            objectProgress.setSecRate(100.00);
        } catch (AmazonServiceException | InterruptedException e) {
            e.printStackTrace();
            //监听进度异常
            status = ProgressStatusEnum.ERROR.getCode();

        } catch (SdkClientException e) {
            e.printStackTrace();
            //监听进度异常
            status = ProgressStatusEnum.ERROR.getCode();
        } catch (IOException e) {
            e.printStackTrace();
            //监听进度异常
            status = ProgressStatusEnum.ERROR.getCode();
        } finally {
            tm.shutdownNow();
            objectProgress.setStatus(status);
            objectProgressService.updateObjectProgress(objectProgress);
        }

        return UploadResult.builder()
                .url(getUrl() + "/" + path)
                .filename(path)
                .originalfileName(file.getOriginalFilename())
                .suffix(nameSuffix)
                .size(file.getSize())
                .md5(streamMD5)
                .configKey(configKey)
                .build();

    }
}
