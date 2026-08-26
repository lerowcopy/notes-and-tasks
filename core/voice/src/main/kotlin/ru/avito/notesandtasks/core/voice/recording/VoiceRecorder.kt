package ru.avito.notesandtasks.core.voice.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.avito.notesandtasks.core.common.result.OperationResult

private const val VOICE_RECORDINGS_DIRECTORY = "voice_recordings"
private const val VOICE_FILE_PREFIX = "voice_"
private const val VOICE_FILE_SUFFIX = ".ogg"
private const val SAMPLE_RATE_HERTZ = 16_000
private const val CHANNEL_COUNT = 1
private const val MAX_SYNC_RECOGNITION_DURATION_MILLIS = 60_000
private const val MAX_SYNC_RECOGNITION_FILE_BYTES = 2_000_000L

sealed interface RecordingState {
    data object Idle : RecordingState

    data class Recording(
        val outputPath: String,
    ) : RecordingState

    data class Error(
        val cause: Throwable,
    ) : RecordingState
}

class VoiceRecorder(
    private val context: Context,
) {
    private val mutableRecordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val recordingState: StateFlow<RecordingState> = mutableRecordingState.asStateFlow()

    fun start(): OperationResult<Unit> {
        if (recorder != null) {
            return fail(VoiceRecorderException.RecordingAlreadyStarted)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return fail(VoiceRecorderException.OggOpusIsNotSupported)
        }

        val newOutputFile = createOutputFile()
        return runCatching {
            createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.OGG)
                setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                setAudioSamplingRate(SAMPLE_RATE_HERTZ)
                setAudioChannels(CHANNEL_COUNT)
                setMaxDuration(MAX_SYNC_RECOGNITION_DURATION_MILLIS)
                setMaxFileSize(MAX_SYNC_RECOGNITION_FILE_BYTES)
                setOutputFile(newOutputFile.absolutePath)
                prepare()
                start()
            }
        }.fold(
            onSuccess = { preparedRecorder ->
                recorder = preparedRecorder
                outputFile = newOutputFile
                mutableRecordingState.value = RecordingState.Recording(newOutputFile.absolutePath)
                OperationResult.Success(Unit)
            },
            onFailure = { cause ->
                newOutputFile.delete()
                fail(cause)
            },
        )
    }

    fun stop(): OperationResult<String> {
        val activeRecorder = recorder ?: return fail(VoiceRecorderException.NoActiveRecording)
        val activeOutputFile = outputFile ?: return fail(VoiceRecorderException.NoActiveRecording)
        recorder = null
        outputFile = null

        return runCatching {
            activeRecorder.stop()
            activeOutputFile.absolutePath
        }.fold(
            onSuccess = { outputPath ->
                activeRecorder.releaseSafely()
                mutableRecordingState.value = RecordingState.Idle
                OperationResult.Success(outputPath)
            },
            onFailure = { cause ->
                activeRecorder.releaseSafely()
                activeOutputFile.delete()
                fail(cause)
            },
        )
    }

    fun release() {
        recorder?.releaseSafely()
        recorder = null
        outputFile?.delete()
        outputFile = null
        mutableRecordingState.value = RecordingState.Idle
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder = MediaRecorder()

    private fun createOutputFile(): File {
        val outputDirectory = File(context.cacheDir, VOICE_RECORDINGS_DIRECTORY).apply {
            mkdirs()
        }
        return File.createTempFile(VOICE_FILE_PREFIX, VOICE_FILE_SUFFIX, outputDirectory)
    }

    private fun <T> fail(cause: Throwable): OperationResult<T> {
        mutableRecordingState.value = RecordingState.Error(cause)
        return OperationResult.Error(cause = cause)
    }
}

private fun MediaRecorder.releaseSafely() {
    runCatching(::release)
}

sealed class VoiceRecorderException : IllegalStateException() {
    data object RecordingAlreadyStarted : VoiceRecorderException()
    data object NoActiveRecording : VoiceRecorderException()
    data object OggOpusIsNotSupported : VoiceRecorderException()
}
