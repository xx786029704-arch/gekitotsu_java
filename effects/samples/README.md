# 自定义 Effect 开发指南

## 目录结构

```
effects/
├── build_plugin.bat            ← 一键编译打包工具（双击即可使用）
├── _example_sort_units.jar     ← 示例插件（已禁用，去掉 _ 前缀即启用）
└── samples/
    ├── SortUnitsEffect.java    ← 示例源码（含详细注释）
    └── README.md               ← 本文件
```

## 快速开始（推荐方式）

### 1. 编写 Java 源文件

复制 `effects/samples/SortUnitsEffect.java` 作为模板，修改其中的逻辑。

需要重写的方法：

| 方法 | 说明 |
|------|------|
| `getName()` | 效果名称（显示在效果库中） |
| `getDescription()` | 功能描述 |
| `getAuthor()` | 作者署名 |
| `getVersion()` | 版本号 |
| `getParameters()` | 可配置参数列表 |
| `validate(params)` | 参数校验（返回空列表表示通过） |
| `execute(formation, params)` | 核心变换逻辑 |

参数类型：`INT`（整数）、`STRING`（字符串）、`BOOLEAN`（布尔）、`UNIT_ID`（单位下拉选择）

### 2. 一键打包

将写好的 `.java` 文件**拖放到** `effects/build_plugin.bat` 上，或在命令行运行：

```
effects\build_plugin.bat MyEffect.java
```

脚本会自动：查找 JDK → 编译 → 生成注册文件 → 打包为 JAR → 放入 effects 目录。

> 前提：需安装 JDK 21+，并设置 `JAVA_HOME` 环境变量或在 PATH 中可用 `javac`。

### 3. 安装启用

- JAR 已自动生成到 `effects/` 目录，重启"激突Kit"即可在效果库中看到
- 如需**禁用**某个插件：在文件名前加 `_` 下划线前缀即可
- 如需**启用**被禁用的插件：去掉文件名中的 `_` 前缀

## 高级方式（手动编译）

```bash
javac -encoding UTF-8 -d out -cp "target/classes" MyEffect.java
mkdir out\META-INF\services
echo org.example.effects.MyEffect > out\META-INF\services\org.example.GUI.Effect
cd out && jar cf ..\effects\my_effect.jar META-INF org
```
