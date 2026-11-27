package org.example.training.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.CompiledFort;
import org.example.Main;
import org.example.training.BattleOptions;
import org.example.training.SeedSchedule;
import org.example.training.TrainingBattleResult;
import org.example.training.TrainingBattleRunner;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Nymph_Fort 2.0 本地训练服务。
 *
 * <p>默认只监听 127.0.0.1，避免无意间把生成与对战接口暴露到局域网。</p>
 */
public final class TrainingServer implements AutoCloseable {
    public static final String API_VERSION = "2.0";
    private static final int DEFAULT_PORT = 8765;
    private static final int DEFAULT_MAX_BATCH_SIZE = 10_000;
    private static final int DEFAULT_CACHE_SIZE = 50_000;

    private final HttpServer server;
    private final ExecutorService httpExecutor;
    private final ExecutorService battleExecutor;
    private final CompiledFortCache fortCache;
    private final ObjectMapper mapper;
    private final int battleThreads;
    private final int maxBatchSize;
    private final AtomicLong completedBattles = new AtomicLong();

    public TrainingServer(int port, int battleThreads) throws IOException {
        this(port, battleThreads, DEFAULT_MAX_BATCH_SIZE, DEFAULT_CACHE_SIZE);
    }

    public TrainingServer(
            int port,
            int battleThreads,
            int maxBatchSize,
            int cacheSize
    ) throws IOException {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port 必须位于 0..65535");
        }
        if (battleThreads <= 0) {
            throw new IllegalArgumentException("battleThreads 必须大于 0");
        }
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("maxBatchSize 必须大于 0");
        }
        this.battleThreads = battleThreads;
        this.maxBatchSize = maxBatchSize;
        this.fortCache = new CompiledFortCache(cacheSize);
        this.mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 64);
        this.httpExecutor = Executors.newCachedThreadPool();
        this.battleExecutor = Executors.newFixedThreadPool(battleThreads);
        this.server.setExecutor(httpExecutor);
        registerRoutes();
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(1);
        battleExecutor.shutdownNow();
        httpExecutor.shutdownNow();
    }

    private void registerRoutes() {
        server.createContext("/health", this::handleHealth);
        server.createContext("/v2/battle", this::handleBattle);
        server.createContext("/v2/battles", this::handleBattles);
        server.createContext("/v2/seeds", this::handleSeeds);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) return;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("api_version", API_VERSION);
        response.put("engine_version", Main.VERSION);
        response.put("battle_threads", battleThreads);
        response.put("max_batch_size", maxBatchSize);
        response.put("compiled_fort_cache_size", fortCache.size());
        response.put("completed_battles", completedBattles.get());
        sendJson(exchange, 200, response);
    }

    private void handleBattle(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) return;
        TrainingApiDtos.BattleRequest request;
        try {
            request = mapper.readValue(exchange.getRequestBody(), TrainingApiDtos.BattleRequest.class);
        } catch (Exception exception) {
            sendError(exchange, 400, "JSON 解析失败", exception.getMessage());
            return;
        }
        try {
            TrainingBattleResult result = CompletableFuture
                    .supplyAsync(() -> runOne(request), battleExecutor)
                    .join();
            sendJson(exchange, 200, result);
        } catch (IllegalArgumentException exception) {
            sendError(exchange, 400, "请求参数错误", exception.getMessage());
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IllegalArgumentException illegalArgument) {
                sendError(exchange, 400, "请求参数错误", illegalArgument.getMessage());
            } else {
                sendError(exchange, 500, "对局执行失败", String.valueOf(cause));
            }
        } catch (Exception exception) {
            sendError(exchange, 500, "对局执行失败", exception.toString());
        }
    }

    private void handleBattles(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) return;
        TrainingApiDtos.BatchRequest request;
        try {
            request = mapper.readValue(exchange.getRequestBody(), TrainingApiDtos.BatchRequest.class);
        } catch (Exception exception) {
            sendError(exchange, 400, "JSON 解析失败", exception.getMessage());
            return;
        }
        if (request.battles() == null) {
            sendError(exchange, 400, "请求参数错误", "battles 不能为空");
            return;
        }
        if (request.battles().size() > maxBatchSize) {
            sendError(
                    exchange,
                    400,
                    "批次过大",
                    "单批最多允许 " + maxBatchSize + " 局"
            );
            return;
        }

        long start = System.nanoTime();
        List<CompletableFuture<TrainingApiDtos.BattleOutcome>> futures =
                new ArrayList<>(request.battles().size());
        for (TrainingApiDtos.BattleRequest battle : request.battles()) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return TrainingApiDtos.BattleOutcome.success(runOne(battle));
                } catch (Exception exception) {
                    return TrainingApiDtos.BattleOutcome.failure(exception.getMessage());
                }
            }, battleExecutor));
        }

        List<TrainingApiDtos.BattleOutcome> outcomes = new ArrayList<>(futures.size());
        for (CompletableFuture<TrainingApiDtos.BattleOutcome> future : futures) {
            outcomes.add(future.join());
        }
        double totalTimeMs = (System.nanoTime() - start) / 1_000_000.0;
        sendJson(exchange, 200, new TrainingApiDtos.BatchResponse(outcomes, totalTimeMs));
    }

    private void handleSeeds(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) return;
        TrainingApiDtos.SeedRequest request;
        try {
            request = mapper.readValue(exchange.getRequestBody(), TrainingApiDtos.SeedRequest.class);
            if (request.count() < 0 || request.count() > 1_000_000) {
                throw new IllegalArgumentException("count 必须位于 0..1000000");
            }
        } catch (Exception exception) {
            sendError(exchange, 400, "请求参数错误", exception.getMessage());
            return;
        }
        sendJson(
                exchange,
                200,
                new TrainingApiDtos.SeedResponse(
                        request.masterSeed(),
                        SeedSchedule.create(request.masterSeed(), request.count())
                )
        );
    }

    private TrainingBattleResult runOne(TrainingApiDtos.BattleRequest request) {
        if (request == null || request.fort1() == null || request.fort2() == null) {
            throw new IllegalArgumentException("fort1 和 fort2 都是必填字段");
        }
        CompiledFort p1 = fortCache.get(request.fort1().code(), "fort1");
        CompiledFort p2 = fortCache.get(request.fort2().code(), "fort2");
        BattleOptions options = request.toOptions();
        TrainingBattleResult result = TrainingBattleRunner.run(
                p1,
                request.fort1().normalizedName(),
                p2,
                request.fort2().normalizedName(),
                options
        );
        completedBattles.incrementAndGet();
        return result;
    }

    private boolean requireMethod(HttpExchange exchange, String expected) throws IOException {
        if (expected.equals(exchange.getRequestMethod())) {
            return true;
        }
        exchange.getResponseHeaders().set("Allow", expected);
        sendError(exchange, 405, "Method Not Allowed", "请使用 " + expected);
        return false;
    }

    private void sendError(HttpExchange exchange, int status, String error, String detail)
            throws IOException {
        sendJson(
                exchange,
                status,
                new TrainingApiDtos.ErrorResponse(
                        error,
                        detail == null || detail.isBlank() ? "未提供详细信息" : detail
                )
        );
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] data = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(data);
        }
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        for (int index = 0; index < args.length; index++) {
            switch (args[index]) {
                case "--port" -> port = Integer.parseInt(requireValue(args, ++index, "--port"));
                case "--threads" -> threads = Integer.parseInt(requireValue(args, ++index, "--threads"));
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                default -> throw new IllegalArgumentException("未知参数: " + args[index]);
            }
        }

        TrainingServer trainingServer = new TrainingServer(port, threads);
        Runtime.getRuntime().addShutdownHook(new Thread(trainingServer::close));
        trainingServer.start();
        System.out.printf(
                "Nymph_Fort 2.0 训练服务已启动：http://127.0.0.1:%d%n",
                trainingServer.port()
        );
        System.out.printf("对局线程：%d，按 Ctrl+C 停止%n", threads);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " 缺少参数");
        }
        return args[index];
    }

    private static void printHelp() {
        System.out.println("""
                用法：
                  java ... org.example.training.server.TrainingServer [选项]

                选项：
                  --port <端口>       本地监听端口，默认 8765
                  --threads <数量>    并行对局线程数，默认 CPU 逻辑核心数
                  --help              显示帮助
                """);
    }
}
