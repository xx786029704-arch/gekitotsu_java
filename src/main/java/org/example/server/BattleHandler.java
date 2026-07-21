package org.example.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.CompiledFort;
import org.example.Fort;
import org.example.GameTask;
import org.example.Main;
import org.example.Result;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

final class JsonUtil {
    static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {}

    static void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] data = MAPPER.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }
}

final class BattleRunner {
    private BattleRunner() {}

    static Dtos.BattleResponse runOne(Dtos.BattleRequest req) {
        if (req.fort1() == null || req.fort2() == null) {
            return Dtos.BattleResponse.error("fort1 和 fort2 都是必填字段");
        }
        if (req.fort1().code() == null || req.fort2().code() == null) {
            return Dtos.BattleResponse.error("阵型代码 code 不能为空");
        }
        try {
            String n1 = req.fort1().name() != null ? req.fort1().name() : "";
            String n2 = req.fort2().name() != null ? req.fort2().name() : "";
            CompiledFort f1 = Main.compileFort(new Fort(n1, req.fort1().code()));
            CompiledFort f2 = Main.compileFort(new Fort(n2, req.fort2().code()));
            int maxFrames = req.maxFrames() != null ? req.maxFrames() : Main.MAX_FRAME_LIMIT;
            return runOneCompiled(f1, f2, n1, n2, maxFrames);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException
                 | ArrayIndexOutOfBoundsException e) {
            return Dtos.BattleResponse.error("阵型代码非法: " + e.getMessage());
        } catch (Exception e) {
            return Dtos.BattleResponse.error("内部错误: " + e);
        }
    }

    static Dtos.BattleResponse runOneCompiled(
            CompiledFort f1, CompiledFort f2,
            String n1, String n2, int maxFrames
    ) {
        try {
            Result r = new GameTask().run_single(f1, f2, maxFrames);
            return Dtos.BattleResponse.success(r, n1, n2);
        } catch (Exception e) {
            return Dtos.BattleResponse.error("内部错误: " + e);
        }
    }
}

final class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            JsonUtil.sendJson(ex, 405, new Dtos.ErrorResponse("Method not allowed", "请用 GET 请求"));
            return;
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "ok");
        resp.put("p1_count", Main.p1List.size());
        resp.put("p2_count", Main.p2List.size());
        resp.put("threads", Main.MAX_THREADS);
        resp.put("max_frames", Main.MAX_FRAME_LIMIT);
        JsonUtil.sendJson(ex, 200, resp);
    }
}

final class BattleHandler implements HttpHandler {
    private final ExecutorService gamePool;

    BattleHandler(ExecutorService gamePool) {
        this.gamePool = gamePool;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            JsonUtil.sendJson(ex, 405, new Dtos.ErrorResponse("Method not allowed", "请用 POST 请求"));
            return;
        }
        Dtos.BattleRequest req;
        try {
            req = JsonUtil.MAPPER.readValue(ex.getRequestBody(), Dtos.BattleRequest.class);
        } catch (Exception e) {
            JsonUtil.sendJson(ex, 400, new Dtos.ErrorResponse("JSON 解析失败", e.getMessage()));
            return;
        }

        Future<Dtos.BattleResponse> fut = gamePool.submit(() -> BattleRunner.runOne(req));
        Dtos.BattleResponse result;
        try {
            result = fut.get();
        } catch (Exception e) {
            result = Dtos.BattleResponse.error("任务执行失败: " + e);
        }

        if (result.status() == -2) {
            JsonUtil.sendJson(ex, 400, new Dtos.ErrorResponse("对战失败", result.error()));
        } else {
            JsonUtil.sendJson(ex, 200, result);
        }
    }
}

final class BatchHandler implements HttpHandler {
    private final ExecutorService gamePool;

    BatchHandler(ExecutorService gamePool) {
        this.gamePool = gamePool;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            JsonUtil.sendJson(ex, 405, new Dtos.ErrorResponse("Method not allowed", "请用 POST 请求"));
            return;
        }
        Dtos.BatchRequest req;
        try {
            req = JsonUtil.MAPPER.readValue(ex.getRequestBody(), Dtos.BatchRequest.class);
        } catch (Exception e) {
            JsonUtil.sendJson(ex, 400, new Dtos.ErrorResponse("JSON 解析失败", e.getMessage()));
            return;
        }
        if (req.battles() == null) {
            JsonUtil.sendJson(ex, 400, new Dtos.ErrorResponse("缺少 battles 字段", "battles 字段是必填的"));
            return;
        }

        long start = System.nanoTime();
        List<Future<Dtos.BattleResponse>> futures = new ArrayList<>(req.battles().size());
        for (Dtos.BattleRequest b : req.battles()) {
            futures.add(gamePool.submit(() -> BattleRunner.runOne(b)));
        }

        List<Dtos.BattleResponse> results = new ArrayList<>(futures.size());
        for (Future<Dtos.BattleResponse> fut : futures) {
            try {
                results.add(fut.get());
            } catch (Exception e) {
                results.add(Dtos.BattleResponse.error("任务执行失败: " + e));
            }
        }

        double totalTimeMs = (System.nanoTime() - start) / 1_000_000.0;
        JsonUtil.sendJson(ex, 200, new Dtos.BatchResponse(results, totalTimeMs));
    }
}
