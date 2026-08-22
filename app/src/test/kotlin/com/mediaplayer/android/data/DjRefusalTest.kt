package com.mediaplayer.android.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Il DJ rifiuta un giro forzato in due modi che l'utente deve poter
 * distinguere: 409 "ce n'e' gia' uno in corso" e 429 "aspetta ancora tot".
 * Retrofit li consegna entrambi come HttpException con un corpo che nessuno
 * legge se non lo si legge di proposito — e la spec chiede espressamente di
 * mostrare l'attesa residua, non di limitarsi a fallire.
 */
class DjRefusalTest {

    private fun httpError(code: Int, body: String, headers: Map<String, String> = emptyMap()):
        HttpException {
        val request = Request.Builder().url("http://localhost/api/dj/run").build()
        val rawBuilder = okhttp3.Response.Builder()
            .code(code)
            .message("error")
            .protocol(Protocol.HTTP_1_1)
            .request(request)
        headers.forEach { (k, v) -> rawBuilder.header(k, v) }
        val errorBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(errorBody, rawBuilder.build()))
    }

    @Test
    fun `a 409 carries the sentence the server wrote`() {
        val refusal = DjRefusal.of(
            httpError(409, """{"error":"Un giro e' gia' in corso per questo utente."}"""))

        assertEquals(409, refusal?.status)
        assertEquals("Un giro e' gia' in corso per questo utente.", refusal?.message)
        assertNull(refusal?.retryAfterSeconds)
    }

    @Test
    fun `a 429 carries the remaining wait from the header`() {
        val refusal = DjRefusal.of(httpError(
            429,
            """{"error":"Troppo presto per un altro giro forzato.","retryAfterSeconds":240}""",
            mapOf("Retry-After" to "240")))

        assertEquals(240L, refusal?.retryAfterSeconds)
    }

    @Test
    fun `a 429 without the header falls back to the body`() {
        // Un proxy che rimuove Retry-After non deve trasformare "aspetta
        // quattro minuti" in "qualcosa e' andato storto".
        val refusal = DjRefusal.of(httpError(
            429, """{"error":"Troppo presto.","retryAfterSeconds":90}"""))

        assertEquals(90L, refusal?.retryAfterSeconds)
    }

    @Test
    fun `a body that is not JSON does not become a second failure`() {
        val refusal = DjRefusal.of(httpError(500, "<html>gateway</html>"))

        assertEquals(500, refusal?.status)
        assertNull(refusal?.message)
    }

    @Test
    fun `a network failure is not a refusal`() {
        assertNull(DjRefusal.of(IOException("offline")))
    }
}
