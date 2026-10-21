package com.example.ui.util

/**
 * Localization dictionary supporting Turkish (tr), English (en), and German (de).
 *
 * Kural 4:
 * Ayarlardan uygulama dili değiştirildiğinde (Türkçe, English, Deutsch)
 * gerçekten tüm ekranlardaki başlıklar, butonlar, durum metinleri ve açıklamalar
 * anında seçilen dile dönüşür.
 */
object Localization {

    fun get(key: String, lang: String = "tr"): String {
        val l = lang.lowercase()
        val isEn = l == "en"
        val isDe = l == "de"

        return when (key) {
            // Navigation
            "nav_record" -> when {
                isEn -> "Record"
                isDe -> "Aufnahme"
                else -> "Kayıt"
            }
            "nav_transcripts" -> when {
                isEn -> "Transcripts"
                isDe -> "Transkripte"
                else -> "Transkript"
            }
            "nav_editor" -> when {
                isEn -> "Editor"
                isDe -> "Editor"
                else -> "Düzenleyici"
            }
            "nav_documents" -> when {
                isEn -> "Documents"
                isDe -> "Dokumente"
                else -> "Dosyalar"
            }
            "nav_feedback" -> when {
                isEn -> "Support"
                isDe -> "Support"
                else -> "Mesaj"
            }
            "nav_settings" -> when {
                isEn -> "Settings"
                isDe -> "Einstellungen"
                else -> "Ayarlar"
            }

            // Record Screen
            "rec_title" -> when {
                isEn -> "Audio Recording Center"
                isDe -> "Audio-Aufnahmezentrum"
                else -> "Ses Kayıt Merkezi"
            }
            "rec_subtitle" -> when {
                isEn -> "Live microphone, other apps & imported audio files"
                isDe -> "Live-Mikrofon, andere Apps & Audiodateien"
                else -> "Canlı mikrofon kaydı, diğer uygulamalar ve harici ses dosyaları"
            }
            "rec_start" -> when {
                isEn -> "Start Recording"
                isDe -> "Aufnahme starten"
                else -> "Kaydı Başlat"
            }
            "rec_stop" -> when {
                isEn -> "Stop Recording"
                isDe -> "Aufnahme stoppen"
                else -> "Kaydı Durdur"
            }
            "rec_pause" -> when {
                isEn -> "Pause"
                isDe -> "Pause"
                else -> "Duraklat"
            }
            "rec_resume" -> when {
                isEn -> "Resume"
                isDe -> "Fortsetzen"
                else -> "Devam Et"
            }
            "rec_mute" -> when {
                isEn -> "Mute Mic"
                isDe -> "Mikrofon stumm"
                else -> "Sessize Al"
            }
            "rec_unmute" -> when {
                isEn -> "Unmute Mic"
                isDe -> "Mikrofon an"
                else -> "Mikrofonu Aç"
            }
            "rec_floating_open" -> when {
                isEn -> "Open Floating Panel"
                isDe -> "Schwebendes Panel öffnen"
                else -> "Yüzen Paneli Aç"
            }
            "rec_floating_close" -> when {
                isEn -> "Close Floating Panel"
                isDe -> "Schwebendes Panel schließen"
                else -> "Yüzen Paneli Kapat"
            }
            "rec_status_idle" -> when {
                isEn -> "Ready to Record"
                isDe -> "Bereit zur Aufnahme"
                else -> "Kayıt Yapılmıyor"
            }
            "rec_status_paused" -> when {
                isEn -> "Recording Paused"
                isDe -> "Aufnahme pausiert"
                else -> "Kayıt Duraklatıldı"
            }
            "rec_status_muted" -> when {
                isEn -> "Recording (Mic Muted)"
                isDe -> "Aufnahme (Stummgeschaltet)"
                else -> "Kayıt Sürüyor (Mikrofon Sessizde)"
            }
            "rec_status_recording" -> when {
                isEn -> "Recording in Mono..."
                isDe -> "Mono-Aufnahme läuft..."
                else -> "Canlı Ses Kaydediliyor..."
            }
            "rec_live_detected" -> when {
                isEn -> "Live Detected Speech:"
                isDe -> "Live erkannte Sprache:"
                else -> "Canlı Algılanan Ses:"
            }
            "rec_import_btn" -> when {
                isEn -> "Upload Audio File"
                isDe -> "Audiodatei hochladen"
                else -> "Harici Ses Dosyası Yükle"
            }
            "rec_import_desc" -> when {
                isEn -> "Auto-converts to mono & transcribes with AI"
                isDe -> "Automatische Mono-Konvertierung & Transkription"
                else -> "Ses dosyası mono sese çevrilip yapay zeka ile metne dönüştürülür"
            }
            "rec_overlay_title" -> when {
                isEn -> "Background & Overlay Service"
                isDe -> "Hintergrund- & Overlay-Dienst"
                else -> "Arka Plan ve Yüzen Panel Servisi"
            }
            "rec_overlay_desc" -> when {
                isEn -> "Continue recording while using Zoom, Phone calls, WhatsApp or other apps"
                isDe -> "Aufnahme während Zoom, Anrufen oder WhatsApp fortsetzen"
                else -> "Zoom, Telefon görüşmesi veya diğer uygulamaların üzerindeyken kayda devam edin"
            }

            // Transcripts Screen
            "trans_title" -> when {
                isEn -> "Speech to Text"
                isDe -> "Sprache zu Text"
                else -> "Transkripsiyon"
            }
            "trans_subtitle" -> when {
                isEn -> "Gemini AI speaker diarization and transcription"
                isDe -> "Gemini AI Sprechererkennung und Transkription"
                else -> "Gemini AI ile konuşmacı ayrımı ve metne dönüştürme"
            }
            "trans_speakers" -> when {
                isEn -> "Speaker Count"
                isDe -> "Sprecheranzahl"
                else -> "Konuşmacı Sayısı"
            }
            "trans_transcribe_btn" -> when {
                isEn -> "Transcribe to Text"
                isDe -> "In Text umwandeln"
                else -> "Metne Dönüştür"
            }
            "trans_search_hint" -> when {
                isEn -> "Search transcript..."
                isDe -> "Im Transkript suchen..."
                else -> "Transkript içinde ara..."
            }
            "trans_no_records" -> when {
                isEn -> "No recordings found"
                isDe -> "Keine Aufnahmen gefunden"
                else -> "Henüz bir ses kaydı bulunmuyor"
            }
            "trans_edit_transcript" -> when {
                isEn -> "Edit in Editor"
                isDe -> "Im Editor bearbeiten"
                else -> "Düzenleyicide Aç"
            }

            // Editor Screen
            "editor_title" -> when {
                isEn -> "Document Editor"
                isDe -> "Dokumenten-Editor"
                else -> "Döküman Düzenleyici"
            }
            "editor_select_doc" -> when {
                isEn -> "Select Document to Edit:"
                isDe -> "Zu bearbeitendes Dokument wählen:"
                else -> "Düzenlenecek Dökümanı Seçin:"
            }
            "editor_save" -> when {
                isEn -> "Save Document"
                isDe -> "Dokument speichern"
                else -> "Metni Kaydet"
            }
            "editor_export_pdf" -> when {
                isEn -> "Export PDF"
                isDe -> "PDF exportieren"
                else -> "PDF Olarak Kaydet"
            }
            "editor_share" -> when {
                isEn -> "Share Text"
                isDe -> "Text teilen"
                else -> "Metni Paylaş"
            }
            "editor_play" -> when {
                isEn -> "Play Audio"
                isDe -> "Audio abspielen"
                else -> "Sesi Dinle"
            }
            "editor_pause" -> when {
                isEn -> "Pause Audio"
                isDe -> "Audio pausieren"
                else -> "Sesi Durdur"
            }
            "editor_rename_speaker" -> when {
                isEn -> "Rename Speaker"
                isDe -> "Sprecher umbenennen"
                else -> "Kişi Etiketini Değiştir"
            }

            // Documents Screen
            "doc_title" -> when {
                isEn -> "Documents & Files"
                isDe -> "Dokumente & Dateien"
                else -> "Dosya Yöneticisi"
            }
            "doc_drive_backup" -> when {
                isEn -> "Backup to Google Drive"
                isDe -> "Auf Google Drive sichern"
                else -> "Google Drive'a Yedekle"
            }
            "doc_share_pdf" -> when {
                isEn -> "Share as PDF"
                isDe -> "Als PDF teilen"
                else -> "PDF Olarak Paylaş"
            }
            "doc_select_all" -> when {
                isEn -> "Select All"
                isDe -> "Alle auswählen"
                else -> "Tümünü Seç"
            }
            "doc_clear_selection" -> when {
                isEn -> "Clear Selection"
                isDe -> "Auswahl aufheben"
                else -> "Seçimi Temizle"
            }
            "doc_search" -> when {
                isEn -> "Search documents..."
                isDe -> "Dokumente durchsuchen..."
                else -> "Dökümanlarda ara..."
            }

            // Settings Screen
            "settings_title" -> when {
                isEn -> "Settings"
                isDe -> "Einstellungen"
                else -> "Ayarlar"
            }
            "settings_lang" -> when {
                isEn -> "Application Language"
                isDe -> "App-Sprache"
                else -> "Uygulama Dili"
            }
            "settings_theme" -> when {
                isEn -> "Theme Appearance"
                isDe -> "Design-Modus"
                else -> "Görünüm Teması"
            }
            "settings_dark" -> when {
                isEn -> "Dark"
                isDe -> "Dunkel"
                else -> "Koyu"
            }
            "settings_light" -> when {
                isEn -> "Light"
                isDe -> "Hell"
                else -> "Açık"
            }
            "settings_system" -> when {
                isEn -> "System"
                isDe -> "System"
                else -> "Sistem"
            }
            "settings_channel_mode" -> when {
                isEn -> "Channel Mode (Recording):"
                isDe -> "Kanalmodus (Aufnahme):"
                else -> "Kanal Modu (Kayıt):"
            }
            "settings_mono_desc" -> when {
                isEn -> "🎙️ Mono (1 Channel - Recommended)"
                isDe -> "🎙️ Mono (1 Kanal - Empfohlen)"
                else -> "🎙️ Mono (1 Kanal - Önerilen)"
            }
            "settings_gemini_title" -> when {
                isEn -> "AI Transcription (Gemini API)"
                isDe -> "AI-Transkription (Gemini API)"
                else -> "AI Transkripsiyon (Gemini API)"
            }
            "settings_gemini_desc" -> when {
                isEn -> "Required for speech-to-text, speaker diarization and meeting minutes"
                isDe -> "Erforderlich für Spracherkennung und Sprechertrennung"
                else -> "Ses kayıtlarını tam metne dökmek ve konuşmacıları ayrıştırmak için gereklidir"
            }
            "settings_gemini_save" -> when {
                isEn -> "Save Key"
                isDe -> "Schlüssel speichern"
                else -> "Kaydet"
            }
            "settings_gemini_help" -> when {
                isEn -> "❓ How to get a free Gemini API Key? (Visual Guide)"
                isDe -> "❓ Wie bekomme ich einen kostenlosen Gemini API Key? (Anleitung)"
                else -> "❓ Ücretsiz Gemini API Anahtarı Nasıl Alınır? (Resimli Rehber)"
            }
            "settings_version_title" -> when {
                isEn -> "Application Version"
                isDe -> "App-Version"
                else -> "Uygulama Sürümü"
            }
            "settings_check_update" -> when {
                isEn -> "Check for Updates"
                isDe -> "Nach Updates suchen"
                else -> "Güncellemeleri Denetle"
            }
            "settings_update_available" -> when {
                isEn -> "New Update Available!"
                isDe -> "Neues Update verfügbar!"
                else -> "Yeni Güncelleme Mevcut!"
            }
            "settings_up_to_date" -> when {
                isEn -> "Application is Up-to-Date"
                isDe -> "App ist auf dem neuesten Stand"
                else -> "Uygulama Güncel"
            }
            "settings_update_btn" -> when {
                isEn -> "Download & Install Update"
                isDe -> "Update herunterladen & installieren"
                else -> "Güncellemeyi İndir & Kur"
            }

            // Feedback / Support Screen
            "feedback_title" -> when {
                isEn -> "Contact Support / Feedback"
                isDe -> "Support kontaktieren / Feedback"
                else -> "Üreticiye / Destek Ekibine Mesaj"
            }
            "feedback_subtitle" -> when {
                isEn -> "Send your inquiries, bug reports or feedback to the developer"
                isDe -> "Senden Sie Anfragen, Fehlerberichte oder Feedback an den Entwickler"
                else -> "Sorularınızı, hata bildirimlerinizi ve önerilerinizi üreticiye iletin"
            }
            "feedback_send" -> when {
                isEn -> "Send Message"
                isDe -> "Nachricht senden"
                else -> "Mesajı Gönder"
            }
            "feedback_name_hint" -> when {
                isEn -> "Your Name or Email Address"
                isDe -> "Ihr Name oder E-Mail-Adresse"
                else -> "Adınız veya E-posta Adresiniz"
            }
            "feedback_msg_hint" -> when {
                isEn -> "Write your message, problem, or feature request..."
                isDe -> "Schreiben Sie Ihre Nachricht, Problem oder Anfrage..."
                else -> "Mesajınızı, sorunuzu veya hata bildirimini yazınız..."
            }
            "feedback_add_audio" -> when {
                isEn -> "Attach Audio"
                isDe -> "Audio anfügen"
                else -> "Ses Ekle"
            }
            "feedback_add_image" -> when {
                isEn -> "Attach Screenshot"
                isDe -> "Screenshot anfügen"
                else -> "Ekran Görüntüsü Ekle"
            }
            "feedback_add_pdf" -> when {
                isEn -> "Attach PDF"
                isDe -> "PDF anfügen"
                else -> "PDF Ekle"
            }
            "feedback_support_alert" -> when {
                isEn -> "🔔 Support Team Notification"
                isDe -> "🔔 Support-Team Benachrichtigung"
                else -> "🔔 Destek Ekibi Uyarısı"
            }
            "feedback_no_alerts" -> when {
                isEn -> "No active support alerts"
                isDe -> "Keine aktiven Support-Warnungen"
                else -> "Aktif destek bildirimi yok"
            }
            "feedback_my_messages" -> when {
                isEn -> "My Sent Messages"
                isDe -> "Meine gesendeten Nachrichten"
                else -> "Gönderilen Mesajlarım"
            }

            // Drive Backup
            "drive_backup_all" -> when {
                isEn -> "Backup All to Drive"
                isDe -> "Alle auf Drive sichern"
                else -> "Tümünü Drive'a Yedekle"
            }
            "drive_backup_selected" -> when {
                isEn -> "Export Selected to Drive"
                isDe -> "Ausgewählte auf Drive sichern"
                else -> "Seçilenleri Drive'a Aktar"
            }
            "drive_save_now" -> when {
                isEn -> "Save to Google Drive Now"
                isDe -> "Jetzt auf Google Drive speichern"
                else -> "Google Drive'a Şimdi Kaydet"
            }
            "drive_open_browser" -> when {
                isEn -> "Open in Google Drive"
                isDe -> "In Google Drive öffnen"
                else -> "Drive'da Kendi Hesabında Aç"
            }
            "drive_dialog_title" -> when {
                isEn -> "Google Drive Cloud Backup"
                isDe -> "Google Drive Cloud-Sicherung"
                else -> "Google Drive Bulut Yedekleme"
            }

            else -> key
        }
    }
}
