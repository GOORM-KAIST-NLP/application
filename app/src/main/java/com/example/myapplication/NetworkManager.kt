package com.example.myapplication

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

data class HelloWorld(
    val text: String
)

data class STTResponse(
    @SerializedName("original_text") val originalText: String?,
    @SerializedName("trans_text") val transText: String?,
)

data class OCRResponse(
    @SerializedName("url") val url: String?,
    @SerializedName("text") val texts: List<STTResponse>?
)

interface LocalTestApi {
    @GET("/")
    suspend fun getHelloWorld(): HelloWorld

    @Multipart
    @POST("/images")
    suspend fun uploadImages(@Part parts: List<MultipartBody.Part>): OCRResponse

    @Multipart
    @POST("/audios")
    suspend fun uploadAudios(@Part parts: List<MultipartBody.Part>): STTResponse
}

interface OCRApi {
    @Multipart
    @POST("/api")
    suspend fun uploadImages(@Part parts: List<MultipartBody.Part>): OCRResponse
}

interface STTApi {
    @Multipart
    @POST("/")
    suspend fun uploadAudios(@Part parts: List<MultipartBody.Part>): STTResponse
}

object NetworkManager {
    const val IMAGE_PATH = "http://34.64.107.143:5000/static/"

    val testApi = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5000/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LocalTestApi::class.java)

    val sttApi = Retrofit.Builder()
        .baseUrl("http://34.125.63.181:5000/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
                .build()
        )
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addConverterFactory(GsonConverterFactory.create(
            GsonBuilder().apply {
                setLenient()
            }.create()
        ))
        .build()
        .create(STTApi::class.java)

    val ocrApi = Retrofit.Builder()
        .baseUrl("http://34.64.107.143:5000/")
        .client(
            OkHttpClient.Builder()
                .apply {
                    connectTimeout(60, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(60, TimeUnit.SECONDS)
                }
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
                .build()
        )
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addConverterFactory(GsonConverterFactory.create(
            GsonBuilder().apply {
                setLenient()
            }.create()
        ))
        .build()
        .create(OCRApi::class.java)
}

class NullOnEmptyConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val delegate: Converter<ResponseBody, *> =
            retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
        return Converter { body -> if (body.contentLength() == 0L) null else delegate.convert(body) }
    }
}