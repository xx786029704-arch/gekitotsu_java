package org.example.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.CompiledFort;
import org.example.Main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

final class RunHandler implements HttpHandler {
    private final SlotStore store;
    private final ExecutorService gamePool;

    RunHandler(SlotStore store, ExecutorService gamePool) {
        this.store = store;
        this.gamePool = gamePool;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            JsonUtil.sendJson(ex, 405, new Dtos.ErrorResponse("Method not allowed", "请用 POST 请求"));
            return;
        }

        Integer maxFrames = parseMaxFrames(ex);
        if (maxFrames == null) return;   // 解析失败已返回 400
        int maxF = maxFrames;

        List<CompiledFort> p1 = store.getP1();
        List<CompiledFort> p2 = store.getP2();
        if (p1.isEmpty() || p2.isEmpty()) {
            JsonUtil.sendJson(ex, 400, new Dtos.ErrorResponse(
                    "阵容未配置",
                    "1P 有 " + p1.size() + " 个，2P 有 " + p2.size() + " 个，都至少需要 1 个"));
            return;
        }

        int total = p1.size() * p2.size();
        System.out.println("[RUN] 1P=" + p1.size() + " × 2P=" + p2.size() + " = " + total + " 局开始");

        long start = System.nanoTime();
        List<Future<Dtos.BattleResponse>> futures = new ArrayList<>(total);
        for (CompiledFort f1 : p1) {
            for (CompiledFort f2 : p2) {
                String n1 = f1.name();
                String n2 = f2.name();
                futures.add(gamePool.submit(() -> BattleRunner.runOneCompiled(f1, f2, n1, n2, maxF)));
            }
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
        System.out.printf("[RUN] 完成 %d 局，总用时 %.2f ms%n", total, totalTimeMs);
        JsonUtil.sendJson(ex, 200, new Dtos.BatchResponse(results, totalTimeMs));
    }

    private Integer parseMaxFrames(HttpExchange ex) throws IOException {
        byte[] body = ex.getRequestBody().readAllBytes();
        if (body.length == 0) return Main.MAX_FRAME_LIMIT;
        try {
            Dtos.RunRequest req = JsonUtil.MAPPER.readValue(body, Dtos.RunRequest.class);
            return req.maxFrames() != null ? req.maxFrames() : Main.MAX_FRAME_LIMIT;
        } catch (Exception e) {
            JsonUtil.sendJson(ex, 400, new Dtos.ErrorResponse("JSON 解析失败", e.getMessage()));
            return null;
        }
    }
}
