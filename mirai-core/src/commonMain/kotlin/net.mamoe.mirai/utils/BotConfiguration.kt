/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */
@file:Suppress("unused", "DEPRECATION_ERROR")

package net.mamoe.mirai.utils

import kotlinx.coroutines.Job
import net.mamoe.mirai.Bot
import net.mamoe.mirai.network.BotNetworkHandler
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * [Bot] 配置
 */
@Suppress("PropertyName")
open class BotConfiguration {
    /** 日志记录器 */
    var botLoggerSupplier: ((Bot) -> MiraiLogger) = { DefaultLogger("Bot(${it.id})") }

    /** 网络层日志构造器 */
    @OptIn(MiraiInternalAPI::class)
    var networkLoggerSupplier: ((BotNetworkHandler) -> MiraiLogger) = { DefaultLogger("Network(${it.bot.id})") }

    /** 设备信息覆盖. 默认使用随机的设备信息. */
    var deviceInfo: ((Context) -> DeviceInfo)? = null

    /** 父 [CoroutineContext]. [Bot] 创建后会覆盖其 [Job], 但会将这个 [Job] 作为父 [Job] */
    var parentCoroutineContext: CoroutineContext = EmptyCoroutineContext

    /** 心跳周期. 过长会导致被服务器断开连接. */
    var heartbeatPeriodMillis: Long = 60.secondsToMillis

    /**
     * 每次心跳时等待结果的时间.
     * 一旦心跳超时, 整个网络服务将会重启 (将消耗约 5s). 除正在进行的任务 (如图片上传) 会被中断外, 事件和插件均不受影响.
     */
    var heartbeatTimeoutMillis: Long = 2.secondsToMillis

    /** 心跳失败后的第一次重连前的等待时间. */
    var firstReconnectDelayMillis: Long = 5.secondsToMillis

    /** 重连失败后, 继续尝试的每次等待时间 */
    var reconnectPeriodMillis: Long = 5.secondsToMillis

    /** 最多尝试多少次重连 */
    var reconnectionRetryTimes: Int = Int.MAX_VALUE

    /** 验证码处理器 */
    var loginSolver: LoginSolver = LoginSolver.Default

    /** 使用协议类型 */
    @SinceMirai("1.0.0")
    var protocol: MiraiProtocol = MiraiProtocol.ANDROID_PAD

    /** 缓存策略  */
    @SinceMirai("1.0.0")
    @MiraiExperimentalAPI
    var fileCacheStrategy: FileCacheStrategy = FileCacheStrategy.PlatformDefault

    @SinceMirai("1.0.0")
    enum class MiraiProtocol(
        /** 协议模块使用的 ID */
        @JvmField internal val id: Long
    ) {
        /**
         * Android 手机.
         *
         * - 与手机冲突
         * - 与平板和电脑不冲突
         */
        ANDROID_PHONE(537062845),

        /**
         * Android 平板.
         *
         * - 与平板冲突
         * - 与手机和电脑不冲突
         */
        ANDROID_PAD(537062409)
    }

    companion object {
        /** 默认的配置实例. 可以进行修改 */
        @JvmStatic
        val Default = BotConfiguration()
    }

    /**
     * 不显示网络日志
     */
    @OptIn(MiraiInternalAPI::class)
    @ConfigurationDsl
    fun noNetworkLog() {
        networkLoggerSupplier = { _: BotNetworkHandler -> SilentLogger }
    }

    /**
     * 使用文件存储设备信息
     *
     * 此函数只在 JVM 有效. 在其他平台将会导致一直使用默认的随机的设备信息.
     */
    @ConfigurationDsl
    @JvmOverloads
    fun fileBasedDeviceInfo(filename: String = "device.json") {
        deviceInfo = getFileBasedDeviceInfoSupplier(filename)
    }

    /**
     * 使用当前协程的 [coroutineContext] 作为 [parentCoroutineContext]
     *
     * 用例:
     * ```
     * coroutineScope {
     *   val bot = Bot(...)
     *   bot.login()
     * } // coroutineScope 会等待 Bot 退出
     * ```
     */
    @ConfigurationDsl
    suspend inline fun inheritCoroutineContext() {
        parentCoroutineContext = coroutineContext
    }

    @DslMarker
    annotation class ConfigurationDsl

    @SinceMirai("1.0.0")
    fun copy(): BotConfiguration {
        @OptIn(MiraiExperimentalAPI::class)
        return BotConfiguration().also { new ->
            new.botLoggerSupplier = botLoggerSupplier
            new.networkLoggerSupplier = networkLoggerSupplier
            new.deviceInfo = deviceInfo
            new.parentCoroutineContext = parentCoroutineContext
            new.heartbeatPeriodMillis = heartbeatPeriodMillis
            new.heartbeatTimeoutMillis = heartbeatTimeoutMillis
            new.firstReconnectDelayMillis = firstReconnectDelayMillis
            new.reconnectPeriodMillis = reconnectPeriodMillis
            new.reconnectionRetryTimes = reconnectionRetryTimes
            new.loginSolver = loginSolver
            new.protocol = protocol
            new.fileCacheStrategy = fileCacheStrategy
        }
    }
}

@OptIn(ExperimentalMultiplatform::class)
internal expect fun getFileBasedDeviceInfoSupplier(filename: String): ((Context) -> DeviceInfo)?