package io.scalecube.services.benchmarks.gateway.standalone;

import io.scalecube.benchmarks.BenchmarkSettings;
import io.scalecube.net.Address;
import io.scalecube.services.Microservices;
import io.scalecube.services.benchmarks.ServiceTransports;
import io.scalecube.services.benchmarks.gateway.AbstractBenchmarkState;
import io.scalecube.services.discovery.ScalecubeServiceDiscovery;
import io.scalecube.services.examples.BenchmarkServiceImpl;
import io.scalecube.services.gateway.clientsdk.Client;
import io.scalecube.services.gateway.http.HttpGateway;
import io.scalecube.services.gateway.rsocket.RSocketGateway;
import io.scalecube.services.gateway.ws.WebsocketGateway;
import java.util.function.BiFunction;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;

public class StandaloneBenchmarkState extends AbstractBenchmarkState<StandaloneBenchmarkState> {

  private final String gatewayName;

  private Microservices microservices;

  public StandaloneBenchmarkState(
      BenchmarkSettings settings,
      String gatewayName,
      BiFunction<Address, LoopResources, Client> clientBuilder) {
    super(settings, clientBuilder);
    this.gatewayName = gatewayName;
  }

  @Override
  protected void beforeAll() throws Exception {
    super.beforeAll();

    microservices =
        Microservices.builder()
            .services(new BenchmarkServiceImpl())
            .gateway(opts -> new RSocketGateway(opts.id("rsws")))
            .gateway(opts -> new WebsocketGateway(opts.id("ws")))
            .gateway(opts -> new HttpGateway(opts.id("http")))
            .discovery(ScalecubeServiceDiscovery::new)
            .transport(ServiceTransports::rsocketServiceTransport)
            .metrics(registry())
            .startAwait();
  }

  @Override
  protected void afterAll() throws Exception {
    super.afterAll();
    if (microservices != null) {
      microservices.shutdown().block();
    }
  }

  /**
   * Factory function for {@link Client}.
   *
   * @return client
   */
  public Mono<Client> createClient() {
    return createClient(microservices, gatewayName, clientBuilder);
  }
}
