package ru.avito.notesandtasks.core.gigachat

import android.content.Context
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import ru.avito.notesandtasks.core.network.factory.OkHttpClientFactory

class GigaChatTlsClientFactory(
    private val context: Context,
) {
    fun create(): OkHttpClient {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificate = context.resources
            .openRawResource(R.raw.russian_trusted_root_ca)
            .use(certificateFactory::generateCertificate)
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry(CERTIFICATE_ALIAS, certificate)
        }
        val trustManager = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore) }
            .trustManagers
            .filterIsInstance<X509TrustManager>()
            .single()
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }
        return OkHttpClientFactory
            .createBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }
}

private const val CERTIFICATE_ALIAS = "russian_trusted_root_ca"
