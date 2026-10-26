package com.project.gainz.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

// Cloudflare R2(S3 호환, 비공개 버킷)에 저장한다 (#262). 내보내는 트래픽이 무료라 큰 영상도 브라우저가 바로 받게 한다.
// 서버는 권한을 확인한 뒤 몇 분짜리 서명 주소(presign)만 내주고, 버킷 키는 서버만 안다.
// gainz.storage.supabase.* 가 있으면 R2에 아직 없는 옛 파일은 Supabase에서 읽는다 (LegacyStorageMigration이 옮기는 동안)
@Component
@ConditionalOnProperty(name = "gainz.storage.type", havingValue = "r2")
public class R2FileStore implements FileStore {

    private final String bucket;
    private final S3Client s3;
    private final S3Presigner presigner;
    private final SupabaseFileStore legacy;

    public R2FileStore(@Value("${gainz.storage.r2.account-id:}") String accountId,
                       @Value("${gainz.storage.r2.bucket}") String bucket,
                       @Value("${gainz.storage.r2.access-key-id}") String accessKeyId,
                       @Value("${gainz.storage.r2.secret-access-key}") String secretAccessKey,
                       // 비우면 https://<account-id>.r2.cloudflarestorage.com (테스트에서 가짜 서버로 바꿀 때만 쓴다)
                       @Value("${gainz.storage.r2.endpoint:}") String endpoint,
                       @Value("${gainz.storage.supabase.url:}") String legacyUrl,
                       @Value("${gainz.storage.supabase.bucket:uploads}") String legacyBucket,
                       @Value("${gainz.storage.supabase.service-key:}") String legacyKey) {
        URI uri = URI.create(endpoint.isBlank() ? "https://" + accountId + ".r2.cloudflarestorage.com" : endpoint);
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        var config = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        this.bucket = bucket;
        this.s3 = S3Client.builder()
                .endpointOverride(uri).region(Region.of("auto")).credentialsProvider(credentials)
                .serviceConfiguration(config)
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(60)))
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(uri).region(Region.of("auto")).credentialsProvider(credentials)
                .serviceConfiguration(config)
                .build();
        this.legacy = legacyUrl.isBlank() || legacyKey.isBlank() ? null : new SupabaseFileStore(legacyUrl, legacyBucket, legacyKey);
    }

    // 옛 파일을 읽는 Supabase (없으면 null). LegacyStorageMigration이 쓴다
    SupabaseFileStore legacy() {
        return legacy;
    }

    // Supabase는 보지 않고 R2에만 있는지
    boolean inR2(String key) throws IOException {
        return r2Size(key) >= 0;
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) throws IOException {
        call(() -> s3.putObject(b -> b.bucket(bucket).key(key).contentType(contentType).contentLength(size),
                RequestBody.fromInputStream(content, size)));
    }

    @Override
    public Resource get(String key) throws IOException {
        long length = r2Size(key);
        if (length >= 0) return new R2Object(key, length);
        return legacy == null ? null : legacy.get(key);
    }

    @Override
    public long size(String key) throws IOException {
        long length = r2Size(key);
        return length >= 0 || legacy == null ? length : legacy.size(key);
    }

    @Override
    public byte[] head(String key, int bytes) throws IOException {
        if (r2Size(key) < 0) return legacy == null ? null : legacy.head(key, bytes);
        return call(() -> s3.getObjectAsBytes(b -> b.bucket(bucket).key(key).range("bytes=0-" + (bytes - 1))).asByteArray());
    }

    // R2 안에서 복사하고 지운다 (서버로 내려받지 않는다)
    @Override
    public void move(String from, String to, String contentType) throws IOException {
        call(() -> s3.copyObject(b -> b.sourceBucket(bucket).sourceKey(from).destinationBucket(bucket).destinationKey(to)
                .contentType(contentType).metadataDirective("REPLACE")));
        delete(from);
    }

    @Override
    public void delete(String key) throws IOException {
        call(() -> s3.deleteObject(b -> b.bucket(bucket).key(key)));
        if (legacy != null) legacy.delete(key);
    }

    // 버킷 안 모든 파일 크기의 합 (목록 한 번에 1,000개씩)
    @Override
    public long totalSize() throws IOException {
        return call(() -> s3.listObjectsV2Paginator(b -> b.bucket(bucket)).contents().stream()
                .mapToLong(S3Object::size).sum());
    }

    @Override
    public String presignPut(String key, String contentType, Duration ttl) {
        return presigner.presignPutObject(p -> p.signatureDuration(ttl)
                .putObjectRequest(b -> b.bucket(bucket).key(key).contentType(contentType))).url().toString();
    }

    // 아직 Supabase에만 있는 옛 파일은 null → 서버가 보내 준다
    @Override
    public String presignGet(String key, String name, String contentType, boolean download, Duration ttl) {
        try {
            if (legacy != null && r2Size(key) < 0) return null;
        } catch (IOException e) {
            return null;
        }
        String disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(name, StandardCharsets.UTF_8).build().toString();
        return presigner.presignGetObject(p -> p.signatureDuration(ttl)
                .getObjectRequest(b -> b.bucket(bucket).key(key)
                        .responseContentType(contentType).responseContentDisposition(disposition)
                        .responseCacheControl("private, max-age=600"))).url().toString();
    }

    private long r2Size(String key) throws IOException {
        try {
            return s3.headObject(b -> b.bucket(bucket).key(key)).contentLength();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return -1;
            throw new IOException("R2 확인 실패 " + e.statusCode(), e);
        } catch (SdkException e) {
            throw new IOException("R2 확인 실패", e);
        }
    }

    private <T> T call(SdkCall<T> action) throws IOException {
        try {
            return action.run();
        } catch (SdkException e) {
            throw new IOException("R2 요청 실패: " + e.getMessage(), e);
        }
    }

    private interface SdkCall<T> {
        T run();
    }

    // 받을 때마다 R2에서 새로 읽는다 (메모리에 다 올리지 않아서 큰 영상·ZIP도 흘려보낸다)
    private final class R2Object extends AbstractResource {
        private final String key;
        private final long length;

        private R2Object(String key, long length) {
            this.key = key;
            this.length = length;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public long contentLength() {
            return length;
        }

        @Override
        public String getDescription() {
            return "R2 " + key;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return call(() -> s3.getObject(b -> b.bucket(bucket).key(key)));
        }
    }
}
