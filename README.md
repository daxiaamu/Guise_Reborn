# Guise Reborn

[![Release](https://img.shields.io/github/v/release/daxiaamu/Guise_Reborn?include_prereleases&sort=semver&display_name=tag&style=flat-square&label=Release)](https://github.com/daxiaamu/Guise_Reborn/releases)
[![Release downloads](https://img.shields.io/github/downloads/daxiaamu/Guise_Reborn/total?style=flat-square&label=Downloads&color=2ea44f)](https://github.com/daxiaamu/Guise_Reborn/releases)
[![Android CI](https://github.com/daxiaamu/Guise_Reborn/actions/workflows/android.yml/badge.svg?branch=main)](https://github.com/daxiaamu/Guise_Reborn/actions/workflows/android.yml)
[![License](https://img.shields.io/github/license/daxiaamu/Guise_Reborn?style=flat-square&label=License)](LICENSE)
[![Last commit](https://img.shields.io/github/last-commit/daxiaamu/Guise_Reborn?style=flat-square&label=Last%20commit)](https://github.com/daxiaamu/Guise_Reborn/commits/main)
[![Commit activity](https://img.shields.io/github/commit-activity/m/daxiaamu/Guise_Reborn?style=flat-square&label=Commits)](https://github.com/daxiaamu/Guise_Reborn/commits/main)

[![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/about/versions/10)
[![minSdk 29](https://img.shields.io/badge/minSdk-29-3DDC84?style=flat-square)](https://developer.android.com/tools/releases/platforms#10)
[![targetSdk 37](https://img.shields.io/badge/targetSdk-37-3DDC84?style=flat-square)](https://developer.android.com/about/versions/17)
[![arm64-v8a](https://img.shields.io/badge/ABI-arm64--v8a-6f42c1?style=flat-square)](#当前技术基线)
[![Modern Xposed API 102](https://img.shields.io/badge/Modern%20Xposed-API%20102-8b5cf6?style=flat-square)](https://github.com/libxposed)
[![Kotlin](https://img.shields.io/github/languages/top/daxiaamu/Guise_Reborn?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-6750A4?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io/)

[![Stars](https://img.shields.io/github/stars/daxiaamu/Guise_Reborn?style=flat-square&logo=github&label=Stars)](https://github.com/daxiaamu/Guise_Reborn/stargazers)
[![Forks](https://img.shields.io/github/forks/daxiaamu/Guise_Reborn?style=flat-square&logo=github&label=Forks)](https://github.com/daxiaamu/Guise_Reborn/forks)
[![Watchers](https://img.shields.io/github/watchers/daxiaamu/Guise_Reborn?style=flat-square&logo=github&label=Watchers)](https://github.com/daxiaamu/Guise_Reborn/watchers)
[![Contributors](https://img.shields.io/github/contributors/daxiaamu/Guise_Reborn?style=flat-square&label=Contributors)](https://github.com/daxiaamu/Guise_Reborn/graphs/contributors)
[![Open issues](https://img.shields.io/github/issues/daxiaamu/Guise_Reborn?style=flat-square&label=Issues)](https://github.com/daxiaamu/Guise_Reborn/issues)
[![Code size](https://img.shields.io/github/languages/code-size/daxiaamu/Guise_Reborn?style=flat-square&label=Code%20size)](https://github.com/daxiaamu/Guise_Reborn)
[![Repository size](https://img.shields.io/github/repo-size/daxiaamu/Guise_Reborn?style=flat-square&label=Repo%20size)](https://github.com/daxiaamu/Guise_Reborn)

Guise Reborn 是 Guise 的社区维护续作，是一个面向 LSPosed/Modern Xposed 的应用运行环境伪装模块。它可针对用户明确选中的目标应用，修改设备、系统、标识符、网络、SIM、Wi-Fi、定位、基站、语言、时区、电池及隐私相关 API 的返回结果。

> 机场推荐：[白月光，稳定高速](https://www.sibker.com/register?invite_code=2XQR1UUz)

本仓库保留公开源码仓库的 Git 历史，并基于原作者公开表示“感兴趣的可以接手继续开发”的版本继续维护：

- 原项目：[Houvven/Guise](https://github.com/Houvven/Guise)
- 本仓库采用的源码上游：[AlliotTech/Guise](https://github.com/AlliotTech/Guise)
- 上述源码仓库的 GitHub Fork 上游：[shenghuang147/Guise](https://github.com/shenghuang147/Guise)

> 维护者：大侠阿木。当前正式版本为 `2.0.1`；README 中的维护版内容均位于原作者说明之前。

## 当前技术基线

| 项目 | 当前状态 |
| --- | --- |
| 最低系统 | Android 10 / API 29 |
| 编译与目标版本 | Android 17 / API 37 |
| CPU 架构 | 仅 `arm64-v8a` |
| Java/Kotlin | Java 17 字节码、Kotlin 2.4.10 |
| 构建系统 | Gradle 9.6.1、Android Gradle Plugin 9.3.1、KSP 2.3.6 |
| UI | Jetpack Compose BOM `2026.06.01`、Material 3、edge-to-edge |
| Xposed | libxposed Modern API 102 与 Xposed service 102 |
| 数据与异步 | MMKV 2.4.1、Room 2.8.4、kotlinx.coroutines 1.11.0、kotlinx.serialization 1.11.0 |
| 发布优化 | Release 启用 R8、资源收缩及 Baseline Profile |

MMKV 2 官方 Android 原生库当前仅提供 64 位构建，因此 Guise Reborn 暂时只生成 `arm64-v8a` APK。32 位设备和仅提供 32 位用户空间的系统不能安装当前构建。

## 内置数据规模

当前安装包直接内置设备与运营商数据，无需联网后再下载：

| 数据 | 当前规模 |
| --- | ---: |
| 内置设备品牌 | 26 个 |
| 内置可选机型 | 8,533 个 |
| 全球运营商记录 | 1,597 条 |
| 运营商覆盖国家/地区 | 226 个 |
| MCC 覆盖 | 233 个 |

机型数量按配置界面实际支持的手机、平板、电视、电视盒子和手表类型去重统计；运营商数量按内置有效 MCC/MNC 记录统计。设备品牌、真实 Build 品牌/制造商值及其全部型号统一维护在单个 `devices.json`；对应数据版本和许可证见 `devices.NOTICE.txt` 与 `carriers.NOTICE.txt`。

与原版 Guise 1.1.2 相比，面向目标应用的独立伪装/隐私配置由 **35 项增加到 40 项**，净新增 **5 项**：制造商、Build ID、显示大小（DPI）、时区和应用列表可见性。这里不把主题、预测性返回、更新检测等管理端设置计入伪装项目。

## 基于原版的维护与重构

Guise Reborn 并非只为原版更换界面。维护版保留按应用伪装的核心思路和原有主要 Hook，同时重构管理端、Xposed 运行层、配置与作用域语义、日志、模板、更新及发布链路。以下内容直接按改动所属部分整理；新增能力、旧 Hook 改进和不再维护的旧实现均在对应章节说明，不另列一套重复的更新清单。

### 1. 整体工程与底层架构

- 从旧 Android/Xposed 工程迁移到 API 37、AGP 9、Kotlin 2.4 与当前 AndroidX 组件。
- 管理界面全面迁移到 Jetpack Compose 与 Material 3，移除旧 View/过时主题实现所带来的维护包袱。
- 模块入口、包加载回调、Hook 辅助层、日志和配置访问迁移到 Modern Xposed API 102；旧 XposedBridge/XposedHelpers 入口不再作为主线实现维护。
- 使用应用私有存储作为配置持久化源，并通过 Xposed Remote Preferences 向目标进程提供只读镜像；移除世界可读配置、旧 `XSharedPreferences` 读取和直接修改 LSPosed 数据库的做法。
- 日志链路重新设计为“Xposed 官方日志 + 显式组件批量投递 + 管理端 Room 归档”：移除原来导出的日志 `ContentProvider`、目标进程逐条写管理端数据库以及依赖 Activity 生命周期批量落盘的实现。目标进程只保留每进程最近 12 条待投递记录，管理端去重后最多保存 2,000 条，避免日志无限增长。
- 目标进程的日志会通过显式组件批量投递至 Guise 的本地数据库，不再错误地向 Hook 进程中的只读 Xposed Remote Preferences 写入；接收端校验发送 UID，在绕过目标应用包可见性限制的同时避免其他应用伪造日志。日志页无需手动刷新，支持错误/信息/调试级别筛选、包名/进程/Hook 类别/正文/堆栈搜索、完整异常展开与诊断信息导出。“详细日志”默认关闭，只控制成功安装 Hook 的调试明细，错误和必要状态始终记录。
- 运行时日志包含时间、级别、目标包名、进程名、Hook 类别、消息和完整异常堆栈，不采集页面内容、账号、输入、照片、位置、网络内容或系统全量 Logcat。日志数据库不参与系统备份；本次重构不迁移旧日志数据库。
- Hook 按设备、系统、唯一标识、网络、SIM、Wi-Fi、定位、基站、电池、截图、时区等职责拆分；单个 Hook 失败会被隔离，避免拖垮整个目标进程。
- 目标进程会先把非默认配置编译成最小 Hook 计划，只构造真正启用的 Hook 组；未配置版本伪装时不再无条件修改 `PackageManager.getPackageInfo()`。显示密度、应用版本、唯一标识和窗口标志等固定 API 使用精确方法签名，减少无关重载被修改的范围。
- 日志投递不再为了取得目标应用 `Context` 而 Hook `Application.attach` 或 `Activity.onCreate`。关闭详细日志且没有需要投递的事件时，不查找目标应用 `Context`、不排队重试也不发送广播；有事件时才短时获取当前 `Application`，避免日志功能额外留下常驻生命周期 Hook。
- 移除目标进程启动后的 Hook 成功 Toast；成功明细进入可选的详细日志，错误和必要状态始终记录，避免打断目标应用正常使用。
- 数据预设从 UI 代码移入资源：Android 版本、SDK、DPI、网络、语言等集中维护在 `app/src/main/res/raw/presets.json`。
- 设备目录由 SQLite 与 UI 映射重构为单一层级化 `devices.json`：26 个品牌直接包含 8,533 个可选型号，并分别保存显示名、数据库来源键、`Build.BRAND` 和 `Build.MANUFACTURER`。目录不参与启动首帧，首次进入设备选择或执行一键随机时在 IO 协程解析一次并缓存。构建测试会校验品牌键、型号唯一性、必填字段及关键厂商大小写。
- 升级 Room、MMKV、KSP、协程和序列化；移除 Accompanist、Ktor 1.x、旧 SQLite shell 及原有通用 `lib` 模块。
- 导入导出改用 MediaStore 与系统文件选择器，移除“所有文件访问”、旧外部存储权限及明文网络配置。
- Release 和 Guise Test 均启用 R8；Guise 只保留 LSPosed 必须识别的模块入口、构造器和框架回调，私有实现及配置类仍可混淆。配置编辑状态改用显式类型安全映射，不再依赖混淆后不可靠的同名字段反射。正式构建还会自动校验入口没有被改名、内部名称没有被意外保留，避免“模块已进入作用域但实际未加载”的静默失效。

### 2. Xposed 作用域与启用逻辑

- 应用列表中的勾选状态现在是 Guise 的唯一启用节点，同时负责同步 LSPosed 作用域。
- 配置以应用私有存储作为持久化源，并镜像到 Xposed Remote Preferences 供目标进程读取；首次升级会先保留既有远程配置。框架服务重新连接时会按勾选状态校准实际作用域，避免框架远程存储重建或设备重启后出现“界面已勾选、实际未加载”的分叉。
- 勾选应用：写入启用状态并加入作用域；取消勾选：停止对该应用执行 Guise Hook，并从作用域移除。
- 不再把“存在配置”误认为“已启用 Hook”；应用可保留配置但暂时取消勾选。
- 已保存为空配置时会自动取消勾选，避免作用域中残留一个实际没有任何配置的目标。
- 删除/清空应用配置和取消勾选已分离，降低用户对“清除配置是否会关闭 Hook”的理解成本。
- 勾选状态变化不会立即打乱当前列表；选中的应用只在下次进入列表或主动刷新时置顶。
- 移除旧的“模块未激活”和“不检测模块激活状态”逻辑。Xposed service 未连接只表示管理端当前不能使用对应服务能力，不再被展示成武断的“模块未激活”。
- 隐藏桌面入口时仍保留框架模块入口，避免在 LSPosed 中也无法重新进入 Guise。

### 3. 设备、系统与应用版本 Hook

- 支持品牌、制造商、型号、设备代号、产品、主板、硬件/CPU 代号、Build ID 和 Fingerprint；`Build.BRAND` 与 `Build.MANUFACTURER` 不再被错误地视为同一字段。
- 品牌和型号选择使用单一内置 JSON 设备目录。界面显示名、目录键、`Build.BRAND` 与 `Build.MANUFACTURER` 明确分离，例如“一加”只用于显示，实际写入 `OnePlus`；OPPO 写入规范大写值。主动选择品牌时会填入可继续编辑的默认制造商，制造商留空则不 Hook。型号只在数据明确提供设备字段时联动 `DEVICE`，不再猜测或覆盖独立的 `PRODUCT`。
- 一键随机会从同一台设备和同一组 Android 版本数据生成品牌、制造商、型号、设备/产品代号、版本、SDK、Build ID 与 Fingerprint，避免各字段来自互不相关的随机样本。Fingerprint 的独立随机按钮也会同步生成对应 Build ID。
- Android 版本与 SDK/API 预设补齐至 Android 17 / API 37，包含 Android 12L / API 32。
- **新增显示大小（DPI）Hook**：支持 72–1000 的 `densityDpi`，覆盖 `Resources.getDisplayMetrics()`、`Resources.getConfiguration()`、`Display.getMetrics()` 和 `Display.getRealMetrics()`，同步修正 `density`、`scaledDensity` 及 dp 配置，并保留原字体缩放比例。界面同时估算对应的“最小宽度”dp，方便和开发者选项中的显示大小建立关系。
- DPI 修改只影响目标应用进程看到的 `DisplayMetrics`/`Configuration`，不会修改系统全局显示大小。
- **改进版本号与版本名 Hook**：原版依赖修改目标应用 `BuildConfig.VERSION_*` 的 `static final` 字段；Android 17 对面向 API 37 应用收紧了这类反射修改，维护版改为在目标应用查询自身时修改系统返回的完整 `PackageInfo`。
- 较旧系统和较旧目标应用仍保留设备构建字段的兼容 Hook；但面向 API 37 的应用不能再把 `static final` 反射修改视为可靠能力。

### 4. 标识符、网络、SIM 与运营商 Hook

- 支持 Android ID/SSAID、IMEI、手机号等目标应用可读取标识的伪装。
- 随机数据改为符合常见平台形态：Android ID 使用 16 位十六进制，MAC 使用合法的本地管理单播地址，IMEI 带 Luhn 校验位，经纬度保持在全球有效范围内。
- **改进 IMEI Hook**：从原版仅处理带卡槽参数的 `getImei(int)`，扩展到 `getImei` 全部重载、`getPrimaryImei`、`getDeviceId` 和 TAC。当前配置仍为单值，应用按任一卡槽读取时均返回该值，因此双卡设备上的 IMEI 1/2 会相同；界面已加入说明，避免误解为只配置第一卡槽。
- Android ID 保留 `Settings.Secure`/`Settings.System` 常用读取入口以及 WebView 等独立进程适配；目标进程初始化后直接使用当前配置，不再在每次读取标识时重复访问 Remote Preferences。
- 支持网络类型、Wi-Fi SSID、BSSID、Wi-Fi MAC 地址。
- 支持 SIM 运营商代码、名称和国家/地区代码，并可从全球 MCC/MNC 运营商数据中搜索选择。
- 运营商预设来自 [pbakondy/mcc-mnc-list](https://github.com/pbakondy/mcc-mnc-list)，编码依据 [ITU-T E.212](https://www.itu.int/rec/T-REC-E.212/en)。
- 支持 LAC/CID 基站参数，以及分别隐藏 Wi-Fi 定位信息和基站定位信息的独立开关。

### 5. 定位、语言、时区、电池与截图 Hook

- 支持经纬度伪装及随机偏移。坐标伪装只改写 `Location` 经纬度读取和最近位置结果，不再伪造卫星状态、NMEA 数据或额外触发定位监听器回调。
- 经纬度修改目标应用通过 Android `Location` 对象读取到的 GPS、网络与融合定位坐标；IP 定位、厂商私有接口和远程服务结果不受影响。
- “隐藏 Wi-Fi 定位信息”会过滤附近热点扫描结果以及当前连接的 SSID、BSSID、MAC、MLO 接入点等标识，但不会关闭 Wi-Fi，也不会伪造定位提供器不存在；该开关优先于自定义 Wi-Fi 标识。
- “隐藏基站定位信息”覆盖同步查询、异步查询、现代回调、旧式监听以及 GSM、CDMA、LTE、NR、WCDMA、TD-SCDMA 等基站标识字段，同时保留移动网络、运营商和信号状态；该开关优先于 LAC/CID 伪装。
- 支持目标应用读取到的语言/地区环境。实现改为设置目标进程默认 `Locale`，不再常驻改写每个 `Locale` 实例的语言、国家与显示名称方法。
- **新增时区 Hook**：设置目标进程默认 `java.util.TimeZone`，Android 的 `java.time`、ICU 格式化和 `ZoneId.systemDefault()` 会从该进程默认值派生；不再逐个拦截时区实例方法，因此应用显式创建的其他时区仍保持原意。时区可从系统时区库选择或单独随机，并受右上角“一键随机”控制；它只改变目标应用读取和格式化本地时间所用的默认时区，不修改系统时钟。
- 电池电量可配置或在 0–100 范围随机，并受“一键随机”控制。
- Fingerprint、时区、电池等可随机字段均提供就近的独立随机按钮，不必每次使用全局随机。
- **重写强制截图 Hook**：原版只在参数恰好等于 `FLAG_SECURE` 时尝试清除，组合标志容易漏过；维护版将文本参数改为“允许强制截图”开关，覆盖 `setFlags`、`addFlags`、`setAttributes`、`WindowManager` 的 add/update 路径，并在 Activity 创建和恢复时清理已存在标志。未启用时不改变应用原有行为，DRM/受保护视频层、厂商私有渲染或应用自行遮挡仍可能无法截图。
- 保留原版“禁止截图”方向的能力：启用对应值时仍会向目标 Activity 添加 `FLAG_SECURE`。

### 6. 隐私与空白通行 Hook

- 支持联系人、图片、视频、音频的“空白通行证”，使目标应用查询对应数据时得到空结果。
- **改进联系人/媒体空白通行**：原版只比较少数固定 URI 并返回 `null`；维护版同时拦截 `ContentResolver` 和 `ContentProviderClient` 查询，按 authority/path 识别联系人、图片、视频和音频，并返回保留请求列结构的空 `Cursor`，适配现代 MediaStore 查询链路。
- **新增“应用列表”空白通行**：目标应用通过常用 `PackageManager` 列表、Intent 查询、UID 包名数组和指定包信息接口时，只能看到自身与系统应用（含更新后的系统应用）。
- 应用列表过滤同时覆盖目标进程内的 `ApplicationPackageManager` 和 `IPackageManager` Binder 代理，但不会 Hook `system_server` 或扩大模块作用域；原生代码、应用自带 Binder 客户端及厂商私有查询接口仍可能绕过，因此设置页会明确提示这一能力边界。
- 支持窗口隐私/截图相关 Hook；权限不足、厂商私有媒体接口或应用自行维护的数据索引仍可能绕过通用 Android API，需使用 Guise Test 或目标应用实测。

### 7. 应用列表、配置与模板交互

- 应用列表支持搜索、系统/用户应用筛选与弱化标识、安装时间/更新时间等排序方式。
- 系统应用/用户应用标记不再占用包名位置，列表密度在信息量和可读性之间重新调整。
- 列表复选框的裁剪范围修正，不会在尚未进入底部导航区域前提前消失。
- 配置项统一使用可复用的输入框、辅助说明、预设列表、随机和删除操作布局，修正不同字段右侧按钮不在同一基线的问题。
- 随机、列表和删除按钮使用一致尺寸与间距；无数据时采用弱化色，有数据或已改动时使用主题色表达状态。
- 保存按钮仅在配置确有变化时可用，保存成功后立即恢复为不可用。
- 从配置页返回时：
  - 有未保存修改会先询问是否保存；
  - 已保存且目标应用正在运行时，再询问是否停止/重启以生效；
  - 目标应用没有运行时不重复打扰；
  - 用户可选择忽略，本次不执行进程操作；
  - 清空全部配置并确认保存后自动取消该应用勾选。
- 从配置页返回不会强制刷新应用列表，避免刚勾选的应用因排序/状态刷新突然从当前位置消失。
- 模板系统支持随安装包发布的版本化默认模板。当前内置“空白通行证”和“允许强制截图”两项通用模板；已有同参数模板会被直接识别，不会重复创建。
- 默认模板使用稳定 ID、独立内容版本与 Room 导入账本管理。后续版本可以补充新模板，但不会覆盖用户已经修改过的内容；用户删除默认模板后会留下删除墓碑，升级或切换语言时均不会再次导入。
- 模板删除与墓碑写入在同一数据库事务中完成，Room v3 → v4 采用无损迁移，现有模板及其目标应用关系保持不变。
- 模板的应用选择页每次进入都会重新扫描已安装应用，下拉刷新也强制走同一套 PackageManager 全量查询，不再复用可能缺少新安装应用的全局缓存。
- 模板列表使用强单色小标签区分“通用”和“专属”，两类采用不同主题色；新建和修改模板时则使用单选分段按钮，明确模板类型只能二选一。

### 8. 停止应用、重启与生效提示

- 勾选或取消勾选应用时都会提示需要重启目标应用以使 Hook/作用域变化生效。
- 进程控制采用三级回退：优先尝试 ROOT，失败后尝试 Xposed service，再失败则引导用户进入应用信息页手动停止。
- 进入系统应用信息页后，手动停止/重启提示改用系统 Toast 显示，避免提示仍留在后台的 Guise 页面而不可见。
- 保留 ROOT 分支是为了兼容尚未成功 Hook、框架服务不可用或不能依赖目标进程内 Hook 的场景。
- 不为了停止一个目标应用而 Hook 系统框架；Xposed service 能力只在框架已提供且连接成功时使用。
- 配置页不再常驻放置容易误触的“停止/重启”按钮，而是把操作放到保存和退出的实际生效流程中。

### 9. 外观、主题与页面交互

- 原版管理端的旧 View/主题界面已整体替换；主界面、应用列表、配置编辑、模板、日志、设置、关于、许可证和更新弹窗统一使用 Material 3。
- 全面适配 edge-to-edge、系统状态栏/导航栏明暗图标、底部手势区域和原生风格底部导航。
- 主题模式支持“跟随系统、浅色、深色”。
- 支持 Monet 动态取色：根据系统壁纸生成配色；关闭后可使用自定义主题色，而不是强制使用 Guise 自有取色算法。
- 壁纸取色提示、未连接 Xposed service 提示、Snackbar 和选中态均跟随当前 Material 色板。
- 设置项补齐图标、分组标题与分割线；可进入的项目使用统一的尾部指示样式。
- 预测性返回默认开启，支持页面随手势进度移动；从右侧返回时页面向右退出，从左侧返回时向左退出，不人为放大手势距离。
- 预测性返回在主页面、配置、模板编辑和模板应用选择等导航层级使用同一套前景、背景、遮罩及完成/取消动画，修复停在半截、配置页响应延迟和页面层级混淆。Android 13 及以上还需要系统启用预测性返回；改变系统开关后通常需要重启应用。
- 阿拉伯文继续支持 RTL，英文、简体中文、日文和阿拉伯文资源保持同步，不因界面重构回退语言覆盖。

### 10. 设置、关于与开放源代码信息

- “关于”中明确显示维护者“大侠阿木”和原作者“Houvven”。
- 大侠阿木条目说明“维护和开发 2.0.0 及之后的版本”，不提供捐赠入口。
- 原作者条目包含说明、支付宝/微信捐赠入口、“未成年人请勿捐赠”和捐赠昵称备注说明。
- 邮箱、Coolapk 等旧反馈入口合并为“查看源代码”，用于在 GitHub 查看代码和提交 Issues。
- 新增应用内“开放源代码许可”页面，列出本应用、依赖、数据来源、项目主页及许可证链接。
- 修复许可证列表滚动到顶部时的回弹/抽搐问题。

### 11. GitHub 更新机制

- 支持启动后延迟检测更新和设置页手动检测更新；版本行本身不可点击，仅“检查更新”按钮执行请求。
- 检测到新版本后显示红点；“稍后”只关闭本次提示，“忽略此版本”会在下一版本发布前不再自动提示该版本。
- 不支持强制更新，用户始终可以稍后处理。
- 更新说明支持可点击的 HTTPS 超链接。
- 更新清单以 GitHub API 为权威源，并发准备 jsDelivr、Fastly、Gcore 和 GitHub Raw 回退，权威源失败时自动使用可用镜像中版本号最高的清单。
- 发布 Guise Release 后由 GitHub Actions 下载已上传附件、计算 SHA-256，并自动提交 `latest-release.json`；Guise Test 的独立 Tag 不触发 Guise 更新清单。
- GitHub Actions 分别维护 `latest-release.json`（正式版）和 `latest-prerelease.json`（预发布版）。预发布构建同时检查两个通道并选择版本号较高者，正式构建只接受带正式通道标记的清单；`guise-test-*` 独立 Tag 不会进入 Guise 更新清单。
- 为保证双通道上线前发布的预览版仍可继续升级，在首个正式版发布前，预发布工作流会同步维护旧版客户端认识的 `latest-release.json` 兼容入口。新客户端会校验清单中的 `prerelease` 标记，正式构建不会将该兼容入口识别为更新；首个正式版发布后兼容入口自动结束。
- 清单提交后工作流会调用 jsDelivr 官方 purge 端点请求清除 `main` 分支别名缓存，尽量缩短 CDN 继续返回旧版本的时间；客户端会比较所有成功来源并采用版本号最高的清单，GitHub API 始终作为权威来源。
- APK 下载清单支持 `apkUrls` 候选列表，按 CDN/镜像/官方 GitHub 地址依次交给系统 `DownloadManager`；下载失败、记录丢失、返回错误文件或 SHA-256 不匹配时会自动尝试下一来源。
- 当前发布工作流生成 10 个经 Release 附件单字节探测验证的代理/CDN 地址，并把官方 GitHub Release 地址作为最终兜底；失效候选可在工作流中集中维护，无需修改客户端代码。
- 下载计划会持久化候选地址、当前来源和预期 SHA-256；即使 Guise 进程被系统回收，下载完成广播仍可继续校验或切换来源。
- `apkUrl` 单地址字段继续兼容旧更新清单；新版本发布应同时提供 `apkUrls` 和 `apkSha256`。
- 下载弹窗使用圆形进度指示和不可点击的“正在下载”，不展示百分比和文件大小；下载完成后可继续安装。
- 有 ROOT 时可通过独立 ROOT shell 静默覆盖安装，并在安装导致旧进程退出后继续执行 `am start` 重新启动 Guise；ROOT 安装失败则回退系统安装器。

### 12. 启动与列表加载性能

本轮对启动链路进行了实机分段测量，并只保留可解释、可维护的优化：

- Android 12+ 首帧就绪后立即移除系统 Splash 的退出动画，不添加人为延时或 KeepOnScreenCondition。
- Room 日志数据库只在 Application 初始化时取得惰性数据库句柄，旧日志库清理和实际 I/O 均进入后台调度器；不再由 ContentProvider 抢在 Application 之前启动数据库。
- 壁纸颜色读取移到 `Dispatchers.IO`，MaterialKolor 配色计算移到 `Dispatchers.Default`；首帧先使用系统动态色或基础色板。
- 应用包扫描延后一帧并移到 IO 调度器，不阻塞第一个 Compose 帧。
- 完整应用扫描不再预先解码所有图标；只为屏幕上实际参与组合的项目加载图标，并使用 8 MiB LRU 缓存。
- Release APK 加入 Guise 自身与 Xposed 适配层的 Baseline Profile，并继续合并 AndroidX 依赖提供的 Profile。
- 主题色状态使用原生整型 Compose State，避免不必要的装箱。
- 对没有稳定收益的“减少一层布局”等尝试已撤回，不把偶然数据当作优化成果。

在测试设备 `7e49aeb9` 上，以 `am force-stop` 后执行 `am start -W` 的相同方式测得：

| 构建/应用 | 冷启动 TotalTime |
| --- | ---: |
| 优化前 Guise Debug | 约 568–580 ms |
| Guise Release + R8 + Profile 编译后 | 约 136–146 ms |

以上数据只用于说明本轮优化方向，不代表所有设备的保证值。Debug 与 Release 不应直接比较；首次安装、刚覆盖安装、设备温度、系统调度以及 Baseline Profile 尚未完成编译时，启动耗时会明显更高。

当前应用元数据仍会在首次进入列表时进行一次完整扫描，只是已移出首帧且不再批量解码图标。后续可在持久化快照基础上结合 `PackageManager.getChangedPackages(sequenceNumber)`、`PACKAGE_ADDED/REMOVED/REPLACED` 广播和周期性全量校验实现增量索引；该增量缓存目前尚未宣称为已实现功能。

### 13. 兼容性变化与旧实现移除

这部分不是临时缺失功能，而是面向新 Android 与 Modern Xposed 后有意做出的取舍：用户配置和模板数据会尽量迁移，已经失效、不安全或会制造双重状态的内部实现不继续背负兼容层。

- **系统要求提高**：管理应用最低支持 Android 10 / API 29，目标和编译版本为 API 37。
- **框架要求变化**：主线面向支持 libxposed Modern API 102/service 102 的框架；旧 XposedBridge/XposedHelpers 入口不再作为主实现维护。
- **ABI 限制**：只提供 `arm64-v8a`，不再包含 32 位 MMKV 原生库。
- **作用域语义变化**：列表勾选才代表启用 Hook；仅保存配置不会隐式启用应用。
- **激活状态简化**：移除“未激活”“不检测模块激活状态”和“跟随 LSPosed 配置”；管理端只报告 Xposed service 是否可用，作用域只由应用勾选同步。
- **版本伪装变化**：API 37 目标应用优先依赖 `PackageInfo` 返回值修改，旧式 `static final BuildConfig` 篡改不再被视为可靠实现。
- **设备配置兼容**：旧配置没有制造商和 Build ID 字段时仍可读取；旧的内部品牌键、本地化显示名及跟随品牌的制造商会在打开配置时规范为真实 Build 值。制造商留空表示不 Hook `Build.MANUFACTURER`，不会再隐式回退到品牌。新版一键随机会从同一品牌/型号记录填入规范品牌和制造商，因此保存后的具体随机值形态会与旧版不同。
- **日志实现变化**：移除旧日志 Provider、目标进程直接写管理端数据库和 Hook 成功 Toast；旧日志数据库不迁移，新的运行日志按容量归档。
- **存储权限变化**：使用系统媒体/文件接口，不再请求所有文件访问和旧式外部存储权限。
- **预测性返回**：Android 13+ 由系统返回框架提供能力；应用内开关控制页面预览动画，系统总开关和应用重启仍会影响最终效果。
- **位置伪装边界**：坐标修改覆盖 Android `Location` 返回值，但不等于阻断 IP 定位、厂商私有接口或远程服务；Wi-Fi/基站隐藏也只过滤目标进程可读取的相关标识。
- **时区伪装边界**：只影响目标进程读取/格式化时间所使用的时区，不修改设备时钟或网络时间。
- **DPI 伪装边界**：只影响目标应用读取的显示参数，不修改系统设置里的显示大小。
- **进程内 Hook 边界**：设备构建字段仍通过目标进程中的 Java API/字段生效，不修改原生系统属性或 `/proc/cpuinfo`。同时对比 Java `Build` 与原生属性、内核信息的应用仍可能发现差异；Guise 不为减少差异而引入需要 ROOT/Zygisk 的全局属性替换。

## 数据来源与预设

- 品牌与型号目录来自 [KHwang9883/MobileModels](https://github.com/KHwang9883/MobileModels) 的[官方 CSV 导出](https://github.com/KHwang9883/MobileModels-csv)，设备数据部分遵循 [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)。来源版本与转换说明见 `app/src/main/assets/devices.NOTICE.txt`。
- 全球运营商预设来自 [pbakondy/mcc-mnc-list](https://github.com/pbakondy/mcc-mnc-list)，采用 MIT License。筛选规则与来源版本见 `app/src/main/assets/carriers.NOTICE.txt`。
- Android 版本、SDK/API、DPI、网络和语言等预设位于 `app/src/main/res/raw/presets.json`。
- 时区列表由系统标准时区 ID 生成，并支持搜索与随机选择。

## 多语言

Guise Reborn 与 Guise Test 当前维护以下完整资源：

- 英文
- 简体中文
- 日文
- 阿拉伯文（含 RTL）

Android 13 及以上可通过系统“应用语言”设置单独切换。新增功能需要同步补齐四套资源，不能只在 Kotlin/Compose 代码中硬编码中文。

## Guise Test

`guise-test` 是用于检测 Guise 伪装效果是否生效的独立“靶应用”。

测试时先安装 Guise Test，在 Guise 中配置明显不同的值并勾选同步作用域，然后停止并重新启动测试应用。权限不足、设备无 SIM 或接口不受支持时会展示具体异常，不会把异常误判为 Hook 成功。

单独构建：

```powershell
.\gradlew.bat :guise-test:assembleDebug :guise-test:lintDebug
```

产物位于 `guise-test/build/outputs/apk/debug/guise-test-debug.apk`，启用 R8 与资源收缩，并使用 Android 默认调试签名。

## 项目模块

- `app`：Compose 管理界面、配置存储、更新机制和完整 Xposed Hook。
- `ktx-xposed`：Modern Xposed API 适配、Hook 辅助与模块日志基础设施。
- `guise-test`：用于检测 Guise 伪装效果是否生效的独立靶应用。

## 构建

需要：

- JDK 17 或兼容当前 Gradle/AGP 的更新 JDK；项目 Java/Kotlin 目标为 17。
- Android SDK Platform 37。
- Android SDK Build-Tools 37.0.0。

在仓库根目录创建不纳入版本控制的 `local.properties`：

```properties
sdk.dir=D\:\\AndroidSDK
```

常用命令：

```powershell
# Debug 构建与检查
.\gradlew.bat assembleDebug lintDebug

# Release/R8 构建
.\gradlew.bat :app:assembleRelease

# Guise Test
.\gradlew.bat :guise-test:assembleDebug :guise-test:lintDebug
```

## 开放源代码许可

应用内可在“设置 → 开放源代码许可”查看项目、数据来源和许可证，并打开项目主页或许可证原文：

- [Guise Reborn](https://github.com/daxiaamu/Guise_Reborn)：[GNU GPL v3.0 or later](LICENSE)。
- [AndroidX / Jetpack Compose / Material 3](https://github.com/androidx/androidx)：[Apache License 2.0](https://source.android.com/docs/setup/about/licenses)。
- [Kotlin / kotlinx.coroutines / kotlinx.serialization](https://github.com/JetBrains/kotlin)：[Apache License 2.0](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt)。
- [libxposed API / service](https://github.com/libxposed)：[Apache License 2.0](https://github.com/libxposed/api/blob/master/LICENSE)。
- [MaterialKolor](https://github.com/jordond/MaterialKolor)：主体为 [MIT License](https://github.com/jordond/MaterialKolor/blob/main/LICENSE)，Material Color Utilities 为 [Apache License 2.0](https://github.com/material-foundation/material-color-utilities/blob/main/LICENSE)。
- [MMKV](https://github.com/Tencent/MMKV)：[BSD 3-Clause License](https://github.com/Tencent/MMKV/blob/master/LICENSE.TXT)。
- [Multiplatform Markdown Renderer](https://github.com/mikepenz/multiplatform-markdown-renderer)：[Apache License 2.0](https://github.com/mikepenz/multiplatform-markdown-renderer/blob/develop/LICENSE)。
- [MobileModels / MobileModels-csv](https://github.com/KHwang9883/MobileModels)：[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)。
- [mcc-mnc-list](https://github.com/pbakondy/mcc-mnc-list)：[MIT License](https://github.com/pbakondy/mcc-mnc-list/blob/master/LICENSE)。

## 开发原则

1. 优先使用 Android 与 Modern Xposed 的正式新 API；旧实现妨碍安全性、可维护性或新系统支持时直接替换。
2. 保留现有用户配置和模板的数据迁移能力，但不为无效或危险的内部实现长期背负兼容层。
3. 启动首帧只做必须同步完成的工作；数据库、文件、PackageManager、图标和网络访问应放到合适的后台调度器或延迟执行。
4. UI 组件、辅助说明和尾部操作使用可复用布局，不为单个字段复制一套尺寸与对齐参数。
5. 性能改动必须通过相同设备、相同构建类型和相同启动方式验证；没有稳定收益的改动应撤回。
6. Hook 功能按职责隔离，单个 Hook 失败不得拖垮目标进程。
7. 新增用户可见文案必须同步维护英文、简体中文、日文和阿拉伯文。
8. 发布 APK 前必须在真实的、已安装支持 Modern Xposed API 102 框架的 Android 设备上回归。

## 许可证

Guise Reborn 的应用代码采用 [GNU General Public License v3.0 or later](LICENSE) 发布。Copyright © 2026 大侠阿木及 Guise Reborn 贡献者。您可以在遵守该许可证、保留版权与许可证声明并公开相应源代码的前提下使用、修改和分发。

原始 Guise 的作者署名及 Git 历史继续保留。独立数据资产维持各自的上游许可证：设备目录为 `CC BY-NC-SA 4.0`，运营商预设为 `MIT License`；详见应用内“开放源代码许可”和对应的 `NOTICE` 文件。

---

## 【以下为原作者说明】

原项目：[Houvven/Guise](https://github.com/Houvven/Guise)

本说明所在的源码仓库：[AlliotTech/Guise](https://github.com/AlliotTech/Guise)

这个软件从第一个版本发布到现在应该有一年多了，具体哪天发布的我已经忘记了，也懒得去看。

只记得在某天晚上下载了王者荣耀发现其并没有开发我当时使用的设备的120帧开关，于是我想要去改机型，当时应用变量和应用伪装最后一个免费版本在我的设备上已经无法使用。我去了解了源计划，但是那个时候我还是大学生，50块钱的永久会员对于那时候的我有点肉疼，于是我便趁着寒假自己开发了一个类似的软件，后面开学后就一直没有更新。至于为什么最近更新了，有点难评…… 我马上毕业了，我在学校主修的方向是Java后端开发，但是无奈一直找不到工作。其实这次更新是想加入一些付费内容，可以看到最新的alpha版本移除了内置的机型库，我其实是想从这方面收费的，基础功能免费且继续开源。这几天也想了很多，这个APP诞生的初衷就是为了免费，我并不想违背我的初衷，而且这个APP并没有什么技术含量，属于非常简单的，任何一个计算机专业的大一学生可以做出来一个类似的。

说了这么多也不知道说了说什么，还是说说重点吧，这个APP我已经决定停更,感兴趣的可以接手继续开发。感谢这段时间大家的支持。
