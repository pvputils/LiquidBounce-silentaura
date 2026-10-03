package net.ccbluex.liquidbounce.api.core

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer

/** Download progress without depending on the removed embedded browser library. */
class OkHttpProgressInterceptorTodoAi(private val listener: ProgressListener) : Interceptor {
    fun interface ProgressListener {
        fun update(bytesRead: Long, contentLength: Long, done: Boolean)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body
        return response.newBuilder().body(object : ResponseBody() {
            private val stream = object : ForwardingSource(body.source()) {
                private var total = 0L
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val read = super.read(sink, byteCount)
                    if (read > 0) total += read
                    listener.update(total, body.contentLength(), read == -1L)
                    return read
                }
            }.buffer()
            override fun contentType() = body.contentType()
            override fun contentLength() = body.contentLength()
            override fun source() = stream
        }).build()
    }
}
