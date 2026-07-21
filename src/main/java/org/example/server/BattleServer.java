package org.example.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BattleServer {
    private static final int HTTP_IO_THREADS = 8;

    private final HttpServer server;
    private final ExecutorService httpPool;

    public BattleServer(int port, ExecutorService gamePool) throws IOException {
        httpPool = Executors.newFixedThreadPool(HTTP_IO_THREADS);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(httpPool);

        SlotStore slotStore = new SlotStore();

        server.createContext("/health", new HealthHandler());
        server.createContext("/battle", new BattleHandler(gamePool));
        server.createContext("/battles", new BatchHandler(gamePool));
        server.createContext("/p1", new SlotHandler(slotStore, 0));
        server.createContext("/p2", new SlotHandler(slotStore, 1));
        server.createContext("/run", new RunHandler(slotStore, gamePool));
    }

    public void start() {
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
        httpPool.shutdownNow();
    }
}
