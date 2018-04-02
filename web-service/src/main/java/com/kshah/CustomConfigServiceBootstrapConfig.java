package com.kshah;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Profile("custom")
@Configuration
public class CustomConfigServiceBootstrapConfig {

    private Environment environment;

    @Value("${spring.cloud.config.uri}")
    private String configServerUri;

    @Value("${custom.ssl.enabled}")
    private Boolean sslEnabled;

    @Value("${custom.ssl.keystore}")
    private Resource keystore;

    @Value("${custom.ssl.keystore-password}")
    private String keystorePassword;

    @Value("${custom.ssl.key-alias}")
    private String keyAlias;

    @Value("${custom.ssl.key-password}")
    private String keyPassword;

    @Value("${custom.ssl.truststore}")
    private Resource truststore;

    @Value("${custom.ssl.truststore-password}")
    private String truststorePassword;

    @Value("${custom.ssl.verify-hostname:true}")
    private Boolean verifyHostname;


    @Autowired
    public CustomConfigServiceBootstrapConfig(Environment environment) {
        this.environment = environment;
    }


    @Bean
    public ConfigClientProperties configClientProperties() {
        ConfigClientProperties client = new ConfigClientProperties(this.environment);
        client.setUri(configServerUri);
        return client;
    }


    @Primary
    @Bean
    public ConfigServicePropertySourceLocator configServicePropertySourceLocator() {
        ConfigClientProperties clientProperties = configClientProperties();
        ConfigServicePropertySourceLocator configServicePropertySourceLocator = new ConfigServicePropertySourceLocator(clientProperties);
        configServicePropertySourceLocator.setRestTemplate(createRestTemplate());
        return configServicePropertySourceLocator;
    }


    private RestTemplate createRestTemplate() {

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        if(sslEnabled) {
            configureSSL(httpClientBuilder);
        }

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build()));
    }


    private void configureSSL(HttpClientBuilder httpClientBuilder) {

        try {
            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            sslContextBuilder.loadKeyMaterial(keystore.getFile(), keystorePassword.toCharArray(), keyPassword.toCharArray(), (map, socket) -> keyAlias);
            sslContextBuilder.loadTrustMaterial(truststore.getFile(), truststorePassword.toCharArray());

            SSLContext sslContext = sslContextBuilder.build();

            if(verifyHostname) {
                httpClientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext));
            } else {
                httpClientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE));
            }

        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | CertificateException | IOException | KeyManagementException e) {
            throw new IllegalStateException("Keystore and/or truststore could not be loaded. Please check that the configurations are set up correctly.", e);
        }

    }

}