package com.example.i18n

enum class AppLanguage(val code: String, val displayName: String, val speechLocale: String) {
    TURKISH("tr", "Türkçe", "tr-TR"),
    ENGLISH("en", "English", "en-US"),
    GERMAN("de", "Deutsch", "de-DE"),
    SPANISH("es", "Español", "es-ES"),
    FRENCH("fr", "Français", "fr-FR")
}

object AppStrings {
    fun get(key: String, lang: AppLanguage): String {
        val map = when (lang) {
            AppLanguage.TURKISH -> turkishMap
            AppLanguage.ENGLISH -> englishMap
            AppLanguage.GERMAN -> germanMap
            AppLanguage.SPANISH -> spanishMap
            AppLanguage.FRENCH -> frenchMap
        }
        return map[key] ?: turkishMap[key] ?: key
    }

    private val turkishMap = mapOf(
        "app_title" to "SesYazıcı AI",
        "tab_notes" to "Kayıtlar",
        "tab_tasks" to "Görevler",
        "tab_cloud" to "Bulut & Senk",
        "tab_settings" to "Ayarlar",
        "search_hint" to "Transkript veya notlarda ara...",
        "all_folders" to "Tümü",
        "favorites" to "Favoriler",
        "new_recording" to "Canlı Kayıt",
        "call_capture" to "Canlı Görüşme Yakala",
        "call_capture_desc" to "Zoom, Meet, WhatsApp ve telefon görüşmelerini canlı transkribe et",
        "import_audio" to "Ses Dosyası Yükle",
        "start_recording" to "Kayda Başla",
        "recording" to "Kayıt Yapılıyor...",
        "paused" to "Duraklatıldı",
        "pause" to "Duraklat",
        "resume" to "Devam Et",
        "finish" to "Bitir ve Kaydet",
        "switch_speaker" to "Konuşmacı Değiştir",
        "speakers" to "Konuşmacılar",
        "rename_speaker" to "Konuşmacıyı Yeniden Adlandır",
        "ai_summary" to "Yapay Zeka Özeti",
        "generate_summary" to "Özet Oluştur",
        "export" to "Dışa Aktar",
        "export_pdf" to "PDF Olarak Aktar",
        "export_docx" to "Word (DOCX) Olarak Aktar",
        "export_txt" to "Düz Metin (TXT)",
        "export_srt" to "Altyazı Dosyası (SRT)",
        "listen_tts" to "Metni Sesli Dinle",
        "stop_tts" to "Okumayı Durdur",
        "extract_tasks" to "Toplantıdan Görev Çıkar",
        "cloud_sync_now" to "Şimdi Senkronize Et",
        "cloud_synced" to "Bulutla Senkronize",
        "offline_ready" to "Çevrimdışı Çalışma Hazır",
        "dark_mode" to "Karanlık Mod",
        "theme_system" to "Sistem",
        "theme_light" to "Açık",
        "theme_dark" to "Koyu",
        "language" to "Uygulama Dili",
        "recognition_lang" to "Ses Tanıma Dili",
        "tags" to "Etiketler",
        "add_tag" to "Etiket Ekle",
        "new_folder" to "Yeni Klasör",
        "delete_confirm" to "Silmek istediğinize emin misiniz?",
        "empty_notes" to "Henüz kayıt bulunmuyor. Canlı ses kaydı başlatın veya bir ses dosyası yükleyin.",
        "duration" to "Süre",
        "date" to "Tarih",
        "tasks_title" to "Entegre Görev Yöneticisi",
        "add_task" to "Yeni Görev",
        "task_priority" to "Öncelik",
        "task_due" to "Bitiş Tarihi",
        "has_reminder" to "Hatırlatıcı Bildirim",
        "no_tasks" to "Henüz atanmış bir görev yok."
    )

    private val englishMap = mapOf(
        "app_title" to "VoxScribe AI",
        "tab_notes" to "Transcripts",
        "tab_tasks" to "Tasks",
        "tab_cloud" to "Cloud & Sync",
        "tab_settings" to "Settings",
        "search_hint" to "Search in transcripts or notes...",
        "all_folders" to "All",
        "favorites" to "Favorites",
        "new_recording" to "Live Record",
        "import_audio" to "Import Audio",
        "start_recording" to "Start Recording",
        "recording" to "Recording...",
        "paused" to "Paused",
        "pause" to "Pause",
        "resume" to "Resume",
        "finish" to "Finish & Save",
        "switch_speaker" to "Switch Speaker",
        "speakers" to "Speakers",
        "rename_speaker" to "Rename Speaker",
        "ai_summary" to "AI Summary",
        "generate_summary" to "Generate Summary",
        "export" to "Export",
        "export_pdf" to "Export as PDF",
        "export_docx" to "Export as Word (DOCX)",
        "export_txt" to "Plain Text (TXT)",
        "export_srt" to "Subtitles (SRT)",
        "listen_tts" to "Read Aloud (TTS)",
        "stop_tts" to "Stop Reading",
        "extract_tasks" to "Extract Tasks from Audio",
        "cloud_sync_now" to "Sync Now",
        "cloud_synced" to "Cloud Synchronized",
        "offline_ready" to "Offline Ready",
        "dark_mode" to "Dark Mode",
        "theme_system" to "System",
        "theme_light" to "Light",
        "theme_dark" to "Dark",
        "language" to "App Language",
        "recognition_lang" to "Speech Recognition Language",
        "tags" to "Tags",
        "add_tag" to "Add Tag",
        "new_folder" to "New Folder",
        "delete_confirm" to "Are you sure you want to delete?",
        "empty_notes" to "No recordings yet. Start a live recording or import an audio file.",
        "duration" to "Duration",
        "date" to "Date",
        "tasks_title" to "Integrated Task Manager",
        "add_task" to "New Task",
        "task_priority" to "Priority",
        "task_due" to "Due Date",
        "has_reminder" to "Reminder Notification",
        "no_tasks" to "No tasks yet."
    )

    private val germanMap = mapOf(
        "app_title" to "VoxScribe AI",
        "tab_notes" to "Aufnahmen",
        "tab_tasks" to "Aufgaben",
        "tab_cloud" to "Cloud & Sync",
        "tab_settings" to "Einstellungen",
        "search_hint" to "Transkripte durchsuchen...",
        "all_folders" to "Alle",
        "favorites" to "Favoriten",
        "new_recording" to "Live-Aufnahme",
        "import_audio" to "Audio importieren",
        "ai_summary" to "KI-Zusammenfassung",
        "export_pdf" to "Als PDF exportieren",
        "export_docx" to "Als Word (DOCX) exportieren",
        "listen_tts" to "Vorlesen lassen",
        "cloud_sync_now" to "Jetzt synchronisieren",
        "dark_mode" to "Dunkelmodus"
    )

    private val spanishMap = mapOf(
        "app_title" to "VoxScribe AI",
        "tab_notes" to "Transcripciones",
        "tab_tasks" to "Tareas",
        "tab_cloud" to "Nube y Sinc.",
        "tab_settings" to "Ajustes",
        "search_hint" to "Buscar en notas...",
        "all_folders" to "Todos",
        "favorites" to "Favoritos",
        "new_recording" to "Grabar en vivo",
        "import_audio" to "Importar audio",
        "ai_summary" to "Resumen IA",
        "export_pdf" to "Exportar como PDF",
        "export_docx" to "Exportar como DOCX",
        "listen_tts" to "Escuchar texto",
        "cloud_sync_now" to "Sincronizar ahora",
        "dark_mode" to "Modo oscuro"
    )

    private val frenchMap = mapOf(
        "app_title" to "VoxScribe AI",
        "tab_notes" to "Enregistrements",
        "tab_tasks" to "Tâches",
        "tab_cloud" to "Cloud & Synchro",
        "tab_settings" to "Paramètres",
        "search_hint" to "Rechercher...",
        "all_folders" to "Tous",
        "favorites" to "Favoris",
        "new_recording" to "Enregistrer",
        "import_audio" to "Importer audio",
        "ai_summary" to "Résumé IA",
        "export_pdf" to "Exporter en PDF",
        "export_docx" to "Exporter en DOCX",
        "listen_tts" to "Lire à haute voix",
        "cloud_sync_now" to "Synchroniser maintenant",
        "dark_mode" to "Mode sombre"
    )
}
