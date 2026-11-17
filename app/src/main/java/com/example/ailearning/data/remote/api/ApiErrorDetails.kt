package com.example.ailearning.data.remote.api

import com.example.ailearning.domain.model.AiProviderCatalog
import com.google.gson.JsonParser
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * API エラーを原因調査しやすい形式に整形する。
 * HTTP ステータス・プロバイダ・モデル・URL・レスポンス本文・例外種別を含める。
 */
object ApiErrorDetails {

    fun http(
        action: String,
        provider: String,
        model: String,
        endpoint: String,
        code: Int,
        body: String?
    ): String = buildString {
        append(action).append("に失敗しました\n")
        append("HTTP ").append(code)
        statusHint(code)?.let { append("（").append(it).append("）") }
        append("\n")
        appendCommon(provider, model, endpoint)
        extractApiMessage(body)?.let { append("\n理由: ").append(it) }
        if (!body.isNullOrBlank()) {
            append("\nレスポンス: ").append(body.trim().take(600))
        }
    }

    fun exception(
        action: String,
        provider: String,
        model: String,
        endpoint: String,
        error: Throwable
    ): String = buildString {
        append(action).append("に失敗しました\n")
        appendCommon(provider, model, endpoint)
        append("\n種類: ").append(error.javaClass.simpleName)
        append("\n詳細: ").append(exceptionReason(error))
        val causeMessage = error.cause?.message
        if (!causeMessage.isNullOrBlank() && causeMessage != error.message) {
            append("\n原因: ").append(causeMessage)
        }
    }

    private fun StringBuilder.appendCommon(provider: String, model: String, endpoint: String) {
        append("プロバイダ: ").append(AiProviderCatalog.label(provider))
        if (model.isNotBlank()) append("\nモデル: ").append(model)
        if (endpoint.isNotBlank()) append("\nURL: ").append(endpoint)
    }

    private fun exceptionReason(error: Throwable): String = when (error) {
        is UnknownHostException ->
            "ホスト名を解決できません。ベースURLとネットワーク接続を確認してください。"
        is SocketTimeoutException ->
            "接続がタイムアウトしました。ネットワーク状況かプロバイダの混雑を確認してください。"
        is ConnectException ->
            "サーバーに接続できません。URL・ポート・ローカルサーバーの起動状態を確認してください。"
        is SSLException ->
            "TLS/SSL 接続に失敗しました。URL の https / 証明書を確認してください。"
        else ->
            error.message?.takeIf { it.isNotBlank() } ?: error.toString()
    }

    private fun statusHint(code: Int): String? = when (code) {
        400 -> "リクエストが不正"
        401 -> "APIキーが無効、または認証に失敗"
        402 -> "残高不足"
        403 -> "アクセス権限なし"
        404 -> "エンドポイントまたはモデルが見つからない"
        405 -> "メソッド非対応"
        408 -> "リクエストタイムアウト"
        413 -> "リクエストが大きすぎる"
        422 -> "入力内容が不正"
        429 -> "レート制限、または利用枠の上限"
        500 -> "プロバイダのサーバーエラー"
        502 -> "ゲートウェイエラー"
        503 -> "サービス一時停止中"
        504 -> "ゲートウェイタイムアウト"
        else -> null
    }

    /** `{"error":{"message":"..."}}` や `{"message":"..."}` などから理由を抜き出す。 */
    private fun extractApiMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = JsonParser.parseString(body)
            if (!json.isJsonObject) return null
            val obj = json.asJsonObject

            val error = obj.get("error")
            when {
                error != null && error.isJsonObject -> {
                    error.asJsonObject.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: error.asJsonObject.get("code")?.takeIf { it.isJsonPrimitive }?.asString
                }
                error != null && error.isJsonPrimitive -> error.asString
                obj.get("message")?.takeIf { it.isJsonPrimitive } != null ->
                    obj.get("message").asString
                obj.get("detail")?.takeIf { it.isJsonPrimitive } != null ->
                    obj.get("detail").asString
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
