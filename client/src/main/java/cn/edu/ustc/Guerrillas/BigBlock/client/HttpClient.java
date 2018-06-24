package cn.edu.ustc.Guerrillas.BigBlock.client;

import cn.edu.ustc.Guerrillas.BigBlock.core.Block;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class HttpClient {
    static Logger logger = Logger.getLogger(HttpClient.class.getName());
    private WebClient webClient = WebClient.create(Vertx.vertx());
    private String serverAddress;
    private int port;

    HttpClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    private static HttpResponse<Buffer> awaitResult(Handler<Handler<AsyncResult<HttpResponse<Buffer>>>> handler) {
        AtomicReference<Optional<HttpResponse<Buffer>>> response = new AtomicReference<>(null);
        Handler<AsyncResult<HttpResponse<Buffer>>> asyncResultHandler = asyncResult -> {
            if (asyncResult.succeeded()) {
                response.set(Optional.of(asyncResult.result()));
            } else {
                response.set(Optional.empty());
            }
        };
        handler.handle(asyncResultHandler);
        while (response.get() == null) ;
        return response.get().get();
    }

    public boolean register(String id, byte[] publicKey) {
        HttpResponse<Buffer> response = awaitResult(asyncResultHandler -> {
            webClient.post(port, serverAddress, "/register")
                    .addQueryParam("id", id)
                    .sendBuffer(Buffer.buffer(publicKey), asyncResultHandler);
        });
        return response.statusCode() == 201;
    }

    public boolean checkBlock(Block block, Block prevBlock) throws Exception {
        if (Block.isRoot(block)) {
            return true;
        }
        HttpResponse<Buffer> response = awaitResult(asyncResultHandler ->
                webClient.get(port, serverAddress, "/publicKey/" + prevBlock.getCommitter()).send(asyncResultHandler));
        byte[] keyBytes = response.bodyAsBuffer().getBytes();
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        return block.checkPrev(publicKey, prevBlock.getFileHashCode());
    }

    public void fetchRepo(String repo, Consumer<Block> blockConsumer) {
        RecordParser parser = RecordParser.newDelimited("\n", h -> {
            Block block = h.toJsonObject().mapTo(Block.class);
            blockConsumer.accept(block);
        });
        HttpResponse<Buffer> response = awaitResult(h -> webClient.get(port, serverAddress, "/repo/" + repo).send(h));
        parser.handle(response.bodyAsBuffer());
    }

    public boolean createRepo(Block block) {
        HttpResponse<Buffer> response = awaitResult(h -> webClient.post(port, serverAddress, "/repo").sendJson(block, h));
        return response.statusCode() == 201;
    }

    public Block getBlock(UUID uuid) {
        HttpResponse<Buffer> response = awaitResult(h -> webClient.get(port, serverAddress, "/block/" + uuid.toString())
                .send(h));
        if (response.statusCode() != 200) {
            return null;
        }
        return response.bodyAsJson(Block.class);
    }

    public boolean putBlock(Block block) {
        HttpResponse<Buffer> response = awaitResult(h -> webClient.post(port, serverAddress, "/block").sendJson(block, h));
        return response.statusCode() == 201;
    }
}
