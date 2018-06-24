package cn.edu.ustc.Guerrillas.BigBlock.server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

public class HttpService {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
//        DeploymentOptions deploymentOptions = new DeploymentOptions()
//                .setInstances(Runtime.getRuntime().availableProcessors());
        vertx.deployVerticle(new HttpServer(9000));
    }
}
