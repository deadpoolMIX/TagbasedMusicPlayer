# 开发日志

## 项目: 标签化音乐播放器

---

## 一、项目架构

### 技术栈
| 组件 | 版本 |
|------|------|
| Kotlin | 1.9.22 |
| Jetpack Compose | 2024.02.00 |
| Hilt | 2.50 |
| Room | 2.6.1 |
| Media3 (ExoPlayer) | 1.2.1 |
| Coil | 2.5.0 |

### 权限配置
- `READ_MEDIA_AUDIO` - 读取音频文件
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - 前台播放服务
- `POST_NOTIFICATIONS` - 通知权限

---

## 二、开发阶段

### 阶段一: 项目基础搭建
- [x] 创建Android Studio项目
- [x] 配置Gradle和依赖库
- [x] 配置Hilt依赖注入
- [x] 配置Navigation导航
- [x] 创建MainActivity + 底部导航栏
- [x] 创建主页面占位符
- [x] 创建主题系统 (Light/Dark)
- [x] 配置权限
- [x] 创建数据库和实体占位符
- [x] 创建播放器服务占位符

### 阶段二: 数据层实现
- [x] Room数据库完整搭建
  - 6个实体: Song, Tag, SongTag, Playlist, PlaylistSong, ScanFolder
  - 4个DAO: SongDao, TagDao, PlaylistDao, ScanFolderDao
- [x] 音乐扫描服务 (MediaStore查询)
- [x] Repository层实现 (SongRepository, TagRepository, PlaylistRepository, ScanFolderRepository, FilterRepository)

**数据库实体关系:**
```
Song (歌曲) ←→ SongTag (多对多) ←→ Tag (标签)
  ↓
PlaylistSong → Playlist (歌单)
ScanFolder (扫描文件夹)
```

**布尔运算公式:** `(A ∩ A2 ∩ ...) ∪ (B ∩ B2 ∩ ...) - C`

### 阶段三: 音频播放核心
- [x] ExoPlayer集成 (播放/暂停/切歌/Seek)
- [x] 播放队列管理 (随机播放/拖拽排序)
- [x] MediaSession集成 (耳机线控/蓝牙)
- [x] 通知栏播放器
- [x] 播放状态管理
- [x] 播放模式切换 (顺序/单曲循环/随机)

### 阶段四: UI基础组件
- [x] MiniPlayer组件 - 底部悬浮播放器
- [x] SongItem组件 - 歌曲列表项
- [x] TagChip组件 - 标签胶囊
- [x] SearchBar组件 - 搜索框
- [x] SongActionSheet组件 - 歌曲操作底部面板

### 阶段五: 首页功能实现
- [x] HomeViewModel创建
- [x] 歌曲列表展示和搜索
- [x] 排序功能 (6种排序方式)
- [x] 分类筛选 (全部/最近播放)
- [x] 权限申请和文件夹管理
- [x] 操作菜单集成

### 阶段六: 歌单功能实现
- [x] PlaylistViewModel创建
- [x] PlaylistScreen歌单列表页面
- [x] PlaylistDetailScreen歌单详情页
- [x] AddToPlaylistDialog添加到歌单对话框
- [x] AddSongsToPlaylistScreen添加歌曲页面

### 阶段七: 标签功能实现
- [x] TagViewModel创建
- [x] TagsScreen标签列表页面
- [x] TagDetailScreen标签详情页
- [x] TagSelectionDialog标签选择对话框
- [x] AddSongsToTagScreen添加歌曲页面

### 阶段八: 筛选功能实现
- [x] FilterViewModel筛选逻辑
- [x] FilterScreen筛选页面
- [x] 布尔运算实现 (交集/并集/差集)
- [x] 筛选结果展示和播放
- [x] 保存筛选结果为歌单

### 阶段九: 正在播放页实现
- [x] PlayerScreen完整播放器界面
- [x] PlaybackQueueScreen全屏播放队列页面
- [x] LyricsParser LRC歌词解析器
- [x] LyricsScreen全屏歌词页面
- [x] 播放控制增强

### 阶段十: 设置功能实现
- [x] SettingsScreen设置页面
- [x] 主题设置 (浅色/深色/跟随系统)
- [x] 数据备份与导入 (JSON格式)
- [x] 文件夹管理
- [x] 清除缓存和关于页面

---

## 三、核心文件清单

### 数据层
| 文件 | 说明 |
|------|------|
| Song.kt, Tag.kt, SongTag.kt, Playlist.kt, PlaylistSong.kt, ScanFolder.kt | 数据库实体 |
| SongDao.kt, TagDao.kt, PlaylistDao.kt, ScanFolderDao.kt | 数据访问对象 |
| SongRepository.kt, TagRepository.kt, PlaylistRepository.kt, ScanFolderRepository.kt, FilterRepository.kt | 数据仓库 |

### 播放层
| 文件 | 说明 |
|------|------|
| MusicPlayer.kt | ExoPlayer封装，播放控制 |
| PlaybackQueue.kt | 队列管理，随机播放，排序 |
| PlaybackState.kt | 播放状态，事件定义 |
| PlaybackService.kt | MediaSession，通知栏，前台服务 |

### ViewModel层
| 文件 | 说明 |
|------|------|
| HomeViewModel.kt | 首页状态管理 |
| PlaylistViewModel.kt | 歌单状态管理 |
| TagViewModel.kt | 标签状态管理 |
| FilterViewModel.kt | 筛选状态管理 |
| PlayerViewModel.kt | 播放控制 |
| SettingsViewModel.kt | 设置状态管理 |
| ArtistViewModel.kt | 艺术家状态管理 |

### UI层
| 文件 | 说明 |
|------|------|
| HomeScreen.kt | 首页 |
| PlaylistScreen.kt, PlaylistDetailScreen.kt, AddSongsToPlaylistScreen.kt | 歌单相关页面 |
| TagsScreen.kt, TagDetailScreen.kt, AddSongsToTagScreen.kt | 标签相关页面 |
| FilterScreen.kt | 筛选页面 |
| PlayerScreen.kt, LyricsScreen.kt, PlaybackQueueScreen.kt | 播放相关页面 |
| ArtistListScreen.kt, ArtistDetailScreen.kt | 艺术家相关页面 |
| SettingsScreen.kt | 设置页面 |
| MiniPlayer.kt, SongItem.kt, TagChip.kt | 通用组件 |

### 工具类
| 文件 | 说明 |
|------|------|
| MusicScanner.kt | 音乐扫描服务 |
| LyricsParser.kt | LRC歌词解析 |
| PermissionUtils.kt | 权限检查工具 |
| AlphabetIndexUtils.kt | 字母索引工具 |

---

## 四、Bug修复记录

### 播放相关
- **进度条拖动后弹回**: 引入`isDragging`状态分离，拖动时显示拖动位置，释放后才触发seek
- **进度条跳动**: 使用`sliderValue`独立管理UI进度，非拖拽时同步真实进度
- **播放队列删除不刷新**: 删除后同步更新`_currentIndex`和`PlaybackState.currentIndex`
- **播放列表拖拽中断**: 使用稳定Key(`song.id`)和状态提升修复
- **播放列表key重复**: 使用`"${index}_${song.id}"`避免重复
- **播放队列页面问题**: 重构为全屏页面，只显示当前播放歌曲及之后的歌曲

### 歌词相关
- **歌词无法显示**: 支持多种扩展名(.lrc/.LRC)和编码(UTF-8/GBK)
- **内嵌歌词读取**: 使用 MediaMetadataRetriever 提取内嵌歌词 (API 29+)
- **歌词页面重写**: 全屏歌词页面，逐行高亮、点击跳转、自动滚动同步
- **下滑关闭手势**: 恢复歌词页下滑关闭功能

### UI相关
- **顶部空白问题**: 移除`enableEdgeToEdge()`调用
- **字母索引拖拽手势失效**: 在`awaitPointerEventScope`内部访问`size.height`
- **FilterScreen滑动问题**: 改为单一LazyColumn统一滑动
- **专辑封面显示**: 使用`ContentUris.withAppendedId()`正确构建URI
- **Coil API修正**: 将AsyncImage替换为SubcomposeAsyncImage

### 功能相关
- **添加文件夹闪退**: 添加URI解析方法获取真实路径
- **最近播放不记录**: 播放时调用`incrementPlayCount()`更新记录
- **筛选无条件时无结果**: 框A/B/C都为空时显示所有歌曲

---

## 五、功能改进记录

### 播放功能增强
- 播放页下滑关闭手势
- 播放队列长按拖拽排序
- 点击封面跳转歌词页
- 标签快捷位显示当前歌曲标签
- "我喜欢"歌单功能(心形按钮收藏)
- 播放队列全屏页面(下滑关闭，隐藏底部导航栏和MiniPlayer)
- 播放队列只显示当前播放歌曲及之后的歌曲

### 导航改进
- 隐藏特定页面底部导航栏
- 歌词页全屏无底部导航栏
- 艺术家点击进入详情页

### UI尺寸优化
- 创建`AppDimensions`统一管理尺寸
- 底部导航栏高度缩小(80dp→56dp)
- 列表项尺寸统一绑定
- 详情页Header尺寸统一绑定

---

## 六、UI重构记录

### 删除内容
- 删除专辑页面(AlbumListScreen, AlbumDetailScreen)
- 删除首页搜索框(改为按钮)
- 删除首页筛选按钮行("全部"/"最近播放")

### 新增内容
- 歌手作为底部导航Tab
- 标签详情页添加歌曲按钮
- AppDimensions尺寸管理

### 修改内容
- 搜索框改为右上角搜索按钮
- 底部导航栏移除文字标签
- 播放页只显示曲名和歌手名(移除专辑名)
- 列表项和标题栏高度缩小

---

## 七、数据库版本

| 版本 | 变更 |
|------|------|
| 1 | 初始版本 |
| 2 | ScanFolder添加name字段 |
| 3 | Playlist添加isSystem字段 |

---

## 八、Git提交记录

| Commit | 说明 |
|--------|------|
| 3c54759 | fix(lyrics): 修复歌词页下滑关闭和歌词解析问题 |
| db97ae7 | feat(lyrics): 重写歌词功能，支持内嵌歌词提取 |
| 59a3b6e | fix(player): 修复播放队列页面问题 |
| c6fcdc8 | feat(player): 重构播放队列为全屏页面 |
| 820429b | docs: 更新开发日志 |
| e392434 | feat(ui): UI尺寸统一和添加标签歌曲功能 |
| 266b55b | style(ui): 缩小首页统计信息行高度 |
| d8e4ceb | feat(ui): 删除首页筛选按钮行 |
| 3e39016 | refactor(ui): UI重构，删除专辑页，歌手作为Tab |
| 180ac8b | feat: 添加专辑详情页 |
| 7419d92 | feat: "我喜欢"歌单功能 |
| 101b1d6 | fix: 播放页UI修复 |
| 0029bb3 | fix: 进度条跳动、播放列表拖拽中断 |
| e81cb79 | feat: 内嵌歌词提取 |

---