package cn.edu.ustc.Guerrillas.BigBlock.server;

import cn.edu.ustc.Guerrillas.BigBlock.core.Block;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

import java.util.Iterator;
import java.util.UUID;


public class HttpServer extends AbstractVerticle {
    private CassandraDao cassandraDao = new CassandraDao();
    private int port;

    public HttpServer(int port) {
        this.port = port;
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);
        router.get("/publicKey/:id").handler(routingContext -> {
            String id = routingContext.request().getParam("id");
            byte[] keyBytes = cassandraDao.getPublicKey(id);
            HttpServerResponse response = routingContext.response();
            response.setChunked(true);
            if (keyBytes == null) {
                response.setStatusCode(404);
                response.end();
            } else {
                response.setStatusCode(200);
                response.end(Buffer.buffer(keyBytes));
            }
        });
        router.post("/register/").handler(context -> {
            HttpServerRequest request = context.request();
            String id = request.getParam("id");
            request.bodyHandler(buffer -> {
                byte[] publicKey = buffer.getBytes();
                HttpServerResponse response = context.response();
                response.putHeader("Content-Length", String.valueOf(0));
                int statusCode = 201;
                String statusMessage = "";
                try {
                    boolean key = cassandraDao.putPublicKey(id, publicKey);
                    if (!key) {
                        statusCode = 409;
                        statusMessage = "ID already exists";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    statusCode = 500;
                    statusMessage = "Database error";
                }
                response.setStatusCode(statusCode);
                response.setStatusMessage(statusMessage);
                response.end();
            });
        });
        router.post("/repo/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("Content-Length", String.valueOf(0));
            routingContext.request().bodyHandler(buffer -> {
                try {
                    Block block = buffer.toJsonObject().mapTo(Block.class);
                    if (Block.isRoot(block) && cassandraDao.createRepo(block)) {
                        response.setStatusCode(201);
                    } else {
                        response.setStatusCode(409);
                        response.setStatusMessage("Repo already exist");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatusCode(500);
                    response.setStatusMessage("Database error");
                }
                response.end();
            });
        });
        router.get("/repo/:repoName").handler(routingContext -> {
            HttpServerRequest request = routingContext.request();
            String repo = request.getParam("repoName");
            HttpServerResponse response = routingContext.response();
            response.setChunked(true);
            try {
                Iterator<Block> blockIterator = cassandraDao.getRepo(repo);
                ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
                while (blockIterator.hasNext()) {
                    Block block = blockIterator.next();
                    response.write(objectWriter.writeValueAsString(block));
                    response.write("\n");
                }
                response.setStatusCode(200);
                response.end();
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatusCode(500);
                response.setStatusMessage("Database error");
                response.end();
            }
        });
        router.post("/block").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            routingContext.request().bodyHandler(buffer -> {
                try {
                    Block block = buffer.toJsonObject().mapTo(Block.class);
                    response.putHeader("Content-Length", String.valueOf(0));
                    if (cassandraDao.putBlock(block)) {
                        response.setStatusCode(201);
                    } else {
                        response.setStatusCode(403);
                        response.setStatusMessage("Invalid block");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatusMessage("Database error");
                    response.setStatusCode(500);
                }
                response.end();
            });
        });
        router.get("/block/:id").handler(routingContext -> {
            String id = routingContext.request().getParam("id");
            UUID uuid = UUID.fromString(id);
            Block block = cassandraDao.getBlock(uuid);
            HttpServerResponse response = routingContext.response();
            if (block != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    response.setChunked(true);
                    response.setStatusCode(200);
                    response.end(objectMapper.writeValueAsString(block));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    response.setStatusCode(500);
                    response.setStatusMessage("Internal error");
                    response.end();
                }
            } else {
                response.setStatusCode(404);
                response.setStatusMessage("No such block");
                response.end();
            }
        });
        router.get("/trace/:id").handler(routingContext -> {
            String id = routingContext.request().getParam("id");
            UUID uuid = UUID.fromString(id);
            HttpServerResponse response = routingContext.response();
            ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
            while (true) {
                Block block = cassandraDao.getBlock(uuid);
                if (block != null) {
                    response.setChunked(true);
                    response.setStatusCode(200);
                    try {
                        response.write(objectWriter.writeValueAsString(block));
                        response.write("\n");
                        if (Block.isRoot(block)) {
                            response.end();
                            return;
                        }
                        uuid = block.getPrevId();
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        response.setStatusCode(500);
                        response.setStatusMessage("Internal error");
                        response.end();
                        return;
                    }
                } else {
                    response.setStatusCode(404);
                    response.setStatusMessage("No such block");
                    response.end();
                    return;
                }
            }
        });
        vertx.createHttpServer().requestHandler(router::accept).listen(port);
    }

    @Override
    public void stop() {
        cassandraDao.stop();
    }
}
