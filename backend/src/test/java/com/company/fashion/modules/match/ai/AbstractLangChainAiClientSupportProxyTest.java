package com.company.fashion.modules.match.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.fashion.modules.member.service.BodyProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AbstractLangChainAiClientSupportProxyTest {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractLangChainAiClientSupportProxyTest.class);
  private final TestSupport support = new TestSupport();

  @Test
  void shouldBuildDirectClientWhenProxyHostMissing() {
    HttpClientBuilder builder = support.createBuilder("OpenAI", "", 0, 3000, 5000, LOG);

    assertThat(builder).isInstanceOf(JdkHttpClientBuilder.class);
    JdkHttpClientBuilder jdkBuilder = (JdkHttpClientBuilder) builder;
    assertThat(jdkBuilder.httpClientBuilder().build().proxy()).isEmpty();
  }

  @Test
  void shouldApplyProxyWhenHostAndPortAreValid() {
    HttpClientBuilder builder = support.createBuilder("Gemini", "127.0.0.1", 10808, 3000, 5000, LOG);

    JdkHttpClientBuilder jdkBuilder = (JdkHttpClientBuilder) builder;
    Proxy proxy = jdkBuilder.httpClientBuilder()
        .build()
        .proxy()
        .orElseThrow()
        .select(URI.create("https://api.openai.com"))
        .getFirst();
    assertThat(proxy.type()).isEqualTo(Proxy.Type.HTTP);
    assertThat(proxy.address()).isInstanceOf(InetSocketAddress.class);
    InetSocketAddress address = (InetSocketAddress) proxy.address();
    assertThat(address.getHostString()).isEqualTo("127.0.0.1");
    assertThat(address.getPort()).isEqualTo(10808);
  }

  @Test
  void shouldThrowWhenProxyHostProvidedButPortInvalid() {
    assertThatThrownBy(() -> support.createBuilder("OpenAI", "127.0.0.1", 0, 3000, 5000, LOG))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OpenAI proxy configuration invalid");
  }

  private static final class TestSupport extends AbstractLangChainAiClientSupport {

    private TestSupport() {
      super(new PromptBuilder(new BodyProfileService(new ObjectMapper())));
    }

    private HttpClientBuilder createBuilder(
        String provider,
        String proxyHost,
        int proxyPort,
        int connectTimeoutMs,
        int readTimeoutMs,
        Logger logger
    ) {
      return buildHttpClientBuilder(provider, proxyHost, proxyPort, connectTimeoutMs, readTimeoutMs, logger);
    }
  }
}
