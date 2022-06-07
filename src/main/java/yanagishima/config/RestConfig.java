package yanagishima.config;

import lombok.RequiredArgsConstructor;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.kerberos.client.KerberosRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RestConfig {

    @Bean
    public CloseableHttpClient httpClient() {
        int pool = 30;

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(null, -1, null), new Credentials() {
            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getPassword() {
                return null;
            }
        });

        return HttpClientBuilder.create()
                .setMaxConnTotal(pool)
                .setMaxConnPerRoute(pool)
                .setDefaultAuthSchemeRegistry(
                        RegistryBuilder.<AuthSchemeProvider>create()
                                .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true)).build()
                )
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

}
