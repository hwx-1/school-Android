# Android 端变更说明（2026-09-03）

> 对应提交：`3290ae8 feat: 完善社区客户端：AI 问答流式、申诉举报与用户主页等`
> 验证：`./gradlew :app:compileDebugKotlin` 编译通过；已推送至 GitHub `origin/main`。

## 一、AI 问答重构（本次核心）

### 1. 进入即新对话

- 首页「AI 助手」与百宝箱 AI 入口现在**直接进入新对话**，不再先进入会话列表页。

- `Route.AI` 渲染 `AIChatScreen(nav, null, "")`，页面启动时自动创建会话并切换到带 `id` 的路由。

### 2. 对话记录内嵌

- 删除独立的会话列表页，历史会话改为**内嵌弹层**（`HistoryDialog`）。

- 对话页右上角并排两个入口：`对话记录`、`新对话`。

- 点击历史会话可切换；历史按 `created_at` 倒序（最新在上、越久越靠下）。

### 3. 修复新建对话不生效

- 原因：原 `newConversation()` 用 `nav.replace` 切换路由，但 `AIChatScreen` 内的 `remember` 状态不会随参数变化而重置。

- 方案：`Route.AIChat` 分支用 `key(route.id)` 按会话 id 强制重建页面，新建/切换会话都会正确刷新。

### 4. 消息展示

- 用户消息：右侧浅灰圆角气泡。

- AI 消息：左侧白色卡片气泡（带边框）。

### 5. 立即展示用户消息

- 发送后**先以临时负 id 追加用户消息**，不等 AI 回答返回。

- `done` 事件返回后用服务端真实消息替换临时消息。

### 6. SSE 流式渲染

- 新增 `Http.stream`（逐行解析 `data:` 事件）与 `Api.askAIStream`。

- 事件类型：`thinking`（思考增量）、`text`（正文增量）、`done`（落库 + remaining）、`error`。

- 正文随增量逐字渲染。

### 7. 思考过程

- AI 回答的 `reasoning` 内容以「思考过程」块展示，**默认收起**，可展开/收起。

### 相关文件

- `app/src/main/java/com/example/schoolandorid/ui/screens/AIScreens.kt`

- `app/src/main/java/com/example/schoolandorid/MainActivity.kt`

- `app/src/main/java/com/example/schoolandorid/model/Models.kt`

- `app/src/main/java/com/example/schoolandorid/net/Api.kt`

- `app/src/main/java/com/example/schoolandorid/net/Http.kt`

***

## 二、新增功能

### 1. 忘记密码

- 登录页新增「忘记密码？」入口。

- 新增 `ForgotPasswordScreen`：手机号 + 短信验证码（`purpose=reset`）+ 新密码，成功后返回登录。

- 接口：`Api.resetPassword`，模型 `ResetPasswordResp`。

### 2. 公告详情

- 公告列表卡片可点击进入详情。

- 新增 `AnnouncementDetailScreen`：标题、发布时间、配图、正文、外链。

- 模型 `Announcement` 补充 `image_url` / `link_url` / `link_text` / `updated_at` / `published_at`。

- 接口：`Api.getAnnouncement`，模型 `AnnouncementResp`。

### 3. 公开用户主页

- 新增 `UserProfileScreen`：公开资料、认证徽标、TA 的公开帖子、发私信、举报。

- 帖子作者头像/昵称可点击进入该主页。

- 接口：`Api.getUser`，模型 `PublicUserResp`。

### 4. 我的申诉

- 新增 `AppealsScreen`：查看申诉记录，`punishmentId > 0` 时可直接发起对应处罚的申诉。

- 模型 `Appeal`，接口 `Api.myAppeals` / `Api.createAppeal`，模型 `SubmitAppealResp`。

### 5. 举报弹窗

- 新增通用 `ReportDialog`：单选原因 + 选填补充说明（200 字内）。

- 支持目标：帖子 / 评论 / 用户 / 私信。

- 举报原因常量 `REPORT_REASONS`，目标类型 `ReportTarget`。

### 6. 帖子编辑模式

- `ComposeScreen` 支持 `postId`，编辑模式加载旧帖并调用 `Api.updatePost`。

- 帖子详情「更多」菜单新增「编辑帖子」入口。

- 模型 `UpdatePostResp`。

***

## 三、修改与增强

### 帖子详情

- 新增右上角「更多」菜单（`⋯`）：

  - 自己的帖子：编辑、删除。

  - 他人帖子：举报。

  - 通用：复制链接。

- 新增删除帖子二次确认弹窗。

- 评论新增举报、点击作者进入主页。

### 列表与卡片

- `PostCard` 新增 `onAuthor` 回调，作者可点击跳转主页。

- `CommentItemView` 新增 `onReport`、`onAuthor`。

- `AnnouncementCard` 新增 `onOpen`，点击进入公告详情。

- 首页、搜索、内容列表、公告列表、帖子详情等页面接入作者跳转与举报入口。

### 发帖图片选择

- 修复 `PickMultipleVisualMedia(maxItems = 剩余可传张数)` 在剩余为 1 时可能闪退的问题。

- 改为固定上限 `MAX_POST_IMAGES`，回调内按剩余可传张数截断。

### 网络层

- `Http.kt`：抽取 `buildRequest`，新增 `streamClient`（读超时 120s）与 `stream` SSE 读取方法。

- `Api.kt`：新增 `askAIStream`，以及上述新功能对应接口。

### 首页信息流分页

- 首页帖子改为 `limit=15 / offset` 分页加载，首屏 15 条，接近底部自动加载下一页。

- `Api.listPosts` 委托 `listPostsPage`；新增 `listPostsPage(query, mineOnly, limit, offset)` 与 `PostsPage(items/total/has_more)`。

- 底部状态：加载中「正在加载更多…」→ 可加载「继续下滑加载更多」→ 加载完「已经看到这里了」。

### AI 回答格式化

- 新增 `formatAIAnswer`，渲染前清洗 Markdown 记号（`**`、`-`、`#`、`>`、反引号、链接等），与 Web 端一致。

### 模型与路由

- `Nav.kt`：新增 `ForgotPassword`、`AnnouncementDetail`、`UserProfile`、`Appeals`；`Compose` 由对象改为 `Compose(postId)`。

- `Models.kt`：新增 `AIStreamEvent`、`Appeal`、`MessageResp`、`UpdatePostResp`、`PublicUserResp`、`AnnouncementResp`、`SubmitAppealResp`、`AIFeedbackResp`、`ResetPasswordResp` 等。

- `DirectConversationDetail` 补充 `updated_at`、`unread_count`。

### 配置

- `AppConfig.API_BASE_URL` 默认切换为生产 HTTPS `https://xsnbb.xyz`。

- `AndroidManifest.xml` 补充内测阶段明文流量说明注释。

***

## 四、文件清单

| 类型 | 文件                                                                          |
| -- | --------------------------------------------------------------------------- |
| 新增 | `app/src/main/java/com/example/schoolandorid/ui/components/ReportDialog.kt` |
| 新增 | `app/src/main/java/com/example/schoolandorid/ui/screens/UserScreens.kt`     |
| 修改 | `app/src/main/java/com/example/schoolandorid/MainActivity.kt`               |
| 修改 | `app/src/main/AndroidManifest.xml`                                          |
| 修改 | `app/src/main/java/com/example/schoolandorid/config/AppConfig.kt`           |
| 修改 | `app/src/main/java/com/example/schoolandorid/model/Models.kt`               |
| 修改 | `app/src/main/java/com/example/schoolandorid/nav/Nav.kt`                    |
| 修改 | `app/src/main/java/com/example/schoolandorid/net/Api.kt`                    |
| 修改 | `app/src/main/java/com/example/schoolandorid/net/Http.kt`                   |
| 修改 | `app/src/main/java/com/example/schoolandorid/ui/components/Business.kt`     |
| 修改 | `app/src/main/java/com/example/schoolandorid/ui/screens/AIScreens.kt`       |
| 修改 | `app/src/main/java/com/example/schoolandorid/ui/screens/AuthScreens.kt`     |
| 修改 | `app/src/main/java/com/example/schoolandorid/ui/screens/MeScreens.kt`       |
| 修改 | `app/src/main/java/com/example/schoolandorid/ui/screens/PostScreens.kt`     |
| 修改 | `app/src/main/java/com/example/schoolandorid/ui/screens/TabsScreen.kt`      |
| 修改 | `.idea/deploymentTargetSelector.xml`、`.idea/misc.xml`、`.idea/vcs.xml`       |

