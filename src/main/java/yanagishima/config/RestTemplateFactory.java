package yanagishima.config;

import lombok.RequiredArgsConstructor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.kerberos.client.KerberosRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@RequiredArgsConstructor
public class RestTemplateFactory {

    private final YanagishimaConfig yanagishimaConfig;
    private final CloseableHttpClient httpClient;

    private static final ConcurrentHashMap<String, RestTemplate> restTemplateMap = new ConcurrentHashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    public RestTemplate getOrCreateRestTemplate(String datasource) {
        lock.lock();
        try {
            if (restTemplateMap.containsKey(datasource)) {
                return restTemplateMap.get(datasource);
            }

            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(httpClient);

            DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
            defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

            Optional<String> kerberosKeytab = yanagishimaConfig.getKerberosKeytab(datasource);
            Optional<String> kerberosPrincipal = yanagishimaConfig.getKerberosPrincipal(datasource);

            RestTemplate restTemplate;

            if (kerberosKeytab.isPresent() && kerberosPrincipal.isPresent()) {
                restTemplate = new KerberosRestTemplate(kerberosKeytab.get(), kerberosPrincipal.get(), httpClient);
            } else {
                restTemplate = new RestTemplate();
            }
            restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);

            restTemplateMap.put(datasource, restTemplate);

            return restTemplate;
        } finally {
            lock.unlock();
        }
    }
}
