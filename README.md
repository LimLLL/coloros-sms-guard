# ColorOS SMS Guard

LSPosed 模块，在**不升级系统**的情况下修复 **CVE-2025-10184**（OPPO / 一加 / realme 的「电话/短信存储」组件漏洞：任意应用无需权限即可读取全部短信，包括验证码），并提供拦截日志界面。

## 你可能是因为这个来的

漏洞公开后，网上流传一个 root 自救方案：**去掉 `mmssms.db` 的读权限**。它确实能挡住漏洞，但会把系统短信库一起弄坏，症状是：

- 给**从没发过短信的号码**发不了短信，新建短信时**发送按钮是灰的**
- 只能在已有会话里发，而且发出去的是 5G 消息
- 依赖短信的验证（Google 账号等）因此失败

本模块换一种方式拦截漏洞，装好后就可以把 `mmssms.db` 权限恢复正常（见下文），短信收发恢复正常，同时仍然受保护。

## 原理

漏洞出在 `com.android.providers.telephony` 的三个 OPlus 组件：`PushMessageProvider`、`PushShopProvider`、`ServiceNumberProvider`。它们没有校验写权限，`update()` 里又拼接了调用方传入的 SQL，可以盲注读取 `mmssms.db`。

模块在 `com.android.phone` 进程里 hook `ContentProvider.Transport`（所有跨进程 Provider 调用的入口），对这三个组件执行一条规则：

- 系统 / 核心进程（app id < 10000）：放行
- 持有 `READ_SMS` 的应用（如默认短信应用）：放行，它们本来就能读短信
- 其余应用：抛出 `SecurityException` 拒绝，并记录日志

## 要求

- 已 root，装有 LSPosed（Zygisk 版即可）
- 已测试：一加 11（PJD110）ColorOS 14.0.0.860 / Android 14
- 其他 ColorOS 12–15 机型使用相同的漏洞组件，理论上可用但未逐一测试，装好后以「自检」结果为准
- 如果「设置 → 软件更新 → 自动更新设置 → 系统文件更新」能收到官方补丁（电话/短信存储组件更新），优先用官方补丁

## 使用

1. 从 [Releases](../../releases) 下载 APK 并安装
2. LSPosed 中启用模块，作用域勾选「电话/短信存储」和「电话服务」
3. 让模块加载进电话进程（它是常驻进程，不会自己退出）：重启手机，或 `su -c 'kill $(pidof com.android.phone)'`（系统会立即拉起，期间断网几秒、通话中断）
4. 在系统设置中允许本应用自启动（ColorOS 会拦截发往未运行应用的广播，否则界面收不到记录）
5. 打开应用点「自检」：三个组件都显示「已被本模块拦截」即生效。本应用没有短信权限，探测方式与漏洞利用相同（用不存在的列做 `update`，不改任何数据）

### 恢复 `mmssms.db` 权限

只有之前按上述方案改过权限才需要。**先确认第 5 步自检通过**，再执行：

```sh
su -c chmod 660 /data/user/0/com.android.providers.telephony/databases/mmssms.db
```

建议先备份 `mmssms.db` 和 `/data/user/0/com.android.mms/databases/bugle_db`。改权限期间收发的短信只存在「信息」应用自己的库里，系统短信库中没有，恢复权限后它们仍会正常显示。

## 日志

- 界面：按应用汇总的拦截次数、首次 / 最近时间、被调用的组件。同一应用 10 秒内的调用合并上报，次数为下限值
- LSPosed 日志：搜索 `SmsGuard:`，即使广播没送达也会有记录
- 应用不申请网络权限，记录只保存在本机

## 构建与发布

只在 GitHub Actions 中构建（`.github/workflows/build.yml`）：每次推送跑单元测试并打包 APK；推送与 `versionName` 一致的标签 `vX.Y.Z` 时自动发布 Release。

签名密钥来自仓库 Secrets：`SIGNING_KEYSTORE_BASE64`、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`。未配置时普通构建产出未签名 APK，发布会直接失败。Fork 自行构建时需要配置自己的密钥，且无法覆盖安装本仓库发布的版本。

`xposed-stub/` 是 Xposed API 的编译期桩，运行时由 LSPosed 提供真实实现，不会打进 APK。

## 免责声明

本模块会注入系统电话进程，请自行承担使用风险。出现异常时在 LSPosed 中停用模块并重启电话进程即可恢复。
