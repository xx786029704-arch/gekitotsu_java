package org.example.training.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 启动随机端口上的真实 HTTP 服务，验证 JSON 协议与对局执行链。
 */
public final class TrainingServerIntegrationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TrainingServerIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        try (TrainingServer server = new TrainingServer(0, 2, 32, 128)) {
            server.start();
            String baseUrl = "http://127.0.0.1:" + server.port();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> health = client.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/health"))
                            .GET()
                            .timeout(Duration.ofSeconds(5))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            check(health.statusCode() == 200, "health 应返回 200");
            JsonNode healthJson = MAPPER.readTree(health.body());
            check("2.0".equals(healthJson.get("api_version").asText()), "API 版本错误");

            String code = "0" + encode(0, 190, 400);
            String battleBody = """
                    {
                      "fort1": {"name": "p1", "code": "%s"},
                      "fort2": {"name": "p2", "code": "%s"},
                      "max_frames": 2000,
                      "seed": 20260724,
                      "telemetry": "EVENTS_AND_SNAPSHOTS",
                      "snapshot_interval": 30
                    }
                    """.formatted(code, code);
            HttpResponse<String> battle = postJson(client, baseUrl + "/v2/battle", battleBody);
            check(battle.statusCode() == 200, "battle 应返回 200: " + battle.body());
            JsonNode battleJson = MAPPER.readTree(battle.body());
            check(battleJson.get("seed").asInt() == 20_260_724, "响应种子错误");
            check(battleJson.get("telemetry").get("summary").get("collision_count").asInt() > 0,
                    "响应应包含碰撞统计");
            check(battleJson.get("telemetry").get("snapshots").size() > 1,
                    "响应应包含定期快照");

            HttpResponse<String> seeds = postJson(
                    client,
                    baseUrl + "/v2/seeds",
                    "{\"master_seed\":42,\"count\":4}"
            );
            check(seeds.statusCode() == 200, "seeds 应返回 200");
            check(MAPPER.readTree(seeds.body()).get("seeds").size() == 4, "种子数量错误");

            HttpResponse<String> invalid = postJson(
                    client,
                    baseUrl + "/v2/battle",
                    "{\"fort1\":{\"code\":\"bad\"},\"fort2\":{\"code\":\"bad\"}}"
            );
            check(invalid.statusCode() == 400, "非法阵型应返回 400");
        }
        System.out.println("TrainingServerIntegrationTest: ALL PASSED");
    }

    private static HttpResponse<String> postJson(HttpClient client, String url, String body)
            throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .timeout(Duration.ofSeconds(30))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private static String encode(int rotation, int x, int y) {
        return Utils.to_rad61(rotation * 1_000_000 + x * 1_000 + y, 5);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
