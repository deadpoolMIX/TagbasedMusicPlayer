# 开发日志

## 项目: 标签化音乐播放器

---

## 阶段一: 项目基础搭建

### 任务清单
- [ ] 创建Android Studio项目
- [ ] 配置build.gradle.kts依赖
- [ ] 配置Hilt依赖注入
- [ ] 配置Navigation导航
- [ ] 创建MainActivity + 底部导航栏
- [ ] 创建4个主页面占位符
- [ ] 创建主题系统 (Light/Dark)
- [ ] 配置权限 (音频读取、通知)

### 进度记录
- [x] 创建项目基础结构
- [x] 配置Gradle和依赖库 (gradle/libs.versions.toml)
- [x] 配置build.gradle.kts (项目级和应用级)
- [x] 配置Hilt依赖注入 (AppModule.kt, MusicPlayerApplication.kt)
- [x] 配置Navigation导航 (Screen.kt, BottomNavBar.kt, NavGraph.kt)
- [x] 创建MainActivity.kt (集成Compose + Hilt)
- [x] 创建4个主页面占位符 (Home/Playlist/Tags/Filter)
- [x] 创建主题系统 (Color.kt, Type.kt, Theme.kt)
- [x] 配置权限 (AndroidManifest.xml)
- [x] 创建基础资源文件 (strings.xml, themes.xml, icons)
- [x] 创建数据库和实体占位符 (MusicDatabase.kt, Song.kt)
- [x] 创建播放器服务占位符 (PlaybackService.kt)

**阶段一完成，等待编译测试。**

### 阶段一测试清单
**请在Android Studio中执行以下测试：**
1. 点击 **Build → Make Project** (Ctrl+F9) 编译项目
2. 确认编译成功，无错误
3. 运行应用，验证底部导航栏可点击切换 (首页/歌单/标签/筛选)
4. 验证主题切换正常（可尝试切换系统深色模式）

---

## 阶段二: 数据层实现

### 任务清单
- [x] Room数据库完整搭建 (所有实体和DAO)
- [x] 音乐扫描服务 (MediaStore查询)
- [x] Repository层实现

### 进度记录
- [x] Room数据库完整搭建 (MusicDatabase.kt)
  - 6个实体: Song, Tag, SongTag, Playlist, PlaylistSong, ScanFolder
  - 4个DAO: SongDao, TagDao, PlaylistDao, ScanFolderDao
- [x] AppModule更新 (提供DAO依赖注入)
- [x] 音乐扫描服务 (MusicScanner.kt)
  - MediaStore查询
  - 支持按路径扫描
  - 支持按ID扫描单曲
- [x] Repository层实现
  - SongRepository (歌曲CRUD + 扫描)
  - TagRepository (标签CRUD + 歌曲关联)
  - PlaylistRepository (歌单CRUD + 歌曲排序)
  - ScanFolderRepository (扫描文件夹管理)
  - FilterRepository (布尔运算筛选逻辑)

### 数据库实体关系
```
Song (歌曲) ←→ SongTag (多对多) ←→ Tag (标签)
  ↓
PlaylistSong → Playlist (歌单)
ScanFolder (扫描文件夹)
```

### 布尔运算公式
- 筛选逻辑: `(A ∩ A2 ∩ ...) ∪ (B ∩ B2 ∩ ...) - C`
- 框A: 交集 (歌曲必须包含框内所有标签)
- 框B: 交集 (歌曲必须包含框内所有标签)
- 并集: A ∪ B
- 框C: 差集 (从结果中排除包含任意C标签的歌曲)

**阶段二完成，等待编译测试。**

### 阶段二完成内容总结

| 模块 | 文件 | 说明 |
|------|------|------|
| **实体** | `Song.kt` | 歌曲信息 (13个字段) |
| | `Tag.kt` | 标签信息 (6个字段) |
| | `SongTag.kt` | 歌曲-标签关联表 |
| | `Playlist.kt` | 歌单信息 (6个字段) |
| | `PlaylistSong.kt` | 歌单-歌曲关联表 (支持排序) |
| | `ScanFolder.kt` | 扫描文件夹配置 |
| **DAO** | `SongDao.kt` | 歌曲CRUD + 搜索 + 播放统计 |
| | `TagDao.kt` | 标签CRUD + 歌曲关联查询 |
| | `PlaylistDao.kt` | 歌单CRUD + 歌曲排序管理 |
| | `ScanFolderDao.kt` | 扫描文件夹管理 |
| **扫描** | `MusicScanner.kt` | MediaStore查询服务 |
| **Repository** | `SongRepository.kt` | 歌曲数据管理 |
| | `TagRepository.kt` | 标签数据管理 |
| | `PlaylistRepository.kt` | 歌单数据管理 |
| | `ScanFolderRepository.kt` | 扫描文件夹管理 |
| | `FilterRepository.kt` | 布尔运算筛选逻辑 |

### 阶段二测试清单
**请在Android Studio中执行以下测试：**
1. 点击 **Build → Make Project** (Ctrl+F9) 编译项目
2. 确认无编译错误
3. 验证 Room 数据库编译生成的代码存在 (检查 `app/build/generated`)
4. 确认所有 Repository 类无依赖注入错误

---

## 阶段三: 音频播放核心

### 任务清单
- [x] ExoPlayer集成 (播放/暂停/切歌)
- [x] 播放队列管理
- [x] MediaSession集成 (耳机线控/蓝牙)
- [x] 通知栏播放器
- [x] 播放状态管理

### 进度记录
- [x] ExoPlayer集成 (MusicPlayer.kt)
  - 播放/暂停/停止
  - 上一首/下一首
  - 进度跳转 (Seek)
  - 播放模式切换 (顺序/单曲循环/随机)
- [x] 播放队列管理 (PlaybackQueue.kt)
  - 播放列表内存管理
  - 支持随机播放算法
  - 支持拖拽排序
- [x] 播放状态管理 (PlaybackState.kt)
  - 播放状态数据类
  - 播放进度实时更新
  - PlayerEvent事件通道
- [x] MediaSession集成 (PlaybackService.kt)
  - 支持耳机线控
  - 支持蓝牙设备控制
  - 音频焦点处理
  - 前台服务保活
- [x] PlayerViewModel创建
  - 播放控制逻辑封装
  - 状态流管理

### 阶段三完成内容总结

| 模块 | 文件 | 说明 |
|------|------|------|
| **播放器** | `MusicPlayer.kt` | ExoPlayer封装，播放控制 |
| **播放队列** | `PlaybackQueue.kt` | 队列管理，随机播放，排序 |
| **状态管理** | `PlaybackState.kt` | 播放状态，事件定义 |
| **媒体服务** | `PlaybackService.kt` | MediaSession，通知栏，前台服务 |
| **ViewModel** | `PlayerViewModel.kt` | 播放控制ViewModel |

### 播放功能清单

| 功能 | 状态 |
|------|------|
| 播放/暂停 | ✅ |
| 上一首/下一首 | ✅ |
| 进度跳转 | ✅ |
| 播放队列管理 | ✅ |
| 单曲循环 | ✅ |
| 列表循环 | ✅ |
| 随机播放 | ✅ |
| MediaSession | ✅ |
| 前台服务 | ✅ |

**阶段三完成，等待编译测试。**

### 阶段三测试清单
**请在Android Studio中执行以下测试：**
1. 点击 **Build → Make Project** (Ctrl+F9) 编译项目
2. 确认无编译错误
3. 验证 Media3 依赖正确引入
4. 确认 PlaybackService 配置正确

---

## 阶段四: UI基础组件

### 任务清单
- [ ] MiniPlayer组件 (底部悬浮播放器)
- [ ] SongItem组件 (歌曲列表项)
- [ ] TagChip组件 (标签胶囊)
- [ ] 搜索框组件
- [ ] 底部操作面板

### 进度记录
- [x] MiniPlayer组件 (底部悬浮播放器)
  - 显示当前播放歌曲信息
  - 播放/暂停/上一首/下一首控制
  - 进度条显示
  - 点击展开完整播放页
- [x] SongItem组件 (歌曲列表项)
  - 专辑封面占位
  - 歌名/歌手显示
  - 更多操作按钮
- [x] TagChip组件 (标签胶囊)
  - 普通标签显示
  - 可删除标签 (带X按钮)
  - 添加标签按钮
- [x] SearchBar组件 (搜索框)
  - 圆角搜索输入框
  - 搜索图标
  - 占位文字
- [x] SongActionSheet组件 (底部操作面板)
  - 下一首播放
  - 收藏到歌单
  - 添加/编辑标签
  - 查看歌手/专辑
  - 删除 (红色警示)
- [x] MainActivity更新
  - 集成MiniPlayer到底部导航栏上方

### 阶段四完成内容总结

| 组件 | 文件 | 说明 |
|------|------|------|
| **MiniPlayer** | `MiniPlayer.kt` | 底部悬浮播放器，支持控制 |
| **SongItem** | `SongItem.kt` | 歌曲列表项 |
| **TagChip** | `TagChip.kt` | 标签胶囊，支持删除 |
| **SearchBar** | `SearchBar.kt` | 搜索输入框 |
| **ActionSheet** | `SongActionSheet.kt` | 歌曲操作底部面板 |

**阶段四完成，等待编译测试。**

### 阶段四测试清单
**请在Android Studio中执行以下测试：**
1. 点击 **Build → Make Project** (Ctrl+F9) 编译项目
2. 确认无编译错误
3. 验证UI组件正确显示
4. 测试MiniPlayer与PlayerViewModel的集成

---

## 阶段五: 首页功能实现

### 任务清单
- [x] HomeViewModel创建
- [x] 歌曲列表展示
- [x] 搜索功能
- [x] 分类筛选 (全部/最近/艺术家/专辑)
- [x] 歌曲数量统计
- [x] 操作菜单集成
- [x] 空状态处理

### 进度记录
- [x] HomeViewModel创建 (`HomeViewModel.kt`)
  - 搜索状态管理
  - 筛选类型状态 (ALL/RECENT/ARTIST/ALBUM)
  - 歌曲列表数据流 (支持搜索过滤)
  - 扫描状态管理
  - 操作菜单状态管理
- [x] HomeScreen完整实现
  - 顶部标题栏 + 扫描按钮
  - 搜索栏组件集成
  - 筛选器芯片 (FilterChips)
  - 歌曲列表 (LazyColumn)
  - 歌曲数量统计
  - 空状态提示 (EmptyState)
  - 扫描按钮 (无歌曲时)
- [x] 播放集成
  - 点击歌曲播放
  - 操作菜单: 下一首播放
  - 操作菜单: 删除歌曲
- [x] 修复权限和添加文件夹管理
  - 修复扫描无反应问题 - 添加运行时权限申请
    - 创建 PermissionUtils.kt 工具类
    - 适配 Android 13+ READ_MEDIA_AUDIO 权限
    - HomeScreen 添加权限申请对话框
  - 添加手动文件夹扫描功能
    - HomeViewModel 添加文件夹管理方法
    - SongRepository 添加 scanFolder 方法
    - HomeScreen 添加文件夹管理对话框
    - 支持使用系统文件夹选择器 (OpenDocumentTree)
    - 支持添加/移除扫描文件夹

### 阶段五完成内容总结

| 模块 | 文件 | 说明 |
|------|------|------|
| **ViewModel** | `HomeViewModel.kt` | 首页状态管理，搜索筛选，文件夹管理 |
| **Screen** | `HomeScreen.kt` | 歌曲列表页面，操作菜单，权限对话框，文件夹管理 |
| **工具** | `PermissionUtils.kt` | 权限检查和申请工具 |
| **Repository** | `SongRepository.kt` | 添加文件夹扫描方法 |

### HomeScreen功能清单

| 功能 | 状态 |
|------|------|
| 歌曲列表展示 | ✅ |
| 搜索歌曲 | ✅ |
| 分类筛选 | ✅ |
| 歌曲统计 | ✅ |
| 操作菜单 (更多) | ✅ |
| 点击播放 | ✅ |
| 扫描按钮 | ✅ |
| 空状态提示 | ✅ |
| 权限申请 | ✅ |
| 文件夹管理 | ✅ |
| 手动添加文件夹 | ✅ |

**阶段五完成，等待编译测试。**

### 阶段五测试清单
**请在Android Studio中执行以下测试：**
1. 点击 **Build → Make Project** (Ctrl+F9) 编译项目
2. 确认无编译错误
3. 运行应用，测试首页功能
4. 点击扫描按钮，授权后扫描本地音乐
5. 验证歌曲列表正确显示
6. 测试搜索功能
7. 测试点击歌曲播放 (MiniPlayer应显示)
8. 测试点击"更多"按钮，显示操作菜单

---

## 阶段六: 歌单功能实现

### 任务清单
- [x] PlaylistViewModel创建
- [x] PlaylistScreen歌单列表页面
- [x] PlaylistDetailScreen歌单详情页
- [x] AddToPlaylistDialog添加到歌单对话框

### 进度记录
- [x] 创建 PlaylistViewModel
  - 歌单列表管理
  - 创建/删除歌单
  - 添加/移除歌曲
- [x] 实现 PlaylistScreen 歌单列表页面
  - 显示所有歌单
  - 创建歌单对话框
  - 删除歌单功能
  - 空状态提示
- [x] 实现 PlaylistDetailScreen 歌单详情页
  - 显示歌单信息
  - 歌曲列表
  - 播放全部功能
- [x] 实现 AddToPlaylistDialog 添加到歌单对话框
  - 在首页操作菜单中集成
  - 支持创建新歌单
  - 选择已有歌单
- [x] 更新 Playlist 实体添加 songCount 字段
- [x] 集成到 HomeScreen
  - "更多"菜单中添加"收藏到歌单"

### 创建的文件
| 文件 | 说明 |
|------|------|
| PlaylistViewModel.kt | 歌单状态管理 |
| PlaylistScreen.kt | 歌单列表页面 |
| PlaylistDetailScreen.kt | 歌单详情页面 |
| AddToPlaylistDialog.kt | 添加到歌单对话框 |

**阶段六完成，等待编译测试。**

---

## 阶段七: 标签功能实现

### 任务清单
- [x] TagViewModel创建
- [x] TagsScreen标签列表页面
- [x] TagDetailScreen标签详情页
- [x] TagSelectionDialog标签选择对话框

### 进度记录
- [x] 创建 TagViewModel
  - 标签列表管理
  - 创建/编辑/删除标签
  - 标签搜索功能
  - 歌曲标签关联/解绑
  - 使用 flatMapLatest 实时获取标签下的歌曲
- [x] 实现 TagsScreen 标签列表页面
  - 显示所有标签
  - 搜索标签
  - 创建/编辑/删除标签对话框
  - 点击标签进入详情页
  - 空状态提示
- [x] 实现 TagDetailScreen 标签详情页
  - 显示标签信息
  - 歌曲列表
  - 播放全部功能
  - 从标签移除歌曲
- [x] 实现 TagSelectionDialog 标签选择对话框
  - 显示歌曲当前已添加的标签（可移除）
  - 搜索可用标签
  - 创建新标签并自动添加
  - 点击标签添加到歌曲
- [x] 集成到 HomeScreen 和 PlaylistDetailScreen
  - "更多"菜单中添加"添加/编辑标签"
- [x] 更新 NavGraph 添加 TAG_DETAIL 路由

### 创建的文件
| 文件 | 说明 |
|------|------|
| TagViewModel.kt | 标签状态管理 |
| TagsScreen.kt | 标签列表页面 |
| TagDetailScreen.kt | 标签详情页面 |
| TagSelectionDialog.kt | 标签选择对话框 |

### 功能清单
| 功能 | 状态 |
|------|------|
| 标签列表展示 | ✅ |
| 标签搜索 | ✅ |
| 创建标签 | ✅ |
| 编辑标签 | ✅ |
| 删除标签 | ✅ |
| 标签详情页 | ✅ |
| 为歌曲添加标签 | ✅ |
| 从歌曲移除标签 | ✅ |
| 创建新标签并添加 | ✅ |
| 播放标签下全部歌曲 | ✅ |

**阶段七完成，等待编译测试。**

---

## 阶段八: 筛选功能实现

### 任务清单
- [x] FilterViewModel 筛选逻辑和状态管理
- [x] FilterScreen 筛选页面UI
- [x] 布尔运算实现（交集、并集、差集）
- [x] 标签选择器组件
- [x] 筛选结果展示（歌曲列表、播放全部）
- [x] 保存筛选结果为歌单功能

### 进度记录
- [x] 创建 FilterViewModel
  - 管理框A/B/C的标签选择状态
  - 集成 FilterRepository 的布尔运算逻辑
  - 实时计算筛选结果
  - 支持保存为歌单
- [x] 实现 FilterScreen 筛选页面
  - 顶部标题栏 + 清除/保存按钮
  - 逻辑公式展示 ((A ∪ B) - C)
  - 框A：必须包含的所有标签（交集）
  - 框B：可选条件（并集）
  - 框C：排除标签（差集）
  - 标签选择器弹窗
  - 筛选结果展示（歌曲列表）
  - 播放全部功能
  - 保存为歌单弹窗
- [x] FilterRepository 已存在，无需修改
  - 布尔运算逻辑：(A AND ...) OR (B AND ...) - C

### 创建的文件
| 文件 | 说明 |
|------|------|
| FilterViewModel.kt | 筛选状态管理和逻辑 |
| FilterScreen.kt | 筛选页面UI |

### 功能清单
| 功能 | 状态 |
|------|------|
| 框A交集筛选 | ✅ |
| 框B并集筛选 | ✅ |
| 框C差集排除 | ✅ |
| 实时结果显示 | ✅ |
| 标签选择器弹窗 | ✅ |
| 播放筛选结果 | ✅ |
| 保存为歌单 | ✅ |
| 清除筛选条件 | ✅ |

**阶段八完成，等待编译测试。**

### 阶段八测试清单
**请在Android Studio中执行以下测试：**
1. 点击 **Build → Make Project** (Ctrl+F9) 编译项目
2. 确认无编译错误
3. 运行应用，进入筛选页面
4. 测试框A添加标签，验证筛选结果
5. 测试框B添加标签，验证并集逻辑
6. 测试框C添加标签，验证排除逻辑
7. 测试播放全部功能
8. 测试保存为歌单功能

---

## 阶段九: 正在播放页实现

### 任务清单
- [x] 播放队列弹窗
- [x] 歌词功能 (LRC解析器、歌词页面)
- [x] 播放控制增强

### 进度记录
- [x] 播放队列弹窗 (PlaybackQueueSheet.kt)
- [x] 歌词功能
  - LRC歌词解析器 (LyricsParser.kt)
  - 全屏歌词页面 (LyricsScreen.kt)
- [x] 播放控制增强
  - PlayerScreen 添加播放队列按钮、歌词入口
  - PlayerViewModel 添加队列管理方法
  - MusicPlayer 添加 queue/currentIndex StateFlow
  - PlaybackQueue 添加 playAtIndex 方法
  - PlaybackState 添加 currentIndex

### 创建的文件
| 文件 | 说明 |
|------|------|
| PlaybackQueueSheet.kt | 播放队列弹窗组件 |
| LyricsParser.kt | LRC歌词解析器 |
| LyricsScreen.kt | 全屏歌词页面 |

### 修改的文件
| 文件 | 修改内容 |
|------|----------|
| PlayerScreen.kt | 添加播放队列按钮、歌词入口 |
| PlayerViewModel.kt | 添加队列管理方法 |
| MusicPlayer.kt | 添加 queue/currentIndex StateFlow |
| PlaybackQueue.kt | 添加 playAtIndex 方法 |
| PlaybackState.kt | 添加 currentIndex |

### 功能清单
| 功能 | 状态 |
|------|------|
| 播放队列弹窗 | ✅ |
| 队列歌曲点击播放 | ✅ |
| 队列歌曲删除 | ✅ |
| 清空队列 | ✅ |
| LRC歌词解析 | ✅ |
| 全屏歌词显示 | ✅ |
| 歌词逐行高亮 | ✅ |
| 点击歌词跳转 | ✅ |
| 歌词自动滚动 | ✅ |

---

## 阶段十: 设置功能实现

### 任务清单
- [x] 设置页UI
- [x] 主题设置 (浅色/深色/跟随系统)
- [x] 数据备份与导入 (JSON格式)
- [x] 文件夹管理
- [x] 其他设置 (清除缓存、关于页面)

### 进度记录
- [x] 设置页UI (SettingsScreen.kt)
- [x] 主题设置 (ThemeManager.kt)
  - 浅色/深色/跟随系统
- [x] 数据备份与导入 (SettingsRepository.kt)
  - JSON格式导出
  - JSON格式导入
- [x] 文件夹管理
- [x] 其他设置
  - 清除缓存
  - 关于页面

### 创建的文件
| 文件 | 说明 |
|------|------|
| SettingsViewModel.kt | 设置状态管理 |
| SettingsScreen.kt | 设置页面UI |
| SettingsRepository.kt | 设置数据存储和备份逻辑 |
| ThemeManager.kt | 主题管理器 |

### 修改的文件
| 文件 | 修改内容 |
|------|----------|
| MainActivity.kt | 集成动态主题切换 |
| HomeScreen.kt | 添加设置入口按钮 |
| NavGraph.kt | 添加 Settings/Lyrics 路由 |
| Screen.kt | 添加 Settings 屏幕定义 |
| AppModule.kt | 添加 DataStore |
| build.gradle.kts | 添加 kotlinx-serialization |
| libs.versions.toml | 添加 serialization 依赖 |
| 实体类 | 添加 @Serializable 注解 |
| DAO | 添加备份/恢复方法 |

### 功能清单
| 功能 | 状态 |
|------|------|
| 主题切换 | ✅ |
| 导出JSON备份 | ✅ |
| 导入JSON备份 | ✅ |
| 文件夹管理 | ✅ |
| 清除缓存 | ✅ |
| 关于页面 | ✅ |

---

## 功能改进记录

### 改进1: FilterScreen 统一滑动
- [x] 修复筛选页整体滑动问题
  - 将 Column + 嵌套 LazyColumn 改为单一 LazyColumn
  - 筛选条件头部作为 LazyColumn 的 item 元素
  - 筛选结果歌曲列表作为 items 元素
  - 整个页面可以统一滑动

### 改进2: 筛选页无条件时显示所有歌曲
- [x] 修改 FilterViewModel
  - 当框A、框B、框C都为空时，显示所有歌曲
  - 从 SongRepository 获取全部歌曲

### 改进3: HomeScreen 添加排序功能
- [x] 添加排序按钮在"n首歌曲"标题栏右侧
- [x] 创建 SortDialog 排序对话框
- [x] 支持6种排序方式：
  - 添加时间（新→旧）
  - 添加时间（旧→新）
  - 歌曲名称（A→Z）
  - 歌曲名称（Z→A）
  - 歌手名称（A→Z）
  - 歌手名称（Z→A）

### 改进4: 实现"最近播放"功能
- [x] SongDao 添加 getRecentlyPlayedSongs() 查询
- [x] SongRepository 添加 getRecentlyPlayedSongs() 方法
- [x] HomeViewModel 添加最近播放数据流
- [x] 点击"最近播放"筛选按钮显示最近播放过的歌曲
- [x] 按最近播放时间倒序排列（最上面是最近播放的）

### 改进5: 实现艺术家列表页面
- [x] 创建 ArtistRepository.kt
  - 按艺术家名称聚合歌曲
  - 统计每个艺术家的歌曲数量
- [x] 创建 ArtistViewModel.kt
- [x] 创建 ArtistListScreen.kt
  - 显示艺术家列表
  - 按字母顺序排序
  - 右侧字母索引栏导航
  - 点击字母跳转到对应艺术家

### 改进6: 实现专辑网格页面
- [x] ArtistRepository 添加 getAlbums() 方法
  - 按专辑名称和艺术家聚合歌曲
  - 统计每个专辑的歌曲数量
- [x] 创建 AlbumViewModel.kt
- [x] 创建 AlbumListScreen.kt
  - 网格布局显示专辑
  - 右上角设置按钮
  - 支持排序（标题/年份/数量）
  - 支持调整列数（2/3/4列）

### 改进7: 更新导航
- [x] NavGraph.kt 添加 ARTIST_LIST 和 ALBUM_LIST 路由
- [x] HomeScreen 点击"艺术家"/"专辑"按钮导航到新页面

### 改进8: 添加歌曲改为独立页面
- [x] 创建 AddSongsToPlaylistScreen.kt 独立页面
  - 支持搜索歌曲
  - 支持多选（Checkbox）
  - 显示已选择数量
  - 顶部标题栏显示选择数量
  - 底部添加按钮
- [x] 更新 NavGraph 添加 ADD_SONGS_TO_PLAYLIST 路由
- [x] 修改 PlaylistDetailScreen 导航到新页面
- [x] 移除 PlaylistDetailScreen 中的弹窗代码

### 改进9: 歌单详情页头部显示歌曲数量
- [x] 修改 PlaylistHeader 组件
  - 移除歌单名称（顶部导航栏已显示）
  - 显示加粗的歌曲数量（与标签详情页一致）
  - 显示 "首歌曲" 文字

### 创建的文件
| 文件 | 说明 |
|------|------|
| ArtistRepository.kt | 艺术家和专辑数据聚合 |
| ArtistViewModel.kt | 艺术家列表状态管理 |
| ArtistListScreen.kt | 艺术家列表页面（带字母索引） |
| AlbumViewModel.kt | 专辑列表状态管理 |
| AlbumListScreen.kt | 专辑网格页面（带设置） |

### 修改的文件
| 文件 | 修改内容 |
|------|----------|
| FilterScreen.kt | 统一滑动布局 |
| FilterViewModel.kt | 无筛选条件时显示所有歌曲 |
| HomeScreen.kt | 添加排序按钮和对话框 |
| HomeViewModel.kt | 添加排序逻辑和最近播放 |
| SongDao.kt | 添加最近播放查询 |
| SongRepository.kt | 添加最近播放方法 |
| NavGraph.kt | 添加艺术家和专辑路由 |

---

## Bug修复记录

### 修复1: Screen.kt 中 Compose Material Icons 导入错误
- [x] 将 AutoMirrored 图标替换为标准图标 (LibraryMusic) 以避免接收器类型不匹配问题

### 修复2: PlaybackService.kt 拼写错误
- [x] 修复第11行拼写错误: `nimport` → `import`
- [x] 添加 MediaSessionCallback 的 `@OptIn(UnstableApi::class)` 注解

### 修复3: 隐藏底部导航栏
- [x] 修改 MainActivity.kt
  - 在进入歌单添加歌曲、专辑列表、艺术家列表、艺术家详情页面时隐藏底部导航栏
  - 添加 hideBottomBar 状态判断

### 修复4: 添加歌曲页面底部按钮位置
- [x] 修改 AddSongsToPlaylistScreen.kt
  - 使用 Box 布局使按钮固定在屏幕底部

### 修复5: 最近播放功能修复
- [x] 修改 MusicPlayer.kt
  - 播放歌曲时调用 songRepository.incrementPlayCount() 更新播放记录
  - 注入 SongRepository 依赖

### 修复6: 创建艺术家详情页面
- [x] 创建 ArtistDetailScreen.kt
  - 显示艺术家所有歌曲
  - 顶部显示歌曲数量
  - 播放全部按钮（顶部和底部各一个）
  - 点击歌曲播放该艺术家所有歌曲
- [x] 创建 ArtistDetailViewModel.kt
  - 根据艺术家名称获取歌曲列表
- [x] 更新 NavGraph.kt 添加 ARTIST_DETAIL 路由

### 修复7: 字母表快速导航改进
- [x] 创建 PinyinUtils.kt 拼音工具类
  - 中文按拼音首字母归类
  - 日文字符、数字、特殊符号归为 #
  - 提供固定字母索引列表 # + A-Z
- [x] 修改 ArtistListScreen.kt
  - 按拼音首字母分组艺术家
  - 右侧字母栏固定显示 # + A-Z
  - 无数据的字母显示为浅灰色
  - 点击/拖拽时只响应有数据的字母
- [x] 修改 AlbumListScreen.kt
  - 应用相同的拼音索引逻辑
  - 中文专辑按拼音首字母分组

### 修复8: UI尺寸调整
- [x] 修改 PlaylistDetailScreen.kt
  - 缩小 PlaylistHeader 尺寸
  - 封面从 80.dp 缩小到 56.dp
  - 整体内边距从 16.dp 减少到 12.dp
  - 歌曲数量改为水平排列，更加紧凑

### 修复9: MiniPlayer点击进入完整播放器
- [x] 创建 PlayerScreen.kt 完整播放器界面
  - 专辑封面占位 (点击显示歌词)
  - 歌曲信息显示 (歌名/歌手/专辑)
  - 进度条和播放控制
  - 播放模式切换 (顺序/列表循环/单曲循环)
  - 随机播放切换
- [x] 更新 NavGraph 添加 PLAYER 路由
- [x] 更新 MainActivity MiniPlayer onPlayerClick 导航到播放器
- [x] 播放器页面隐藏底部导航栏

### 修复10: 顶部空白问题
- [x] 移除 MainActivity.enableEdgeToEdge() 调用
- [x] 修复状态栏与标题栏之间的空白

### 修复11: 添加文件夹闪退
- [x] 修复 URI 处理逻辑
  - 添加 getRealPathFromUri() 方法解析真实路径
  - 添加 getFolderNameFromUri() 获取文件夹名称
- [x] 更新 ScanFolder 实体添加 name 字段
- [x] 数据库版本升级到 2
- [x] 更新 ScanFolderRepository 支持保存名称
- [x] 更新 HomeViewModel.addScanFolderByUri() 正确处理 URI

### 修复12: 字母索引拖拽手势修复
- [x] 修复艺术家列表和专辑列表页面的右侧字母索引栏拖拽手势生命周期Bug
  - 问题: `pointerInput` 使用了外部状态 `componentHeight`，首次加载时高度为0，导致手势计算失效
  - 解决: 移除外部高度状态，在 `awaitPointerEventScope` 内部直接访问 `size.height`
  - 修改 ArtistListScreen.kt 和 AlbumListScreen.kt

### 修复13: 播放页UI修复 (2026-03-14)
- [x] 修复下滑关闭效果
  - 问题: 页面缩小下沉效果不符合预期
  - 解决: 使用 `offset` 替代 `scale`，让页面向下滑出消失
- [x] 修复播放队列列表被遮盖
  - 问题: LazyColumn 没有设置 weight，被其他内容挤压
  - 解决: 添加 `.weight(1f)` 让列表占据剩余空间
- [x] 修复进度条拖动后弹回且不更新
  - 问题: Slider 拖动逻辑中 `isDragging` 状态管理不当，且拖动时未同步 `sliderPosition`
  - 解决:
    - 在非拖动状态下自动同步 `sliderPosition = positionFraction`
    - 避免在 `onValueChange` 中重复设置 `isDragging`
    - 拖动时始终显示 `sliderPosition` 计算的时间

### 创建的文件
| 文件 | 说明 |
|------|------|
| AlphabetIndexUtils.kt | 字母索引分组工具类（使用 ICU Transliterator） |
| ArtistDetailScreen.kt | 艺术家详情页面 |
| ArtistDetailViewModel.kt | 艺术家详情状态管理 |

### 修改的文件
| 文件 | 修改内容 |
|------|----------|
| MainActivity.kt | 特定页面隐藏底部导航栏 |
| AddSongsToPlaylistScreen.kt | 修复底部按钮位置 |
| MusicPlayer.kt | 修复最近播放记录更新 |
| ArtistListScreen.kt | 字母索引导航改进 |
| AlbumListScreen.kt | 字母索引导航改进 |
| PlaylistDetailScreen.kt | 缩小头部尺寸 |
| NavGraph.kt | 添加艺术家详情路由 |
| PlayerScreen.kt | 修复下滑手势、进度条逻辑 |
| ScanFolder.kt | 添加 name 字段 |
| ScanFolderRepository.kt | 支持保存名称 |
| HomeViewModel.kt | 正确处理 URI |
| PlaybackQueueSheet.kt | 修复列表高度 |

---

## 技术栈已配置

| 组件 | 版本 | 状态 |
|------|------|------|
| Kotlin | 1.9.22 | ✅ |
| Jetpack Compose | 2024.02.00 | ✅ |
| Hilt | 2.50 | ✅ |
| Room | 2.6.1 | ✅ |
| Media3 (ExoPlayer) | 1.2.1 | ✅ |
| Coil | 2.5.0 | ✅ |

## 权限已配置

- `READ_MEDIA_AUDIO` - 读取音频文件
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - 前台播放服务
- `POST_NOTIFICATIONS` - 通知权限

---

## 播放页UI修复 (2026-03-14)

### 修复1: 点击封面跳转到歌词页面
- [x] 移除顶部栏的歌词按钮
- [x] 专辑封面添加点击跳转手势
- **Git**: 101b1d6

### 修复2: 歌词页面全屏无底部导航栏
- [x] MainActivity.kt 添加 LYRICS 路由到 hideBottomBar 判断
- [x] LyricsScreen 添加下滑关闭手势
- **Git**: 101b1d6

### 修复3: 标签快捷位
- [x] PlayerScreen 添加 TagChipsRow 组件
- [x] 显示当前歌曲的标签（带 # 前缀的胶囊样式）
- [x] 点击 + 按钮弹出 TagSelectionDialog（复用首页弹窗）
- [x] 点击标签可移除
- **Git**: 101b1d6

### 修复4: 下滑关闭手势
- [x] PlayerScreen: 添加 detectVerticalDragGestures
- [x] PlaybackQueueSheet: 添加拖动手势和指示条
- [x] LyricsScreen: 添加下滑关闭手势
- **Git**: 101b1d6

### 修复5: 进度条拖动后更新问题
- [x] 使用本地状态 sliderPosition 和 isDragging 跟踪拖动
- [x] 拖动时显示拖动位置，释放后才触发 onSeek
- **Git**: 101b1d6

### 修复6: 专辑封面显示
- [x] 创建统一的 AlbumArt 组件
- [x] 使用 Coil SubcomposeAsyncImage 加载 MediaStore 专辑封面
- [x] 更新 PlayerScreen、SongItem、MiniPlayer
- **Git**: 101b1d6, 9dc11d0

### 修复7: Coil API 修正
- [x] 将 AsyncImage 替换为 SubcomposeAsyncImage
- [x] 使用 loading 和 error 参数显示占位符
- **Git**: 9dc11d0

### 创建的文件
| 文件 | 说明 |
|------|------|
| - | 无新文件 |

### 修改的文件
| 文件 | 修改内容 |
|------|----------|
| MainActivity.kt | 添加 LYRICS 到 hideBottomBar |
| PlayerScreen.kt | 重构播放页UI，添加标签、手势、封面 |
| LyricsScreen.kt | 添加下滑关闭手势 |
| PlaybackQueueSheet.kt | 添加下滑关闭手势和指示条 |
| SongItem.kt | 添加专辑封面显示 |
| MiniPlayer.kt | 添加专辑封面显示 |

---

## 播放队列修复 (2026-03-14)

### 修复1: 播放列表长按拖动排序
- [x] 修改 PlaybackQueueSheet.kt
  - 添加 `detectDragGesturesAfterLongPress` 实现长按拖拽
  - 添加拖动指示器（DragHandle 图标）
  - 实时计算目标位置并交换
  - 添加 `onMoveSong` 回调
- [x] 添加 moveSong 方法到 MusicPlayer 和 PlayerViewModel

### 修复2: 歌词页面显示歌词
- [x] 修改 MusicScanner.kt
  - 添加 `loadLyricsFromFile()` 方法
  - 扫描时自动查找同目录 .lrc 文件
  - 支持 UTF-8 编码
- [x] 歌词解析使用 LyricsParser.parseLrc()

### 修复3: 播放列表删除立即刷新
- [x] 修改 MusicPlayer.removeFromQueue()
  - 删除后更新 `_currentIndex.value`
  - 删除后更新 `PlaybackState.currentIndex`

### 修复4: 播放列表当前播放索引同步
- [x] 修改 MusicPlayer.playAtIndex()
  - 更新 `PlaybackState.currentIndex` 为正确索引
  - 问题根源：之前只更新了 `_currentIndex`，未同步到 PlaybackState

### 创建的文件
| 文件 | 说明 |
|------|------|
| - | 无新文件 |

### 修改的文件
| 文件 | 修改内容 |
|------|----------|
| PlaybackQueueSheet.kt | 添加长按拖拽排序功能 |
| PlayerScreen.kt | 添加 onMoveSong 回调 |
| PlayerViewModel.kt | 添加 moveSong 方法 |
| MusicPlayer.kt | 修复 currentIndex 同步，修复 removeFromQueue 刷新 |
| MusicScanner.kt | 添加歌词文件读取 |

---

## 播放问题修复

### 修复1: 内嵌歌词解析
- [x] 修改 MusicScanner.kt
  - 添加 `readEmbeddedLyrics()` 方法使用 MediaMetadataRetriever
  - 优先读取音频文件内嵌歌词，其次查找 .lrc 文件
  - 支持 METADATA_KEY_LYRICS 和 METADATA_KEY_COMMENT

### 修复2: 进度条跳动问题（状态分离）
- [x] 修改 PlayerScreen.kt 的 ProgressBar 组件
  - 引入 `isDragging` 标志位控制状态分离
  - `sliderValue` 独立管理 UI 进度
  - LaunchedEffect 监听真实进度，仅在非拖拽时同步
  - onValueChange 设置 isDragging = true 拦截底层更新
  - onValueChangeFinished 触发 seekTo 并恢复 isDragging = false

### 修复3: 播放列表拖拽中断（稳定Key + 状态提升）
- [x] 重写 PlaybackQueueSheet.kt
  - 稳定的 Item Key：`items(key = { song.id })` 使用唯一 ID
  - 固定的 pointerInput Key：`.pointerInput(song.id)` 确保协程存活
  - 状态提升：`draggedItemId`、`draggedItemIndex`、`dragOffsetY` 由父级管理
  - 支持单次长按、连续跨越多行平滑排序

### 修改的文件
| 文件 | 修改内容 |
|------|----------|
| MusicScanner.kt | 添加内嵌歌词解析 |
| PlayerScreen.kt | 修复 ProgressBar 状态分离 |
| PlaybackQueueSheet.kt | 修复拖拽中断问题 |

---

- **0029bb3** - 修复内嵌歌词、进度条跳动、播放列表拖拽中断


  - 确保 StateFlow 能检测到列表变化并触发重组

### 修复3: 播放列表长按拖拽排序
- [x] 重写 PlaybackQueueSheet.kt
  - 使用 SnapshotStateList 管理内部列表状态
  - 拖拽手柄使用 `detectDragGesturesAfterLongPress`
  - 被拖动项有悬浮效果（scale + elevation）
  - 实时计算目标位置并交换数据
  - 不拦截整行的普通点击事件

### 修复4: 歌词无法显示
- [x] 修改 MusicScanner.kt 的 loadLyricsFromFile()
  - 支持多种文件扩展名（.lrc, .LRC, .txt, .TXT）
  - 支持 UTF-8 和 GBK 编码自动检测
  - 修复 GBK 编码读取逻辑
- [x] 修改 LyricsScreen.kt
  - 添加详细的空状态提示，说明如何添加歌词文件

### 修改的文件
| 文件 | 修改内容 |
|------|----------|
| PlayerScreen.kt | 修复 ProgressBar 同步逻辑 |
| PlaybackQueue.kt | getQueue() 返回列表副本 |
| PlaybackQueueSheet.kt | 实现长按拖拽排序 |
| MusicScanner.kt | 修复歌词读取，支持多种编码 |
| LyricsScreen.kt | 改进空状态提示 |

---

- **5a52aad** - 修复播放问题

