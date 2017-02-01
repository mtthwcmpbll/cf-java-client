/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.ProxyConfiguration;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Configuration
@EnableAutoConfiguration
public class ProxyTest {

    public static void main(String[] args) throws InterruptedException {
        new SpringApplicationBuilder(ProxyTest.class)
            .web(false)
            .run(args)
            .getBean(Runner.class)
            .run()
            .await();
    }

    @Bean
    ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorCloudFoundryClient.builder()
            .connectionContext(connectionContext)
            .tokenProvider(tokenProvider)
            .build();
    }

    @Bean
    DefaultConnectionContext connectionContext(@Value("${test.apiHost}") String apiHost,
                                               @Value("${test.skipSslValidation:false}") Boolean skipSslValidation) {

        return DefaultConnectionContext.builder()
            .apiHost(apiHost)
            .skipSslValidation(skipSslValidation)
            .proxyConfiguration(ProxyConfiguration.builder()
                .host("localhost")
                .port(8080)
                .build())
            .build();
    }

    @Bean
    PasswordGrantTokenProvider tokenProvider(@Value("${test.password}") String password,
                                             @Value("${test.username}") String username) {

        return PasswordGrantTokenProvider.builder()
            .password(password)
            .username(username)
            .build();
    }

    @Component
    private static final class Runner {

        private final CloudFoundryClient cloudFoundryClient;

        private Runner(CloudFoundryClient cloudFoundryClient) {
            this.cloudFoundryClient = cloudFoundryClient;
        }

        private CountDownLatch run() {
            CountDownLatch latch = new CountDownLatch(1);

            this.cloudFoundryClient.info()
                .get(GetInfoRequest.builder()
                    .build())
                .subscribe(System.out::println, t -> {
                    t.printStackTrace();
                    latch.countDown();
                }, latch::countDown);

            return latch;
        }

    }

}
