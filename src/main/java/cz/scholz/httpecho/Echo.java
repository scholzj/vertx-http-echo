package cz.scholz.httpecho;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by schojak on 10.1.17.
 */
public class Echo extends AbstractVerticle {
    final static private Logger LOG = LoggerFactory.getLogger(Echo.class);
    private WebClient client = null;
    private Router router = null;
    private HttpServer server = null;
    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void start(Future<Void> fut)
    {
        ConfigStoreOptions envStore = new ConfigStoreOptions()
                .setType("env");
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(envStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig(ar -> {
            if (ar.failed()) {
                LOG.error("Failed to get configuration", ar.cause());
                fut.fail(ar.cause());
            } else {
                JsonObject config = ar.result();

                if (config.getString("SERVICE_TYPE", "client").equals("server")) {
                    String httpHost = config.getString("HTTP_HOSTNAME", "0.0.0.0");
                    Integer httpPort = config.getInteger("HTTP_PORT", 8080);

                    LOG.info("Starting HTTP server on {}:{}", httpHost, httpPort);

                    router = Router.router(vertx);

                    router.route("/*").handler(BodyHandler.create());
                    router.post("/echo").handler(req -> {
                        LOG.info("Received request: {}", req.getBodyAsString());
                        req.response().end(req.getBodyAsString());
                    });

                    HttpServerOptions httpOptions = new HttpServerOptions();
                    server = vertx.createHttpServer(httpOptions)
                            .requestHandler(router::accept)
                            .listen(httpPort, httpHost, res -> {
                                if (res.failed()) {
                                    LOG.error("Failed to HTTP server", ar.cause());
                                    fut.fail(ar.cause());
                                } else {
                                    LOG.info("HTTP server is running");
                                    fut.complete();
                                }
                            });
                }
                else {
                    String httpHost = config.getString("HTTP_HOSTNAME", "localhost");
                    Integer httpPort = config.getInteger("HTTP_PORT", 8080);
                    Integer timer = config.getInteger("TIMEOUT", 1000);

                    LOG.info("Starting HTTP client on {}:{}", httpHost, httpPort);

                    client = WebClient.create(vertx);

                    vertx.setPeriodic(timer, id -> {
                        String message = "Echo " + counter.incrementAndGet();
                        LOG.info("Sending message: {}", message);

                        client.post(httpPort, httpHost, "/echo")
                            .sendBuffer(Buffer.buffer(message),res -> {
                                if (res.succeeded()) {
                                    LOG.info("Received response: {}", res.result().bodyAsString());
                                } else {
                                    LOG.error("Failed to get response", res.cause());
                                }
                            });
                    });

                    fut.complete();
                }
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (client != null) {
            client.close();
        }

        if (server != null) {
            server.close();
        }

        LOG.info("Shutting down");
        // Nothing to do
    }
}
