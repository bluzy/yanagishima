package yanagishima.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import lombok.experimental.UtilityClass;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static yanagishima.util.Constants.YANAGISHIAM_HIVE_JOB_PREFIX;

@UtilityClass
public final class YarnUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String kill(RestTemplate restTemplate, String resourceManagerUrl, String applicationId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", APPLICATION_JSON);
            String json = OBJECT_MAPPER.writeValueAsString(Map.of("state", "KILLED"));
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            return restTemplate.exchange(resourceManagerUrl + "/ws/v1/cluster/apps/" + applicationId + "/state", HttpMethod.PUT,
                    entity, String.class).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<Map> getApplication(RestTemplate restTemplate, String resourceManagerUrl, String queryId, String userName,
                                               Optional<String> beginTime) {
        List<Map> yarnJobs = getJobList(restTemplate, resourceManagerUrl, beginTime);
        if (userName == null) {
            return yarnJobs.stream().filter(job -> job.get("name").equals(YANAGISHIAM_HIVE_JOB_PREFIX + queryId))
                    .findFirst();
        }
        return yarnJobs.stream().filter(
                job -> job.get("name").equals(YANAGISHIAM_HIVE_JOB_PREFIX + userName + "-" + queryId)).findFirst();
    }

    public static List<Map> getJobList(RestTemplate restTemplate, String resourceManagerUrl, Optional<String> beginTime) {
        try {
            String json;
            if (beginTime.isPresent()) {
                long currentTimeMillis = System.currentTimeMillis();
                long startedTimeBegin = currentTimeMillis - Long.valueOf(beginTime.get());
                json = restTemplate.getForObject(
                        resourceManagerUrl + "/ws/v1/cluster/apps?startedTimeBegin=" + startedTimeBegin, String.class);
            } else {
                json = restTemplate.getForObject(resourceManagerUrl + "/ws/v1/cluster/apps", String.class);
            }
            return jsonToMaps(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    public static List<Map> jsonToMaps(String json) throws IOException {
        Map map = OBJECT_MAPPER.readValue(json, Map.class);
        if (map.get("apps") == null) {
            return List.of();
        }
        return (List) ((Map) map.get("apps")).get("app");
    }
}
