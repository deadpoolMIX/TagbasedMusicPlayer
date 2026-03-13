# 标签化音乐播放器 - 实现计划

## 项目概述
- **平台**: Android (Android Studio + Claude Code插件)
- **目标**: 原生Android APK应用
- **核心特色**: 基于标签的音乐管理与布尔运算筛选
- **最低Android版本**: Android 8.0 (API 26)
- **目标Android版本**: Android 14 (API 34)

---

## 技术栈选择

### 核心框架与库
| 组件 | 技术选择 | 说明 |
|------|----------|------|
| **编程语言** | Kotlin | 官方推荐，协程支持 |
| **UI框架** | Jetpack Compose | 现代声明式UI，完美实现Stitch设计 |
| **音频播放** | ExoPlayer | Google官方，支持广泛格式 |
| **媒体会话** | Media3 Session | 支持通知栏/锁屏控制 |
| **数据库** | Room | 本地数据持久化 |
| **图片加载** | Coil | Compose原生支持 |
| **依赖注入** | Hilt | 简化依赖管理 |
| **异步处理** | Kotlin Coroutines + Flow | 响应式数据流 |
| **数据存储** | DataStore | 替代SharedPreferences |
| **导航** | Jetpack Navigation | 页面导航管理 |

### 音频格式支持
ExoPlayer原生支持：MP3, AAC, FLAC, WAV, OGG, Opus, M4A

### 补充：音频播放器内核模块
以下是你未提及但必需的音频播放器内核组件：

1. **音频焦点管理 (Audio Focus)**
   - 处理来电、导航语音等中断场景
   - 自动暂停/恢复播放

2. **媒体会话管理 (MediaSession)**
   - 支持耳机线控（播放/暂停/上一首/下一首）
   - 支持蓝牙设备控制
   - 支持Android Auto/Wear OS

3. **通知栏播放器 (MediaNotification)**
   - 常驻通知栏播放控制
   - 锁屏界面显示
   - 专辑封面大图显示

4. **播放队列管理 (Playback Queue)**
   - 播放列表内存管理
   - 播放模式：顺序/单曲循环/随机

5. **歌词解析器 (Lyrics Parser)**
   - 支持LRC格式歌词
   - 时间戳同步解析
   - 逐字/逐行高亮

6. **元数据提取 (Metadata Extractor)**
   - 读取歌曲ID3标签
   - 提取专辑封面
   - 读取歌词内嵌标签

---

## 数据库设计

### 实体关系
```
Song (歌曲) ←→ SongTag (多对多) ←→ Tag (标签)
  ↓
PlaylistSong (歌单歌曲关联) → Playlist (歌单)
```

### 核心表结构

#### Song (歌曲表)
```kotlin
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,           // MediaStore ID
    val title: String,                   // 歌曲名
    val artist: String,                  // 歌手
    val album: String,                   // 专辑
    val albumId: Long,                   // 专辑ID
    val duration: Long,                  // 时长(毫秒)
    val filePath: String,                // 文件路径
    val fileName: String,                // 文件名
    val dateAdded: Long,                 // 添加时间
    val dateModified: Long,              // 修改时间
    val size: Long,                      // 文件大小
    val lyrics: String?,                 // 内嵌歌词
    val playCount: Int = 0,              // 播放次数
    val lastPlayed: Long? = null,        // 最后播放时间
)
```

#### Tag (标签表)
```kotlin
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // 标签名
    val color: Int?,                     // 标签颜色(可选)
    val createdAt: Long,                 // 创建时间
    val updatedAt: Long,                 // 更新时间
    val sortOrder: Int = 0,              // 排序顺序
)
```

#### SongTag (歌曲标签关联表)
```kotlin
@Entity(
    tableName = "song_tags",
    primaryKeys = ["songId", "tagId"]
)
data class SongTag(
    val songId: Long,
    val tagId: Long,
    val addedAt: Long,                   // 关联时间
)
```

#### Playlist (歌单表)
```kotlin
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // 歌单名
    val coverPath: String?,              // 自定义封面路径
    val createdAt: Long,                 // 创建时间
    val updatedAt: Long,                 // 更新时间
    val sortOrder: Int = 0,              // 排序顺序(用于手动排序)
)
```

#### PlaylistSong (歌单歌曲关联表)
```kotlin
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index(value = ["playlistId", "sortOrder"])]
)
data class PlaylistSong(
    val playlistId: Long,
    val songId: Long,
    val sortOrder: Int,                  // 在歌单中的顺序
    val addedAt: Long,                   // 添加时间
)
```

#### ScanFolder (扫描文件夹表)
```kotlin
@Entity(tableName = "scan_folders")
data class ScanFolder(
    @PrimaryKey val path: String,        // 文件夹路径
    val isIncluded: Boolean = true,      // 是否包含子文件夹
    val addedAt: Long,                   // 添加时间
)
```

---

## 实现阶段规划

### 阶段一：项目基础搭建 (预计2-3天)
**目标**: 建立项目骨架，配置依赖，搭建基础架构

#### 1.1 项目创建
- [ ] 创建Android Studio项目 (Empty Activity模板)
- [ ] 配置build.gradle.kts (依赖库)
- [ ] 配置minSdk=26, targetSdk=34
- [ ] 启用Compose

#### 1.2 依赖注入配置
- [ ] 配置Hilt Application类
- [ ] 创建基础Module (DatabaseModule, PlayerModule)

#### 1.3 基础架构搭建
- [ ] 配置Navigation组件
- [ ] 创建MainActivity + 底部导航栏框架
- [ ] 创建4个主页面占位符 (首页/歌单/标签/筛选)
- [ ] 创建主题系统 (Light/Dark模式，仿Stitch设计配色)

#### 1.4 权限配置
- [ ] 申请读取音频文件权限 (READ_MEDIA_AUDIO)
- [ ] 申请通知权限 (POST_NOTIFICATIONS)
- [ ] 申请存储权限 (Android 10以下兼容)

---

### 阶段二：数据层实现 (预计3-4天)
**目标**: 数据库设计与实现，音乐扫描服务

#### 2.1 Room数据库搭建
- [ ] 创建Song实体和DAO
- [ ] 创建Tag实体和DAO
- [ ] 创建SongTag关联表和DAO
- [ ] 创建Playlist实体和DAO
- [ ] 创建PlaylistSong关联表和DAO
- [ ] 创建ScanFolder实体和DAO
- [ ] 配置Database类和Migrations

#### 2.2 音乐扫描服务
- [ ] 创建MusicScanner类 (MediaStore查询)
- [ ] 实现文件夹扫描逻辑
- [ ] 实现增量扫描 (新歌曲检测)
- [ ] 实现歌曲删除检测
- [ ] 后台扫描Worker (WorkManager)

#### 2.3 Repository层
- [ ] SongRepository (歌曲CRUD)
- [ ] TagRepository (标签CRUD + 歌曲关联)
- [ ] PlaylistRepository (歌单CRUD + 歌曲排序)
- [ ] FilterRepository (布尔运算筛选逻辑)
- [ ] SettingsRepository (设置存储)

---

### 阶段三：音频播放核心 (预计4-5天)
**目标**: 实现完整的音频播放功能

#### 3.1 ExoPlayer集成
- [ ] 创建ExoPlayer封装类
- [ ] 实现播放/暂停/停止
- [ ] 实现上一首/下一首
- [ ] 实现进度跳转 (Seek)
- [ ] 实现播放模式切换 (顺序/单曲循环/随机)

#### 3.2 播放队列管理
- [ ] 创建PlaybackQueue管理器
- [ ] 实现队列添加/删除/排序
- [ ] 实现随机播放算法
- [ ] 持久化当前队列

#### 3.3 MediaSession集成
- [ ] 配置Media3 Session
- [ ] 支持耳机线控
- [ ] 支持蓝牙设备控制
- [ ] 音频焦点处理 (来电暂停)

#### 3.4 通知栏播放器
- [ ] 创建MediaNotificationManager
- [ ] 设计通知栏布局 (仿需求说明)
- [ ] 锁屏媒体控制
- [ ] 进度条实时更新

#### 3.5 播放状态管理
- [ ] 创建PlaybackState数据类
- [ ] 使用StateFlow管理播放状态
- [ ] 播放进度实时更新 (每1000ms)

---

### 阶段四：UI基础组件 (预计2-3天)
**目标**: 创建可复用的UI组件

#### 4.1 主题系统
- [ ] 定义颜色系统 (Primary: #1978e5, Background Light: #f6f7f8, Dark: #111821)
- [ ] 定义字体系统 (使用系统默认中文)
- [ ] 定义圆角系统 (卡片8dp, 按钮9999dp)
- [ ] 定义暗色模式切换逻辑

#### 4.2 通用组件
- [ ] MiniPlayer组件 (底部悬浮播放器)
- [ ] BottomNavigationBar组件
- [ ] SongItem组件 (歌曲列表项)
- [ ] AlbumArt组件 (专辑封面加载)
- [ ] MoreMenu组件 (⋮ 底部操作面板)
- [ ] TagChip组件 (标签胶囊)
- [ ] SearchBar组件 (搜索框)
- [ ] EmptyState组件 (空白状态)

#### 4.3 图标系统
- [ ] 导入Material Symbols Outlined图标
- [ ] 自定义需要的图标资源

---

### 阶段五：首页功能实现 (预计3-4天)
**目标**: 完成首页(全局曲库)所有功能

#### 5.1 首页UI
- [ ] 顶部统计栏 (歌曲总数)
- [ ] 搜索框实现
- [ ] 分类筛选Chip (最近添加/歌手/专辑/风格)
- [ ] 歌曲列表展示 (双行结构)
- [ ] 下拉刷新

#### 5.2 歌曲操作面板
- [ ] 底部弹窗实现
- [ ] 下一首播放功能
- [ ] 收藏到歌单功能
- [ ] 添加/编辑标签入口
- [ ] 查看歌手/专辑功能
- [ ] 删除功能 (红色警示)

#### 5.3 搜索功能
- [ ] 本地搜索 (歌名/歌手/专辑)
- [ ] 搜索结果实时显示
- [ ] 搜索历史记录

---

### 阶段六：歌单功能实现 (预计2-3天)
**目标**: 完成歌单页所有功能

#### 6.1 歌单列表UI
- [ ] 顶部统计栏 (歌单总数)
- [ ] 歌单项展示 (封面+名称+歌曲数)
- [ ] 拖拽排序热区 (六点图标)
- [ ] 歌单更多操作 (删除)

#### 6.2 歌单详情页
- [ ] 创建歌单详情页面
- [ ] 歌单内歌曲列表
- [ ] 歌曲排序调整
- [ ] 从歌单移除歌曲

#### 6.3 歌单管理
- [ ] 新建歌单弹窗
- [ ] 重命名歌单
- [ ] 删除歌单确认
- [ ] 拖拽排序实现 (ItemTouchHelper)

---

### 阶段七：标签功能实现 (预计3-4天)
**目标**: 完成标签页和标签管理功能

#### 7.1 标签页UI
- [ ] 顶部搜索框
- [ ] 标签列表展示 (#图标+名称+数量)
- [ ] 标签项点击进入详情

#### 7.2 标签详情页
- [ ] 显示标签名和歌曲数量
- [ ] 歌曲列表展示
- [ ] 支持歌曲操作

#### 7.3 标签管理弹窗
- [ ] 居中弹窗实现
- [ ] 当前标签展示 (带×删除)
- [ ] 搜索框 ("搜索或创建标签...")
- [ ] 创建新标签选项 (+ 创建标签)
- [ ] 模糊搜索匹配标签
- [ ] 常用标签推荐

#### 7.4 标签数据逻辑
- [ ] 标签CRUD操作
- [ ] 歌曲标签关联/解绑
- [ ] 标签重命名
- [ ] 标签颜色管理 (可选)

---

### 阶段八：筛选功能实现 (预计3-4天)
**目标**: 完成布尔运算筛选器

#### 8.1 筛选页UI
- [ ] 逻辑公式展示 ((A ∪ B) - C)
- [ ] 框A: 包含标签输入区
- [ ] 并集符号 (+)
- [ ] 框B: 可选标签输入区
- [ ] 差集符号 (-)
- [ ] 框C: 排除标签输入区

#### 8.2 布尔运算逻辑
- [ ] 交集计算 (A ∩ B...)
- [ ] 并集计算 (A ∪ B)
- [ ] 差集计算 (Result - C)
- [ ] 空白态处理 (三框为空=全库)
- [ ] 实时筛选结果计算

#### 8.3 筛选结果展示
- [ ] 结果数量统计
- [ ] 播放全部按钮
- [ ] 歌曲列表展示
- [ ] 保存为歌单功能

#### 8.4 标签选择器
- [ ] 标签输入自动完成
- [ ] 已选标签展示 (可删除)
- [ ] 标签下拉建议列表

---

### 阶段九：正在播放页实现 (预计4-5天)
**目标**: 完成完整播放页和歌词功能

#### 9.1 播放页UI
- [ ] 顶部标题栏 (正在播放 + 来源)
- [ ] 大面积专辑封面展示
- [ ] 歌曲名/歌手名显示
- [ ] 标签快捷位 (胶囊+添加按钮)

#### 9.2 播放控制
- [ ] 进度条 (可拖拽)
- [ ] 时间显示 (当前/总时长)
- [ ] 循环模式按钮 (单曲/列表/随机)
- [ ] 上一首/播放暂停/下一首
- [ ] 播放队列按钮

#### 9.3 播放队列弹窗
- [ ] 当前播放队列列表
- [ ] 拖拽排序
- [ ] 删除歌曲
- [ ] 清空队列

#### 9.4 歌词功能
- [ ] 歌词解析器 (LRC格式)
- [ ] 全屏歌词页面
- [ ] 逐行高亮显示
- [ ] 点击歌词跳转
- [ ] 歌词滚动同步

#### 9.5 手势操作
- [ ] 点击封面进入歌词页
- [ ] 左右滑动切歌 (可选)
- [ ] 下拉关闭播放页

---

### 阶段十：设置功能实现 (预计2-3天)
**目标**: 完成设置页面和数据管理

#### 10.1 设置页UI
- [ ] 设置列表布局
- [ ] 分组标题

#### 10.2 主题设置
- [ ] 浅色/深色模式切换
- [ ] 跟随系统开关
- [ ] 主题色选择 (可选)

#### 10.3 数据备份与导入
- [ ] 导出备份 (JSON格式)
   - 标签数据
   - 歌曲标签关联
   - 歌单数据
- [ ] 导入备份
- [ ] 导入冲突处理

#### 10.4 文件夹管理
- [ ] 已添加文件夹列表
- [ ] 添加文件夹 (SAF文件选择器)
- [ ] 删除文件夹
- [ ] 重新扫描触发

#### 10.5 其他设置
- [ ] 清除缓存
- [ ] 关于页面
- [ ] 版本信息

---

### 阶段十一：优化与测试 (预计3-4天)
**目标**: 性能优化和bug修复

#### 11.1 性能优化
- [ ] 图片加载优化 (Coil缓存)
- [ ] 列表懒加载优化
- [ ] 数据库查询优化 (索引)
- [ ] 内存泄漏检查

#### 11.2 异常处理
- [ ] 音频文件损坏处理
- [ ] 权限拒绝处理
- [ ] 存储空间不足处理

#### 11.3 兼容性测试
- [ ] Android 8-14兼容性
- [ ] 不同屏幕尺寸适配
- [ ] 横竖屏适配

#### 11.4 边缘场景
- [ ] 空曲库状态
- [ ] 无标签状态
- [ ] 大量歌曲测试 (10000+首)

---

## UI设计规范 (基于Stitch设计)

### 颜色系统
```kotlin
// Light Theme
val Primary = Color(0xFF1978E5)
val BackgroundLight = Color(0xFFF6F7F8)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1A1A1A)
val SecondaryTextLight = Color(0xFF6B7280)

// Dark Theme
val BackgroundDark = Color(0xFF111821)
val SurfaceDark = Color(0xFF1E293B)
val OnSurfaceDark = Color(0xFFF1F5F9)
val SecondaryTextDark = Color(0xFF94A3B8)
```

### 字体系统
- 主标题: 20sp, Bold
- 歌曲名: 16sp, Medium
- 歌手名: 14sp, Regular
- 标签文字: 12sp, Medium
- 统计数字: 24sp, Bold

### 间距系统
- 页面边距: 16dp
- 列表项内边距: 12dp
- 组件间距: 8dp
- 卡片圆角: 12dp
- 按钮圆角: 9999dp (全圆)
- 标签胶囊圆角: 9999dp

### 图标系统
- 使用Material Symbols Outlined
- 底部导航: 24dp
- 操作按钮: 20-24dp
- 歌曲列表更多: 24dp

---

## 项目目录结构

```
app/src/main/java/com/yourname/musicplayer/
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── MusicDatabase.kt
│   │   │   ├── SongDao.kt
│   │   │   ├── TagDao.kt
│   │   │   └── ...
│   │   ├── entity/
│   │   │   ├── Song.kt
│   │   │   ├── Tag.kt
│   │   │   └── ...
│   │   └── preferences/
│   │       └── SettingsDataStore.kt
│   ├── repository/
│   │   ├── SongRepository.kt
│   │   ├── TagRepository.kt
│   │   └── ...
│   └── scanner/
│       └── MusicScanner.kt
├── domain/
│   ├── model/
│   │   ├── PlaybackState.kt
│   │   └── FilterCriteria.kt
│   └── usecase/
│       ├── FilterSongsUseCase.kt
│       └── ...
├── player/
│   ├── MusicPlayer.kt
│   ├── PlaybackQueue.kt
│   ├── MediaSessionCallback.kt
│   └── NotificationManager.kt
├── ui/
│   ├── components/
│   │   ├── MiniPlayer.kt
│   │   ├── SongItem.kt
│   │   ├── TagChip.kt
│   │   └── ...
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── playlist/
│   ├── tags/
│   ├── filter/
│   ├── player/
│   ├── settings/
│   └── navigation/
│       └── NavGraph.kt
├── di/
│   └── AppModule.kt
└── MainActivity.kt
```

---

## 里程碑与交付物

### Milestone 1: 基础架构完成
- 可运行的空壳应用
- 底部导航栏切换
- 主题切换正常

### Milestone 2: 数据层完成
- 音乐扫描正常工作
- 数据库读写正常
- 首页显示歌曲列表

### Milestone 3: 播放功能完成
- 可正常播放音乐
- 通知栏控制正常
- MiniPlayer正常显示

### Milestone 4: 核心功能完成
- 标签CRUD完成
- 歌单功能完成
- 筛选功能完成

### Milestone 5: 完整产品
- 所有功能可用
- 通过基础测试
- 可打包APK

---

## 开发注意事项

### 1. 中文环境适配
- 确保所有UI文字使用中文
- 搜索支持中文拼音 (可选增强)
- 文件名编码处理 (防止乱码)

### 2. 存储权限适配
- Android 10以下: 需要WRITE_EXTERNAL_STORAGE
- Android 10-12: 分区存储适配
- Android 13+: 使用READ_MEDIA_AUDIO

### 3. 后台播放保活
- 使用前台服务 (Foreground Service)
- 电池优化白名单引导
- 省电模式适配

### 4. 性能考虑
- 歌曲列表使用LazyColumn
- 图片异步加载
- 数据库查询使用索引
- 大量歌曲时使用分页加载

---

## 需求确认记录

### 已确认问题 (2026-03-13)

| 问题 | 确认结果 |
|------|----------|
| 1. 是否需要APE/DSD等特殊格式? | **不需要** (仅使用ExoPlayer原生支持的格式即可) |
| 2. 歌词来源? | **仅用内嵌歌词** (软件纯本地运行，无网络功能) |
| 3. 备份数据格式? | **JSON格式** 可以 |
| 4. 播放页滑动切歌? | **不需要** |

---

## 开发规则 (重要)

### 规则1: 开发过程记录
每次完成一个功能都要记录到专门的开发日志文件中。
**日志文件**: `D:\Projects\TagbasedMusicPlayer\DEVELOPMENT_LOG.md`

### 规则2: 需求确认流程
每当我提出新需求时：
1. 先优化需求
2. 制定可执行计划
3. 确认需求细节
4. **经我确认后**再开始实现

### 规则3: 资源获取方式
如果有些事做不到（如无法联网搜索/获取某个东西）：
- 先不要想着用别的方法代替
- 如果网上搜索该信息是最优解，**停止输出并告诉我需要什么**
- 由我来帮你获取所需资源

### 规则4: 编译检查
每次完成一项功能后：
1. 你先编译一次
2. 确认没有bug后再让我运行确认

### 规则5: 里程碑测试
每完成一个Milestone就：
1. 让我运行测试一次
2. 看看效果
3. 确认后再开始下一个Milestone

### 规则6: 音频内核模块
音频的内核模块之类的：
- 如果能使用现有的开源项目，直接使用
- 如果无法使用，**告诉我该怎么做才能帮你添加**

---

## 音频解码器说明

### 当前方案: ExoPlayer (Google官方)
ExoPlayer是Google开源的Android媒体播放器，支持：
- MP3, AAC, FLAC, WAV, OGG, Opus, M4A等常见格式
- 已内置音频焦点管理、MediaSession支持
- 通过Media3库可直接实现通知栏播放控制

**不需要额外获取解码器**，ExoPlayer已足够满足需求。

### 如需增强 (后续可选)
如果需要支持更多格式，可考虑：
- **FFmpeg扩展**: ExoPlayer官方提供FFmpeg音频解码扩展
- 需要自行编译FFmpeg so库
- **如果未来需要，请告知，我会指导你如何编译**

---

*计划已确认，开始阶段一实现。*
