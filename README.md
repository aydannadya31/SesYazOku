# 🎙️ SesYazıcı AI

> **Canlı ve Harici Görüşme Ses Transkripsiyonu, Konuşmacı Etiketleme, Yapay Zeka Özetleme ve Görev Yöneticisi**

[![Android CI & Build APK](https://github.com/actions/workflows/build.yml/badge.svg)](../../actions)
[![Deploy Live Web App to GitHub Pages](https://github.com/actions/workflows/pages.yml/badge.svg)](../../actions)
![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Web-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-green)
![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey)

---

## 🌐 GitHub Üzerinde Canlı Çalışma & Kullanım

Bu depo, GitHub üzerinde hem **otomatik derleme (CI/CD)** hem de **canlı web uygulaması (GitHub Pages)** olarak çalışacak şekilde yapılandırılmıştır:

1. **Canlı Web Sürümü (GitHub Pages)**:
   - Depo ayarlarından **GitHub Pages**'i etkinleştirdiğinizde (veya GitHub Actions otomatik dağıtımı tamamlandığında), `https://<kullanici-adiniz>.github.io/<depo-adiniz>/` adresi üzerinden tarayıcınızda canlı olarak çalışır.
   - Tarayıcı üzerinden mikrofonla canlı transkripsiyon, konuşmacı ayrımı, yapay zeka özetleme ve PDF/Word dışa aktarma işlemlerini anında test edebilirsiniz.

2. **Otomatik Android APK Derlemesi (GitHub Actions)**:
   - Depoya her `git push` yapıldığında veya `v*` sürüm etiketi eklendiğinde `.github/workflows/build.yml` iş akışı devreye girer.
   - Android APK'sı otomatik olarak derlenir ve GitHub Actions **Artifacts** ile **Releases** sekmesinden hemen indirilip telefona kurulabilir.

---

## 🚀 Öne Çıkan Özellikler

### 1. 🎙️ Canlı & Kayıtlı Ses Transkripsiyonu (Süre Sınırı Yok)
- **Sınırsız Süre:** Toplantılar, mülakatlar, dersler ve saatler süren görüşmeler için kesintisiz kayıt ve transkripsiyon.
- **Ses Dosyası Yükleme:** Cihazınızdaki `.mp3`, `.wav`, `.m4a` formatındaki ses kayıtlarını yükleyip tek tıkla yazıya dökebilme.
- **Entegre Ses Oynatıcı:** Zaman damgalı ses dalgası, ±5 saniye atlama ve ayarlanabilir oynatma hızları (0.75x, 1x, 1.25x, 1.5x, 2x).

### 2. 🎥 Harici Canlı Görüşme Yakalama (Zoom, Meet, Teams, WhatsApp)
- **Ön Plan Servisi (Foreground Service):** Siz Zoom, Google Meet veya WhatsApp görüşmesine geçtiğinizde arka planda mikrofon ve görüşme seslerini kaydetmeye ve transkribe etmeye devam eder.
- **Ekran Üstü Kayan Baloncuk (Floating Bubble):** Video görüşmesini engellemeden ekranda gezdirilebilen, canlı transkripti gösteren ve tek dokunuşla konuşmacı değiştirmeye olanak tanıyan yüzen pencere.
- **Görüşmeye Özel Ses Filtresi (`VOICE_COMMUNICATION`):** Donanımsal yankı ve gürültü engelleme desteği.

### 3. 👥 Konuşmacı Etiketleme (Speaker Diarization)
- Katılımcıları ayırt etme (*Konuşmacı 1*, *Konuşmacı 2*, *Ahmet*, *Zeynep* vb.).
- Canlı kayıt ve görüşme sırasında anlık **"Konuşmacı Değiştir"** butonu.
- Transkript üzerinde konuşmacı isimlerini ve renk etiketlerini düzenleme.
- Konuşmacı konuşma payı analiz grafiği.

### 4. 🤖 Yapay Zeka ile Otomatik Özetleme
- **Yönetici Özeti (Executive Summary):** Görüşmenin ana fikri.
- **Alınan Kararlar:** Toplantıda mutabık kalınan kritik noktalar.
- **Aksiyon Maddeleri:** Yapılacak işler, görevliler ve aciliyet dereceleri.

### 5. 📄 4 Farklı Formatta Dışa Aktarma
- **PDF (.pdf):** Renkli başlıklar, katılımcı etiketleri ve yapay zeka özeti içeren doküman.
- **Word (.docx):** Microsoft Word ve Google Docs ile %100 uyumlu standart OpenXML dokümanı.
- **Düz Metin (.txt):** Standart not formatı.
- **Altyazı (.srt):** Zaman damgalı video altyazı formatı.

### 6. ☁️ Bulut Senkronizasyonu & Çevrimdışı Öncelikli
- **Çevrimdışı (Room/SQLite):** İnternet bağlantısı olmasa bile tüm kayıtlar cihazda güvenle saklanır.
- **Bulut Yedekleme & Çapraz Platform (.json):** Verilerinizi web, masaüstü veya diğer cihazlarla senkronize eden evrensel yedekleme paketi.

### 7. 🔊 Metni Sesli Dinleme (Text-to-Speech)
- Transkripti veya özeti sesli olarak dinleme, konuşma hızı ayarı ve seslendirilen cümlenin vurgulanması.

### 8. 🌓 Karanlık Mod & Çoklu Dil Desteği
- Sistem, Açık ve AMOLED uyumlu Koyu tema seçenekleri.
- Türkçe, English, Deutsch, Español ve Français dil desteği.

---

## 🛠️ Yerel Geliştirme & Derleme

Depoyu yerel bilgisayarınızda derlemek için:

```bash
# Depoyu klonlayın
git clone https://github.com/<kullanici-adiniz>/<depo-adiniz>.git
cd <depo-adiniz>

# Testleri çalıştırın
./gradlew testDebugUnitTest

# Debug APK derleyin
./gradlew assembleDebug
```

Derlenen APK dosyası `app/build/outputs/apk/debug/app-debug.apk` dizininde oluşturulacaktır.

---

## 📄 Lisans
Bu proje [Apache License 2.0](LICENSE) ile lisanslanmıştır.
