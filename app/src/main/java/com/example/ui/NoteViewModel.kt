package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Note
import com.example.data.NoteRepository
import com.example.ui.sound.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val sharedPrefs: SharedPreferences = application.getSharedPreferences("atomic_headaches_prefs", Context.MODE_PRIVATE)

    // Notes collected reactively from DB
    val notes: StateFlow<List<Note>>

    // Google Authentication & Synchronization fields
    val authCompleted = mutableStateOf(false)
    val authSkipped = mutableStateOf(false)
    val userEmail = mutableStateOf<String?>(null)
    val userDisplayName = mutableStateOf<String?>(null)
    val userPhotoUrl = mutableStateOf<String?>(null)
    val syncStatus = mutableStateOf("")

    // Book reading view toggle
    val bookReaderOpen = mutableStateOf(false)

    // UI state flags reflecting original React app
    val onboardingDone = mutableStateOf(false)
    val showOnboarding = mutableStateOf(true)
    val composerOpen = mutableStateOf(false)
    val detailOpenId = mutableStateOf<String?>(null)
    val focusMode = mutableStateOf(false)
    val search = mutableStateOf("")
    val filter = mutableStateOf("All")
    val syncPulse = mutableStateOf(0)
    val draft = mutableStateOf<Note?>(null)
    val editingId = mutableStateOf<String?>(null)
    val analyticsOpen = mutableStateOf(false)
    val audioState = mutableStateOf(false)
    val walkingDetected = mutableStateOf(false)

    // Audio recording & playback states
    val isRecording = mutableStateOf(false)
    val recordingDuration = mutableStateOf(0)
    val isPlayingAudio = mutableStateOf(false)
    val playingAudioPath = mutableStateOf<String?>(null)
    val currentRecordingPath = mutableStateOf<String?>(null)
    val recordingPath = currentRecordingPath
    val amplitudeList = androidx.compose.runtime.mutableStateListOf<Float>()

    private var mediaRecorder: android.media.MediaRecorder? = null
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var recordingTimer: java.util.Timer? = null

    init {
        val database = AppDatabase.getDatabase(application)
        val noteDao = database.noteDao()
        repository = NoteRepository(noteDao)

        // Bind DB flow
        notes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Read persist states
        val savedOnboarding = sharedPrefs.getBoolean("onboarding_done", false)
        onboardingDone.value = savedOnboarding
        showOnboarding.value = !savedOnboarding

        val savedAudio = sharedPrefs.getBoolean("audio_state", false)
        audioState.value = savedAudio
        SoundManager.setAudioEnabled(savedAudio)

        // Read Authentication states
        authCompleted.value = sharedPrefs.getBoolean("auth_completed", false)
        authSkipped.value = sharedPrefs.getBoolean("auth_skipped", false)
        userEmail.value = sharedPrefs.getString("auth_email", null)
        userDisplayName.value = sharedPrefs.getString("auth_name", null)
        userPhotoUrl.value = sharedPrefs.getString("auth_photo", null)
    }

    fun playSound(freq: Float = 800f, duration: Float = 0.15f) {
        SoundManager.playSoundAsync(freq, duration)
    }

    fun toggleGlobalAudio() {
        val nextVal = !audioState.value
        audioState.value = nextVal
        SoundManager.setAudioEnabled(nextVal)
        sharedPrefs.edit().putBoolean("audio_state", nextVal).apply()
        if (nextVal) {
            playSound(700f, 0.15f)
        }
    }

    fun completeOnboarding() {
        onboardingDone.value = true
        showOnboarding.value = false
        sharedPrefs.edit().putBoolean("onboarding_done", true).apply()
        playSound(1000f, 0.25f)
        
        // If notes is empty, open a prefilled draft automatically!
        if (notes.value.isEmpty()) {
            openNewNote("Home")
        }
    }

    fun completeOnboardingWithNote(title: String, body: String) {
        if (title.isNotEmpty() || body.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val firstNote = Note(
                id = "n_${now}_${UUID.randomUUID().toString().substring(0, 6)}",
                title = title.trim(),
                body = body.trim(),
                createdAt = now,
                updatedAt = now,
                locationLabel = "Home",
                mood = "Calm",
                preset = "",
                deviceLocation = ""
            )
            viewModelScope.launch {
                repository.insert(firstNote)
                syncPulse.value += 1
            }
        }
        onboardingDone.value = true
        showOnboarding.value = false
        sharedPrefs.edit().putBoolean("onboarding_done", true).apply()
        playSound(1000f, 0.25f)
    }

    fun openNewNote(locationLabel: String = "Home") {
        playSound(900f, 0.15f)
        editingId.value = null
        val now = System.currentTimeMillis()
        draft.value = Note(
            id = "n_${now}_${UUID.randomUUID().toString().substring(0, 6)}",
            title = "",
            body = "",
            createdAt = now,
            updatedAt = now,
            locationLabel = locationLabel,
            mood = "",
            preset = "",
            deviceLocation = ""
        )
        composerOpen.value = true
        focusMode.value = true
        // Clear previous audio path and states
        recordingPath.value = null
    }

    fun openEdit(note: Note) {
        playSound(850f, 0.15f)
        editingId.value = note.id
        draft.value = note.copy(updatedAt = System.currentTimeMillis())
        composerOpen.value = true
        detailOpenId.value = null
        focusMode.value = true
        recordingPath.value = note.audioPath
    }

    fun saveDraft() {
        val currentDraft = draft.value ?: return
        viewModelScope.launch {
            var finalDraft = currentDraft.copy(
                title = currentDraft.title.trim(),
                body = currentDraft.body.trim(),
                updatedAt = System.currentTimeMillis()
            )
            
            // If title is REALLY empty, auto-generate a snippet!
            if (finalDraft.title.isEmpty() || finalDraft.title == "Untitled" || finalDraft.title == "Drawing" || finalDraft.title == "Voice Log Entry") {
                val words = finalDraft.body.split("\\s+".toRegex()).take(5)
                val autoHeadline = if (words.isNotEmpty() && words[0].isNotEmpty()) {
                    words.joinToString(" ") + (if (finalDraft.body.split("\\s+".toRegex()).size > 5) "..." else "")
                } else {
                    "Voice Log Entry"
                }
                finalDraft = finalDraft.copy(title = autoHeadline)
            }
            
            if (finalDraft.mood.isEmpty()) {
                finalDraft = finalDraft.copy(mood = "Reflective")
            }
            if (finalDraft.locationLabel.isEmpty()) {
                finalDraft = finalDraft.copy(locationLabel = "Home")
            }

            repository.insert(finalDraft)
            
            // Increment syncPulse to render visual feedback
            syncPulse.value += 1
            
            composerOpen.value = false
            draft.value = null
            editingId.value = null
            focusMode.value = false
            playSound(1100f, 0.25f)
        }
    }

    fun useDeviceLocation() {
        val currentDraft = draft.value ?: return
        playSound(750f, 0.15f)
        // Set location label to Outside and attach Simulated coordinates
        draft.value = currentDraft.copy(
            locationLabel = "Outside",
            deviceLocation = "GL-45.109, LN-122.680"
        )
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
            detailOpenId.value = null
            composerOpen.value = false
            draft.value = null
            editingId.value = null
            focusMode.value = false
            playSound(300f, 0.3f)
        }
    }

    fun updateDraftTitle(newTitle: String) {
        val current = draft.value ?: return
        draft.value = current.copy(title = newTitle)
    }

    fun updateDraftBody(newBody: String) {
        val current = draft.value ?: return
        draft.value = current.copy(body = newBody)
    }

    fun updateDraftLocation(newLoc: String) {
        val current = draft.value ?: return
        draft.value = current.copy(locationLabel = newLoc)
    }

    fun updateDraftMood(newMood: String) {
        val current = draft.value ?: return
        draft.value = current.copy(mood = newMood)
    }

    fun updateDraftPreset(presetLabel: String, seedTitle: String, hintBody: String) {
        val current = draft.value ?: return
        draft.value = current.copy(
            preset = presetLabel,
            title = seedTitle,
            body = if (current.body.isEmpty()) hintBody else current.body
        )
    }

    // Audio recording & playback systems
    fun startRecording(noteId: String, context: Context) {
        if (isRecording.value) return
        playSound(750f, 0.15f)
        try {
            val audioFile = File(context.filesDir, "audio_note_${noteId}.mp4")
            recordingPath.value = audioFile.absolutePath
            
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(audioFile.absolutePath)
            recorder.prepare()
            recorder.start()
            
            mediaRecorder = recorder
            isRecording.value = true
            recordingDuration.value = 0
            amplitudeList.clear()
            
            var elapsedMs = 0L
            viewModelScope.launch {
                var elapsedSilenceMs = 0L
                while (isRecording.value) {
                    kotlinx.coroutines.delay(200)
                    if (!isRecording.value) break
                    elapsedMs += 200
                    recordingDuration.value = (elapsedMs / 1000).toInt()
                    
                    val amp = try { mediaRecorder?.maxAmplitude ?: 0 } catch(e: Exception) { 0 }
                    amplitudeList.add(amp.toFloat())
                    if (amplitudeList.size > 40) amplitudeList.removeAt(0)
                    
                    // amp < 1800 means quiet/silence (highly accurate speech detection floor)
                    if (amp < 1800) {
                        elapsedSilenceMs += 200
                        if (elapsedSilenceMs >= 5000) { // Wait exactly 5 seconds of silence before auto-saving
                            stopRecordingAndSave(noteId)
                            break
                        }
                    } else {
                        elapsedSilenceMs = 0
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecordingAndSave(noteId: String) {
        if (!isRecording.value) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording.value = false
        }
        playSound(650f, 0.2f)
        
        // Update the current draft with the audioPath!
        val currentDraft = draft.value
        val path = recordingPath.value
        if (currentDraft != null && currentDraft.id == noteId && path != null) {
            draft.value = currentDraft.copy(audioPath = path)
        }
    }

    fun playAudio(path: String) {
        try {
            stopAudio()
            val player = android.media.MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener {
                    isPlayingAudio.value = false
                    playingAudioPath.value = null
                    stopAudio()
                }
            }
            mediaPlayer = player
            playingAudioPath.value = path
            isPlayingAudio.value = true
            playSound(700f, 0.12f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            isPlayingAudio.value = false
            playingAudioPath.value = null
        }
    }

    fun toggleAudioPlay(path: String) {
        if (isPlayingAudio.value && playingAudioPath.value == path) {
            stopAudio()
        } else {
            playAudio(path)
        }
    }

    fun deleteAudioFile() {
        val path = draft.value?.audioPath ?: recordingPath.value ?: return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        draft.value = draft.value?.copy(audioPath = null)
        recordingPath.value = null
        playSound(350f, 0.15f)
    }

    // Date formatting matching React implementation
    fun formatTime(time: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(time))
    }

    fun formatDay(time: Long): String {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        return sdf.format(Date(time))
    }

    fun groupLabel(time: Long): String {
        val now = Calendar.getInstance()
        val noteCal = Calendar.getInstance().apply { timeInMillis = time }

        val diffDays = ((atStartOfDay(now.timeInMillis) - atStartOfDay(noteCal.timeInMillis)) / 86400000).toInt()

        return when (diffDays) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> {
                val sdf = SimpleDateFormat("MMMM d", Locale.getDefault())
                sdf.format(Date(time))
            }
        }
    }

    fun getDaysDiff(time: Long): Int {
        val now = Calendar.getInstance()
        val noteCal = Calendar.getInstance().apply { timeInMillis = time }
        return ((atStartOfDay(now.timeInMillis) - atStartOfDay(noteCal.timeInMillis)) / 86400000).toInt()
    }

    private fun atStartOfDay(timeMs: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Google Authentication actions
    fun completeAuth(email: String, name: String, photo: String) {
        authCompleted.value = true
        authSkipped.value = false
        userEmail.value = email
        userDisplayName.value = name
        userPhotoUrl.value = photo

        sharedPrefs.edit().apply {
            putBoolean("auth_completed", true)
            putBoolean("auth_skipped", false)
            putString("auth_email", email)
            putString("auth_name", name)
            putString("auth_photo", photo)
        }.apply()
    }

    fun skipAuth() {
        authCompleted.value = false
        authSkipped.value = true
        userEmail.value = null
        userDisplayName.value = null
        userPhotoUrl.value = null

        sharedPrefs.edit().apply {
            putBoolean("auth_completed", false)
            putBoolean("auth_skipped", true)
            remove("auth_email")
            remove("auth_name")
            remove("auth_photo")
        }.apply()
    }

    fun logOut(context: Context) {
        authCompleted.value = false
        authSkipped.value = false
        userEmail.value = null
        userDisplayName.value = null
        userPhotoUrl.value = null
        syncStatus.value = ""

        sharedPrefs.edit().apply {
            putBoolean("auth_completed", false)
            putBoolean("auth_skipped", false)
            remove("auth_email")
            remove("auth_name")
            remove("auth_photo")
        }.apply()
        
        // Log out Google Sign In Client
        try {
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso).signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sync actions
    fun performDriveSync(context: Context, onAuthResolutionIntent: (android.content.Intent) -> Unit = {}) {
        val email = userEmail.value ?: return
        viewModelScope.launch {
            syncStatus.value = "Starting cloud sync..."
            val fullyMerged = com.example.ui.sync.SyncManager.syncWithGoogleDrive(
                context = context,
                accountEmail = email,
                localNotes = notes.value,
                onAuthResolutionNeeded = onAuthResolutionIntent,
                onStatusUpdate = { syncStatus.value = it }
            )
            if (fullyMerged != null) {
                // Bulk insert/replace them in Room database
                fullyMerged.forEach { note ->
                    repository.insert(note)
                }
                syncStatus.value = "Google Drive synchronization complete."
                playSound(1000f, 0.2f)
            }
        }
    }

    fun importBookFromFile(context: Context, uri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        val imported = com.example.ui.sync.SyncManager.importJournalBook(context, uri)
        if (imported != null) {
            viewModelScope.launch {
                imported.forEach { note ->
                    repository.insert(note)
                }
                onComplete(true)
                playSound(1000f, 0.25f)
            }
        } else {
            onComplete(false)
            playSound(300f, 0.3f)
        }
    }

    fun exportBookToFile(context: Context, onResult: (String?) -> Unit) {
        com.example.ui.sync.SyncManager.exportJournalBook(
            context = context,
            notes = notes.value,
            onSuccess = { onResult(null) },
            onError = { err -> onResult(err) }
        )
    }

    override fun onCleared() {
        super.onCleared()
        try {
            mediaRecorder?.release()
            mediaRecorder = null
        } catch(e: Exception){}
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch(e: Exception){}
    }
}
