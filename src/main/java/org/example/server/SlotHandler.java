package org.example.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.CompiledFort;
import org.example.Fort;
import org.example.Main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class SlotHandler implements HttpHandler {
    private final SlotStore store;
    private final int side;   // 0 = 1P, 1 = 2P

    SlotHandler(SlotStore store, int side) {
        this.store = store;
        this.side = side;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        switch (ex.getRequestMethod()) {
            case "PUT"    -> handlePut(ex);
            case "GET"    -> handleGet(ex);
            case "DELETE" -> handleDelete(ex);
            default -> JsonUtil.sendJson(ex, 405, new Dtos.ErrorResponse(
                    "Method not allowed", "请用 PUT 上传、GET 查询或 DELETE 清空"));
        }
    }

    private void handlePut(HttpExchange ex) throws IOException {
        Dtos.FortSlotRequest req;
        try {
            req = JsonUtil.MAPPER.readValue(ex.getRequestBody(), Dtos.FortSlotRequest.class);
        } catch (Exception e) {
            JsonUtil.sendJson(ex, 400, new Dtos.ErrorResponse("JSON 解析失败", e.getMessage()));
            return;
        }
        if (req.forts() == null) {
            JsonUtil.sendJson(ex, 400, new Dtos.ErrorResponse("缺少 forts 字段", "forts 是必填的"));
            return;
        }

        List<CompiledFort> compiled = new ArrayList<>(req.forts().size());
        List<String> names = new ArrayList<>(req.forts().size());
        try {
            for (Dtos.FortDto f : req.forts()) {
                if (f.code() == null) {
                    throw new IllegalArgumentException("阵型代码 code 不能为空");
                }
                String name = f.name() != null ? f.name() : "";
                compiled.add(Main.compileFort(new Fort(name, f.code())));
                names.add(name);
            }
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException
                 | ArrayIndexOutOfBoundsException e) {
            JsonUtil.sendJson(ex, 400, new Dtos.ErrorResponse("阵型代码非法", e.getMessage()));
            return;
        } catch (Exception e) {
            JsonUtil.sendJson(ex, 500, new Dtos.ErrorResponse("内部错误", e.toString()));
            return;
        }

        if (side == 0) {
            store.setP1(compiled, names);
        } else {
            store.setP2(compiled, names);
        }
        String sideName = side == 0 ? "1P" : "2P";
        JsonUtil.sendJson(ex, 200, new Dtos.FortSlotResponse(compiled.size(), names));
        System.out.println("[" + sideName + "] 已加载 " + compiled.size() + " 个阵容");
    }

    private void handleGet(HttpExchange ex) throws IOException {
        List<CompiledFort> forts = side == 0 ? store.getP1() : store.getP2();
        List<String> names = side == 0 ? store.getP1Names() : store.getP2Names();
        JsonUtil.sendJson(ex, 200, new Dtos.FortSlotResponse(forts.size(), names));
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        if (side == 0) {
            store.clearP1();
        } else {
            store.clearP2();
        }
        JsonUtil.sendJson(ex, 200, new Dtos.FortSlotResponse(0, List.of()));
    }
}
