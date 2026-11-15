package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "AiImageRepository"

class AiImageRepository {

    private fun buildModel(aspectRatio: AspectRatio? = null) = Firebase.ai.generativeModel(
        modelName = "gemini-nano-banana-2.1",
        generationConfig = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            if (aspectRatio != null) {
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(aspectRatio)
                    .build()
            }
        }
    )

    private val defaultModel by lazy { buildModel(null) }

    suspend fun generateImage(
        prompt: String,
        aspectRatio: AspectRatio? = null
    ): Result<Bitmap> {
        return try {
            val model = if (aspectRatio == null) defaultModel else buildModel(aspectRatio)
            val bitmap = withTimeout(60_000L) {
                val response = model.generateContent(prompt)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image ?: error("The model did not return an image. Try refining your prompt.")
            }
            Result.success(bitmap)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI image generation timed out", e)
            Result.failure(IllegalStateException("Image generation timed out after 60 seconds. Please try again."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "AI image generation quota exceeded", e)
            Result.failure(IllegalStateException("Rate limit or Firebase Blaze quota exceeded. Please try again later."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI image generation failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "AI image generation failed."))
        }
    }

    suspend fun editImage(
        sourceBitmap: Bitmap,
        instruction: String
    ): Result<Bitmap> {
        return try {
            val bitmap = withTimeout(60_000L) {
                val response = defaultModel.generateContent(
                    content {
                        image(sourceBitmap)
                        text(instruction)
                    }
                )
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image ?: error("The model did not return an edited image.")
            }
            Result.success(bitmap)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI image editing timed out", e)
            Result.failure(IllegalStateException("Image editing timed out after 60 seconds. Please try again."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "AI image editing quota exceeded", e)
            Result.failure(IllegalStateException("Rate limit or Firebase Blaze quota exceeded. Please try again later."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI image editing failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "AI image editing failed."))
        }
    }
}
