# gekitotsu_java HTTP API 文档

## 启动服务

### 方式一：双击启动脚本
- Windows：双击 `start.bat`
- Linux/Mac：`./start.sh`

### 方式二：命令行启动
```bash
java -jar target/gekitotsu_java-1.3.4.jar --mode server --port 8080
```

### 启动选项
| 选项 | 说明 | 默认 |
|------|------|------|
| `--mode <interactive\|batch\|server>` | 运行模式 | `interactive` |
| `--port <端口号>` | server 模式监听端口 | `8080` |
| `--help` | 显示帮助 | - |

也可通过 `config.ini` 配置（`MODE`、`PORT`、`MAX_FRAME_LIMIT`、`MAX_THREADS` 等），命令行优先级高于配置文件。

启动后看到如下输出表示就绪：
```
正在导入阵容...
阵容导入完成！
对战模拟服务启动中...
监听: http://localhost:8080
已加载阵型: 1P=1个, 2P=1个
线程数: 8
按 Ctrl+C 停止
```

## API 端点

### 一次性对战

#### GET /health
健康检查。

**响应**：
```json
{
  "status": "ok",
  "p1_count": 1,
  "p2_count": 1,
  "threads": 8,
  "max_frames": 65536
}
```

#### POST /battle
单局对战，每次都传完整阵型代码。适合一次性测试或偶发调用。

**请求体**：
```json
{
  "fort1": {"name": "AI阵型1", "code": "0abc..."},
  "fort2": {"name": "基准",    "code": "0xyz..."},
  "max_frames": 65536
}
```
- `fort1.name` / `fort2.name`：阵型名称（可空字符串）
- `fort1.code` / `fort2.code`：阵型代码（必填，长度 ≥6，且为 6 的倍数）
- `max_frames`：可选，本局最大帧数，缺省用 `config.ini` 里的 `MAX_FRAME_LIMIT`

**响应 200**：
```json
{
  "status": 1,
  "winner_hp": 87,
  "frames": 12345,
  "time_ms": 45.2,
  "fort1_name": "AI阵型1",
  "fort2_name": "基准"
}
```
- `status`：`1`=1P胜, `2`=2P胜, `0`=平局, `-1`=超时, `-2`=异常

**响应 400**（阵型代码非法）：
```json
{"error": "对战失败", "detail": "阵型代码非法: 阵容代码长度不足: X [abc]"}
```

#### POST /battles
批量对战。一次提交多局，并发执行，按提交顺序返回结果。

**请求体**：
```json
{
  "battles": [
    {"fort1": {...}, "fort2": {...}, "max_frames": 65536},
    {"fort1": {...}, "fort2": {...}}
  ]
}
```

**响应 200**：
```json
{
  "results": [
    {"status": 1, "winner_hp": 87, "frames": 12345, "time_ms": 45.2, "fort1_name": "...", "fort2_name": "..."},
    {"status": -2, "winner_hp": 0, "frames": 0, "time_ms": 0, "error": "阵型代码非法: ..."}
  ],
  "total_time_ms": 234.5
}
```
- 单局编译失败时该位置的 `status=-2` 且带 `error` 字段，其他局不受影响

### 持久化槽位 + 触发对战

适合训练循环：先把基准阵型上传一次，之后每轮只换 AI 阵型，触发对战。

#### PUT /p1
上传 1P 阵容列表（覆盖式，替换原有 1P 列表）。服务端预编译并缓存。

**请求体**：
```json
{
  "forts": [
    {"name": "AI-001", "code": "0abc..."},
    {"name": "AI-002", "code": "0def..."}
  ]
}
```

**响应 200**：
```json
{"count": 2, "names": ["AI-001", "AI-002"]}
```

**响应 400**（任一阵型代码非法）：整个 PUT 失败，**已有列表保持不变**（原子性）。

#### GET /p1
查看当前 1P 阵容。

**响应**：
```json
{"count": 2, "names": ["AI-001", "AI-002"]}
```

#### DELETE /p1
清空 1P 阵容。响应：`{"count": 0, "names": []}`

#### PUT /p2 / GET /p2 / DELETE /p2
2P 槽位同上。

#### POST /run
对当前 1P × 2P 阵容做笛卡尔乘积对战。

**请求体**（可空）：
```json
{"max_frames": 65536}
```
- `max_frames` 可选，缺省用 `MAX_FRAME_LIMIT`

**响应 200**（与 `/battles` 同格式）：
```json
{
  "results": [
    {"status": 1, "winner_hp": 87, "frames": 12345, "time_ms": 45.2, "fort1_name": "AI-001", "fort2_name": "基准"},
    {"status": 1, "winner_hp": 87, "frames": 12345, "time_ms": 45.2, "fort1_name": "AI-002", "fort2_name": "基准"}
  ],
  "total_time_ms": 234.5
}
```

**响应 400**（1P 或 2P 为空）：
```json
{"error": "阵容未配置", "detail": "1P 有 0 个，2P 有 5 个，都至少需要 1 个"}
```

**注意**：`/run` 是同步接口，会等所有对战完成才返回。1000 局大约 1 分钟。如需更大批量，建议客户端分批调用。

## 阵型代码格式

阵型代码是 `0-9a-zA-Z` 字符组成的字符串：
- 第 1 位：要塞类型（0/1/2 对应 Core/BossCore/BossCore2）
- 接下来 5 位：要塞坐标（5 位 61 进制）
- 之后每 6 位一个单位：第 1 位是单位类型（`pskey` 索引），后 5 位是坐标和朝向

长度必须 ≥6 且为 6 的倍数。详情见 `Main.compileFort` 和 `Setting.CompileForts`。

## Python 调用示例

```python
import requests

BASE = "http://localhost:8080"

# 单局对战
resp = requests.post(f"{BASE}/battle", json={
    "fort1": {"name": "AI", "code": "0abc..."},
    "fort2": {"name": "基准", "code": "0xyz..."}
}).json()
print(resp)

# 批量对战（推荐训练时使用）
resp = requests.post(f"{BASE}/battles", json={
    "battles": [
        {"fort1": {"name": "AI1", "code": "..."}, "fort2": {"name": "基准", "code": "..."}},
        {"fort1": {"name": "AI2", "code": "..."}, "fort2": {"name": "基准", "code": "..."}}
    ]
}).json()
for r in resp["results"]:
    print(r["status"], r.get("winner_hp"))
```

完整示例见 `clients/python_example.py`。

## 错误码汇总

| HTTP 状态 | 含义 |
|-----------|------|
| 200 | 成功 |
| 400 | 请求错误（JSON 解析失败、阵型代码非法、缺少必填字段） |
| 405 | HTTP 方法不对（如 GET /battle） |
| 500 | 服务器内部错误 |

## 常见问题

**Q: 服务起不来？**
检查端口是否被占用，或换一个端口：`java -jar xxx.jar --mode server --port 9000`

**Q: 阵型代码怎么生成？**
看 `1P.txt` 和 `2P.txt` 里的示例，或参考原始游戏。AI 训练时可以随机生成 `0-9a-zA-Z` 字符串然后通过 `/battle` 验证。

**Q: 怎么加大并发？**
修改 `config.ini` 里的 `MAX_THREADS`（默认为 CPU 核数），重启服务生效。

**Q: 想保留原有的交互菜单？**
默认就是交互模式，直接 `java -jar xxx.jar` 即可。
