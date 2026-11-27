# ColorOS SMS Guard

LSPosed 模块，在不升级系统的情况下拦截 **CVE-2025-10184**（OPPO / 一加 / realme 的电话/短信存储组件漏洞：任意应用无需权限即可读取短信），并提供一个查看拦截记录的界面。

## 原理

漏洞出在 `com.android.providers.telephony` 的三个 OPlus 组件：`PushMessageProvider`、`PushShopProvider`、`ServiceNumberProvider`。它们没有校验写权限，`update()` 里又拼接了调用方传入的 SQL，可以盲注读取 `mmssms.db`。

模块在 `com.android.phone` 进程里 hook `ContentProvider.Transport`（所有跨进程 Provider 调用的入口），对这三个组件执行一条规则：

- 系统 / 核心进程（app id < 10000）：放行
- 持有 `READ_SMS` 的应用（如默认短信应用）：放行，它们本来就能读短信
- 其余应用：抛出 `SecurityException` 拒绝，并记录日志

不依赖去掉 `mmssms.db` 的读权限，所以短信收发不受影响。

## 使用

1. 从 Actions 的 `smsguard-apk` 产物下载并安装 APK
2. LSPosed 中启用模块，作用域勾选「电话/短信存储」和「电话服务」
3. 重启手机
4. 允许本应用自启动（ColorOS 会拦截发往未运行应用的广播，否则界面收不到记录）
5. 打开应用，看到「模块已在电话进程生效」即生效

如果之前按酷安方案去掉了 `mmssms.db` 的读权限，确认模块生效后恢复为 `0660`：

```sh
su -c chmod 660 /data/user/0/com.android.providers.telephony/databases/mmssms.db
```

## 日志

- 界面：按应用汇总的拦截次数、首次/最近时间、被调用的组件。同一应用 10 秒内的调用合并上报，次数为下限值
- LSPosed 日志：搜索 `SmsGuard:`，即使广播没送达也会有记录

## 构建

只在 GitHub Actions 中构建（`.github/workflows/build.yml`）：跑单元测试并打包 release APK。签名密钥来自仓库 Secrets：`SIGNING_KEYSTORE_BASE64`、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`；未配置时产物为未签名 APK。

`xposed-stub/` 是 Xposed API 的编译期桩，运行时由 LSPosed 提供真实实现，不会打进 APK。
