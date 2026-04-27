package com.study.trigger.internal

import android.app.Application
import android.os.Bundle
import com.study.trigger.NavigationTarget
import com.study.trigger.R
import com.study.trigger.ReminderContent
import com.study.trigger.RetentionBehaviorConfig
import com.study.trigger.RetentionModule
import com.study.trigger.ToolbarItem
import java.util.Locale

internal object RetentionStaticContent {

    fun buildToolbarItems(application: Application, locale: Locale): List<ToolbarItem> {
        val labels = toolbarLabelsByLanguage[normalizeLanguageCode(locale)] ?: toolbarLabelsByLanguage.getValue("en")
        return listOf(
            ToolbarItem(
                iconResId = R.drawable.ic_play_arrow,
                title = labels[0],
                target = buildTarget(entry = "toolbar_photos", jumpPage = 1),
            ),
            ToolbarItem(
                iconResId = R.drawable.ic_play_arrow,
                title = labels[1],
                target = buildTarget(entry = "toolbar_videos", jumpPage = 2),
            ),
            ToolbarItem(
                iconResId = R.drawable.ic_play_arrow,
                title = labels[2],
                target = buildTarget(entry = "toolbar_audio", jumpPage = 3),
            ),
            ToolbarItem(
                iconResId = R.drawable.ic_play_arrow,
                title = labels[3],
                target = buildTarget(entry = "toolbar_files", jumpPage = 4),
            ),
        )
    }

    fun buildReminderContents(locale: Locale): List<ReminderContent> {
        val seeds = reminderSeedsByLanguage[normalizeLanguageCode(locale)] ?: reminderSeedsByLanguage.getValue("en")
        return seeds.map { seed ->
            val jumpPage = resolveReminderJumpPage(seed.targetPage)
            ReminderContent(
                bucketId = seed.bucketId,
                iconResId = reminderIcon(jumpPage),
                message = seed.message,
                actionLabel = seed.action,
                target = buildTarget(entry = navigationTarget(seed.targetPage), jumpPage = jumpPage),
            )
        }
    }

    fun bucketOrder(behaviorConfig: RetentionBehaviorConfig): List<Int> {
        return if (behaviorConfig.fileRefererReadBlocked) {
            listOf(500001, 500002, 500004)
        } else {
            listOf(500020, 500030, 500001, 500002, 500004)
        }
    }

    private fun normalizeLanguageCode(locale: Locale): String {
        val language = locale.language.lowercase(Locale.US)
        val normalized = if (language == "in") "id" else language
        return if (normalized in reminderSeedsByLanguage) normalized else "en"
    }

    private fun reminderIcon(targetPage: Int): Int {
        return when (targetPage) {
            1 -> R.drawable.image_large_photo
            2 -> R.drawable.image_large_video
            3 -> R.drawable.image_large_audio
            4 -> R.drawable.image_large_document
            else -> R.drawable.image_large_photo
        }
    }

    private fun resolveReminderJumpPage(targetPage: Int): Int {
        return when (targetPage) {
            20 -> 4
            30 -> 1
            else -> targetPage
        }
    }

    private fun navigationTarget(targetPage: Int): String {
        return when (targetPage) {
            20 -> "cleanup"
            30 -> "deep_scan"
            1 -> "recover_photos"
            2 -> "recover_videos"
            3 -> "recover_audio"
            else -> "recover_files"
        }
    }

    private fun buildTarget(entry: String, jumpPage: Int): NavigationTarget {
        return NavigationTarget(
            id = entry,
            extras = Bundle().apply {
                putString(RetentionModule.EXTRA_RETENTION_SOURCE, SOURCE_RETENTION_KIT)
                putInt(RetentionModule.EXTRA_LEGACY_JUMP_TO, jumpPage)
            },
        )
    }

    private const val SOURCE_RETENTION_KIT = "retention-kit"

    private data class ReminderSeed(
        val targetPage: Int,
        val bucketId: Int,
        val message: String,
        val action: String,
    )

    private val toolbarLabelsByLanguage: Map<String, List<String>> = mapOf(
        "en" to listOf("Photos", "Videos", "Audio", "Files"),
        "ar" to listOf("الصور", "الفيديوهات", "الصوت", "الملفات"),
        "de" to listOf("Fotos", "Videos", "Audio", "Dateien"),
        "es" to listOf("Fotos", "Vídeos", "Audio", "Archivos"),
        "fa" to listOf("عکس‌ها", "ویدئوها", "صدا", "فایل‌ها"),
        "fr" to listOf("Photos", "Vidéos", "Audio", "Fichiers"),
        "id" to listOf("Foto", "Video", "Audio", "File"),
        "pt" to listOf("Fotos", "Vídeos", "Áudio", "Arquivos"),
    )

    private val reminderSeedsByLanguage: Map<String, List<ReminderSeed>> = mapOf(
        "en" to listOf(
            ReminderSeed(20, 500020, "Duplicate files found and taking up space 🧹", "Clean"),
            ReminderSeed(20, 500020, "Multiple copies of the same file are ready to review", "Check"),
            ReminderSeed(20, 500020, "Extra duplicate files are filling your storage", "Scan"),
            ReminderSeed(20, 500020, "Free up space by clearing duplicate files", "Start"),
            ReminderSeed(20, 500020, "Temporary files can be cleaned to save space", "Clean"),
            ReminderSeed(20, 500020, "Junk files found and ready to fix 🗂️", "Fix"),
            ReminderSeed(30, 500030, "Matching copies are ready to review", "View"),
            ReminderSeed(30, 500030, "Merge duplicate items to reclaim storage", "Free"),
            ReminderSeed(30, 500030, "Remove unwanted photos to free up space 🗑️", "Delete"),
            ReminderSeed(30, 500030, "Start a deep cleanup for your gallery", "Start"),
            ReminderSeed(30, 500030, "Hidden junk files are ready to clean 🧽", "Clean"),
            ReminderSeed(30, 500030, "Unused data was found during the scan", "Remove"),
            ReminderSeed(30, 500030, "Run a deep scan to tidy up storage", "Scan"),
            ReminderSeed(1, 500001, "Deleted photos may still be recoverable 📷", "Recover"),
            ReminderSeed(1, 500001, "Restore lost photos while they are still available", "Restore"),
            ReminderSeed(1, 500001, "Recoverable images were found in the scan 🖼️", "Save"),
            ReminderSeed(1, 500001, "Scan results are ready for photo review", "View"),
            ReminderSeed(1, 500001, "Check now for photos that can still be recovered", "Check"),
            ReminderSeed(2, 500002, "Recoverable videos were found in the scan 🎬", "Save"),
            ReminderSeed(2, 500002, "Restore missing videos from recent scan results", "Restore"),
            ReminderSeed(2, 500002, "Videos may still be available to review", "View"),
            ReminderSeed(2, 500002, "Large videos were found and can be recovered 🎞️", "Recover"),
            ReminderSeed(2, 500002, "Save recoverable videos before they are cleared", "Secure"),
            ReminderSeed(4, 500004, "Recoverable documents were found in the scan 📄", "Restore"),
            ReminderSeed(4, 500004, "Files may be replaced unless you save them", "Save"),
            ReminderSeed(4, 500004, "Lost documents are ready to recover", "Recover"),
            ReminderSeed(4, 500004, "Scanned files are ready to review", "View"),
            ReminderSeed(4, 500004, "Check now for deleted files that can be restored 📁", "Check"),
        ),
        "ar" to listOf(
            ReminderSeed(20, 500020, "تم العثور على ملفات مكررة تشغل مساحة 🧹", "تنظيف"),
            ReminderSeed(20, 500020, "نسخ متعددة من الملف نفسه جاهزة للمراجعة", "تحقق"),
            ReminderSeed(20, 500020, "الملفات المكررة الإضافية تملأ مساحة التخزين", "مسح"),
            ReminderSeed(20, 500020, "حرر المساحة بحذف الملفات المكررة", "ابدأ"),
            ReminderSeed(20, 500020, "يمكن تنظيف الملفات المؤقتة لتوفير المساحة", "تنظيف"),
            ReminderSeed(20, 500020, "تم العثور على ملفات غير مهمة وجاهزة للإصلاح 🗂️", "إصلاح"),
            ReminderSeed(30, 500030, "النسخ المتطابقة جاهزة للمراجعة", "عرض"),
            ReminderSeed(30, 500030, "ادمج العناصر المكررة لاستعادة المساحة", "تحرير"),
            ReminderSeed(30, 500030, "احذف الصور غير المرغوب فيها لتوفير مساحة 🗑️", "حذف"),
            ReminderSeed(30, 500030, "ابدأ تنظيفًا عميقًا لمعرضك", "ابدأ"),
            ReminderSeed(30, 500030, "الملفات المخفية غير المهمة جاهزة للتنظيف 🧽", "تنظيف"),
            ReminderSeed(30, 500030, "تم العثور على بيانات غير مستخدمة أثناء المسح", "إزالة"),
            ReminderSeed(30, 500030, "شغّل مسحًا عميقًا لترتيب التخزين", "مسح"),
            ReminderSeed(1, 500001, "قد تظل الصور المحذوفة قابلة للاستعادة 📷", "استعادة"),
            ReminderSeed(1, 500001, "استرجع الصور المفقودة بينما لا تزال متاحة", "استرجاع"),
            ReminderSeed(1, 500001, "تم العثور على صور قابلة للاستعادة في المسح 🖼️", "حفظ"),
            ReminderSeed(1, 500001, "نتائج المسح جاهزة لمراجعة الصور", "عرض"),
            ReminderSeed(1, 500001, "تحقق الآن من الصور التي لا تزال قابلة للاستعادة", "تحقق"),
            ReminderSeed(2, 500002, "تم العثور على فيديوهات قابلة للاستعادة في المسح 🎬", "حفظ"),
            ReminderSeed(2, 500002, "استرجع الفيديوهات المفقودة من نتائج المسح الأخيرة", "استرجاع"),
            ReminderSeed(2, 500002, "قد تظل الفيديوهات متاحة للمراجعة", "عرض"),
            ReminderSeed(2, 500002, "تم العثور على فيديوهات كبيرة ويمكن استعادتها 🎞️", "استعادة"),
            ReminderSeed(2, 500002, "احفظ الفيديوهات القابلة للاستعادة قبل حذفها", "تأمين"),
            ReminderSeed(4, 500004, "تم العثور على مستندات قابلة للاستعادة في المسح 📄", "استرجاع"),
            ReminderSeed(4, 500004, "قد يتم استبدال الملفات ما لم تحفظها", "حفظ"),
            ReminderSeed(4, 500004, "المستندات المفقودة جاهزة للاستعادة", "استعادة"),
            ReminderSeed(4, 500004, "الملفات الممسوحة جاهزة للمراجعة", "عرض"),
            ReminderSeed(4, 500004, "تحقق الآن من الملفات المحذوفة القابلة للاستعادة 📁", "تحقق"),
        ),
        "de" to listOf(
            ReminderSeed(20, 500020, "Doppelte Dateien gefunden, die Speicher belegen 🧹", "Bereinigen"),
            ReminderSeed(20, 500020, "Mehrere Kopien derselben Datei sind zur Prüfung bereit", "Prüfen"),
            ReminderSeed(20, 500020, "Zusätzliche Duplikate füllen deinen Speicher", "Scannen"),
            ReminderSeed(20, 500020, "Gib Speicher frei, indem du doppelte Dateien entfernst", "Starten"),
            ReminderSeed(20, 500020, "Temporäre Dateien können bereinigt werden, um Platz zu sparen", "Bereinigen"),
            ReminderSeed(20, 500020, "Junk-Dateien wurden gefunden und können behoben werden 🗂️", "Beheben"),
            ReminderSeed(30, 500030, "Übereinstimmende Kopien sind zur Prüfung bereit", "Anzeigen"),
            ReminderSeed(30, 500030, "Führe doppelte Elemente zusammen, um Speicher freizugeben", "Freigeben"),
            ReminderSeed(30, 500030, "Entferne unerwünschte Fotos, um Speicher freizugeben 🗑️", "Löschen"),
            ReminderSeed(30, 500030, "Starte eine gründliche Bereinigung deiner Galerie", "Starten"),
            ReminderSeed(30, 500030, "Versteckte Junk-Dateien sind zur Bereinigung bereit 🧽", "Bereinigen"),
            ReminderSeed(30, 500030, "Beim Scan wurden ungenutzte Daten gefunden", "Entfernen"),
            ReminderSeed(30, 500030, "Starte einen Tiefenscan, um den Speicher aufzuräumen", "Scannen"),
            ReminderSeed(1, 500001, "Gelöschte Fotos könnten noch wiederherstellbar sein 📷", "Wiederherstellen"),
            ReminderSeed(1, 500001, "Stelle verlorene Fotos wieder her, solange sie noch verfügbar sind", "Wiederherstellen"),
            ReminderSeed(1, 500001, "Wiederherstellbare Bilder wurden im Scan gefunden 🖼️", "Sichern"),
            ReminderSeed(1, 500001, "Scan-Ergebnisse sind zur Fotoansicht bereit", "Anzeigen"),
            ReminderSeed(1, 500001, "Prüfe jetzt, welche Fotos noch wiederherstellbar sind", "Prüfen"),
            ReminderSeed(2, 500002, "Wiederherstellbare Videos wurden im Scan gefunden 🎬", "Sichern"),
            ReminderSeed(2, 500002, "Stelle fehlende Videos aus den letzten Scan-Ergebnissen wieder her", "Wiederherstellen"),
            ReminderSeed(2, 500002, "Videos könnten noch zur Ansicht verfügbar sein", "Anzeigen"),
            ReminderSeed(2, 500002, "Große Videos wurden gefunden und können wiederhergestellt werden 🎞️", "Wiederherstellen"),
            ReminderSeed(2, 500002, "Sichere wiederherstellbare Videos, bevor sie entfernt werden", "Schützen"),
            ReminderSeed(4, 500004, "Wiederherstellbare Dokumente wurden im Scan gefunden 📄", "Wiederherstellen"),
            ReminderSeed(4, 500004, "Dateien könnten ersetzt werden, wenn du sie nicht sicherst", "Sichern"),
            ReminderSeed(4, 500004, "Verlorene Dokumente sind zur Wiederherstellung bereit", "Wiederherstellen"),
            ReminderSeed(4, 500004, "Gescannte Dateien sind zur Ansicht bereit", "Anzeigen"),
            ReminderSeed(4, 500004, "Prüfe jetzt gelöschte Dateien, die wiederhergestellt werden können 📁", "Prüfen"),
        ),
        "es" to listOf(
            ReminderSeed(20, 500020, "Se encontraron archivos duplicados que ocupan espacio 🧹", "Limpiar"),
            ReminderSeed(20, 500020, "Varias copias del mismo archivo están listas para revisarse", "Revisar"),
            ReminderSeed(20, 500020, "Los archivos duplicados extra están llenando tu almacenamiento", "Escanear"),
            ReminderSeed(20, 500020, "Libera espacio eliminando archivos duplicados", "Iniciar"),
            ReminderSeed(20, 500020, "Los archivos temporales pueden limpiarse para ahorrar espacio", "Limpiar"),
            ReminderSeed(20, 500020, "Se encontraron archivos basura listos para corregirse 🗂️", "Corregir"),
            ReminderSeed(30, 500030, "Las copias coincidentes están listas para revisarse", "Ver"),
            ReminderSeed(30, 500030, "Combina elementos duplicados para recuperar espacio", "Liberar"),
            ReminderSeed(30, 500030, "Elimina fotos no deseadas para liberar espacio 🗑️", "Eliminar"),
            ReminderSeed(30, 500030, "Inicia una limpieza profunda de tu galería", "Iniciar"),
            ReminderSeed(30, 500030, "Los archivos basura ocultos están listos para limpiarse 🧽", "Limpiar"),
            ReminderSeed(30, 500030, "Se encontraron datos sin uso durante el escaneo", "Quitar"),
            ReminderSeed(30, 500030, "Ejecuta un escaneo profundo para ordenar el almacenamiento", "Escanear"),
            ReminderSeed(1, 500001, "Las fotos eliminadas aún podrían recuperarse 📷", "Recuperar"),
            ReminderSeed(1, 500001, "Restaura fotos perdidas mientras sigan disponibles", "Restaurar"),
            ReminderSeed(1, 500001, "Se encontraron imágenes recuperables en el escaneo 🖼️", "Guardar"),
            ReminderSeed(1, 500001, "Los resultados del escaneo están listos para revisar fotos", "Ver"),
            ReminderSeed(1, 500001, "Revisa ahora qué fotos aún pueden recuperarse", "Revisar"),
            ReminderSeed(2, 500002, "Se encontraron videos recuperables en el escaneo 🎬", "Guardar"),
            ReminderSeed(2, 500002, "Restaura videos perdidos desde los resultados recientes", "Restaurar"),
            ReminderSeed(2, 500002, "Los videos aún podrían estar disponibles para revisarse", "Ver"),
            ReminderSeed(2, 500002, "Se encontraron videos grandes y pueden recuperarse 🎞️", "Recuperar"),
            ReminderSeed(2, 500002, "Guarda los videos recuperables antes de que se eliminen", "Proteger"),
            ReminderSeed(4, 500004, "Se encontraron documentos recuperables en el escaneo 📄", "Restaurar"),
            ReminderSeed(4, 500004, "Los archivos podrían reemplazarse si no los guardas", "Guardar"),
            ReminderSeed(4, 500004, "Los documentos perdidos están listos para recuperarse", "Recuperar"),
            ReminderSeed(4, 500004, "Los archivos escaneados están listos para revisarse", "Ver"),
            ReminderSeed(4, 500004, "Revisa ahora qué archivos eliminados pueden restaurarse 📁", "Revisar"),
        ),
        "fa" to listOf(
            ReminderSeed(20, 500020, "فایل‌های تکراری پیدا شدند و فضا اشغال کرده‌اند 🧹", "پاکسازی"),
            ReminderSeed(20, 500020, "چند نسخه از یک فایل برای بررسی آماده است", "بررسی"),
            ReminderSeed(20, 500020, "فایل‌های تکراری اضافه فضای ذخیره‌سازی را پر کرده‌اند", "اسکن"),
            ReminderSeed(20, 500020, "با پاک کردن فایل‌های تکراری فضا آزاد کنید", "شروع"),
            ReminderSeed(20, 500020, "فایل‌های موقت را می‌توان برای صرفه‌جویی در فضا پاک کرد", "پاکسازی"),
            ReminderSeed(20, 500020, "فایل‌های اضافی پیدا شدند و آماده رفع هستند 🗂️", "رفع"),
            ReminderSeed(30, 500030, "نسخه‌های مشابه برای بررسی آماده هستند", "مشاهده"),
            ReminderSeed(30, 500030, "برای بازپس‌گیری فضا، موارد تکراری را ادغام کنید", "آزادسازی"),
            ReminderSeed(30, 500030, "برای آزاد کردن فضا، عکس‌های ناخواسته را حذف کنید 🗑️", "حذف"),
            ReminderSeed(30, 500030, "پاکسازی عمیق گالری را شروع کنید", "شروع"),
            ReminderSeed(30, 500030, "فایل‌های پنهان اضافی آماده پاکسازی هستند 🧽", "پاکسازی"),
            ReminderSeed(30, 500030, "داده‌های بدون استفاده در اسکن پیدا شدند", "پاک کردن"),
            ReminderSeed(30, 500030, "برای مرتب کردن حافظه، یک اسکن عمیق اجرا کنید", "اسکن"),
            ReminderSeed(1, 500001, "عکس‌های حذف‌شده ممکن است هنوز قابل بازیابی باشند 📷", "بازیابی"),
            ReminderSeed(1, 500001, "تا وقتی هنوز در دسترس‌اند، عکس‌های از دست‌رفته را بازگردانید", "بازگرداندن"),
            ReminderSeed(1, 500001, "تصاویر قابل بازیابی در اسکن پیدا شدند 🖼️", "ذخیره"),
            ReminderSeed(1, 500001, "نتایج اسکن برای بررسی عکس‌ها آماده است", "مشاهده"),
            ReminderSeed(1, 500001, "همین حالا عکس‌هایی را که هنوز قابل بازیابی‌اند بررسی کنید", "بررسی"),
            ReminderSeed(2, 500002, "ویدئوهای قابل بازیابی در اسکن پیدا شدند 🎬", "ذخیره"),
            ReminderSeed(2, 500002, "ویدئوهای از دست‌رفته را از نتایج اخیر بازگردانید", "بازگرداندن"),
            ReminderSeed(2, 500002, "ویدئوها ممکن است هنوز برای بررسی در دسترس باشند", "مشاهده"),
            ReminderSeed(2, 500002, "ویدئوهای حجیم پیدا شدند و قابل بازیابی هستند 🎞️", "بازیابی"),
            ReminderSeed(2, 500002, "پیش از پاک شدن، ویدئوهای قابل بازیابی را ایمن کنید", "ایمن‌سازی"),
            ReminderSeed(4, 500004, "اسناد قابل بازیابی در اسکن پیدا شدند 📄", "بازگرداندن"),
            ReminderSeed(4, 500004, "اگر فایل‌ها را ذخیره نکنید ممکن است جایگزین شوند", "ذخیره"),
            ReminderSeed(4, 500004, "اسناد از دست‌رفته برای بازیابی آماده هستند", "بازیابی"),
            ReminderSeed(4, 500004, "فایل‌های اسکن‌شده برای بررسی آماده هستند", "مشاهده"),
            ReminderSeed(4, 500004, "همین حالا فایل‌های حذف‌شده قابل بازگردانی را بررسی کنید 📁", "بررسی"),
        ),
        "fr" to listOf(
            ReminderSeed(20, 500020, "Des fichiers en double occupent de l’espace 🧹", "Nettoyer"),
            ReminderSeed(20, 500020, "Plusieurs copies du même fichier sont prêtes à être vérifiées", "Vérifier"),
            ReminderSeed(20, 500020, "Des doublons supplémentaires remplissent votre stockage", "Scanner"),
            ReminderSeed(20, 500020, "Libérez de l’espace en supprimant les fichiers en double", "Démarrer"),
            ReminderSeed(20, 500020, "Les fichiers temporaires peuvent être nettoyés pour gagner de l’espace", "Nettoyer"),
            ReminderSeed(20, 500020, "Des fichiers inutiles ont été trouvés et peuvent être corrigés 🗂️", "Corriger"),
            ReminderSeed(30, 500030, "Des copies identiques sont prêtes à être vérifiées", "Voir"),
            ReminderSeed(30, 500030, "Fusionnez les éléments en double pour récupérer de l’espace", "Libérer"),
            ReminderSeed(30, 500030, "Supprimez les photos indésirables pour libérer de l’espace 🗑️", "Supprimer"),
            ReminderSeed(30, 500030, "Lancez un nettoyage approfondi de votre galerie", "Démarrer"),
            ReminderSeed(30, 500030, "Des fichiers inutiles cachés sont prêts à être nettoyés 🧽", "Nettoyer"),
            ReminderSeed(30, 500030, "Des données inutilisées ont été trouvées pendant l’analyse", "Retirer"),
            ReminderSeed(30, 500030, "Lancez une analyse approfondie pour ranger le stockage", "Scanner"),
            ReminderSeed(1, 500001, "Des photos supprimées peuvent encore être récupérables 📷", "Récupérer"),
            ReminderSeed(1, 500001, "Restaurez les photos perdues tant qu’elles sont encore disponibles", "Restaurer"),
            ReminderSeed(1, 500001, "Des images récupérables ont été trouvées pendant l’analyse 🖼️", "Enregistrer"),
            ReminderSeed(1, 500001, "Les résultats de l’analyse sont prêts pour la revue des photos", "Voir"),
            ReminderSeed(1, 500001, "Vérifiez maintenant quelles photos peuvent encore être récupérées", "Vérifier"),
            ReminderSeed(2, 500002, "Des vidéos récupérables ont été trouvées pendant l’analyse 🎬", "Enregistrer"),
            ReminderSeed(2, 500002, "Restaurez les vidéos perdues à partir des derniers résultats", "Restaurer"),
            ReminderSeed(2, 500002, "Les vidéos peuvent encore être disponibles pour vérification", "Voir"),
            ReminderSeed(2, 500002, "De grandes vidéos ont été trouvées et peuvent être récupérées 🎞️", "Récupérer"),
            ReminderSeed(2, 500002, "Sécurisez les vidéos récupérables avant leur suppression", "Sécuriser"),
            ReminderSeed(4, 500004, "Des documents récupérables ont été trouvés pendant l’analyse 📄", "Restaurer"),
            ReminderSeed(4, 500004, "Les fichiers peuvent être remplacés si vous ne les enregistrez pas", "Enregistrer"),
            ReminderSeed(4, 500004, "Les documents perdus sont prêts à être récupérés", "Récupérer"),
            ReminderSeed(4, 500004, "Les fichiers analysés sont prêts à être consultés", "Voir"),
            ReminderSeed(4, 500004, "Vérifiez maintenant quels fichiers supprimés peuvent être restaurés 📁", "Vérifier"),
        ),
        "id" to listOf(
            ReminderSeed(20, 500020, "File duplikat ditemukan dan memakan ruang 🧹", "Bersihkan"),
            ReminderSeed(20, 500020, "Beberapa salinan file yang sama siap ditinjau", "Periksa"),
            ReminderSeed(20, 500020, "File duplikat tambahan memenuhi penyimpanan Anda", "Pindai"),
            ReminderSeed(20, 500020, "Kosongkan ruang dengan menghapus file duplikat", "Mulai"),
            ReminderSeed(20, 500020, "File sementara bisa dibersihkan untuk menghemat ruang", "Bersihkan"),
            ReminderSeed(20, 500020, "File sampah ditemukan dan siap diperbaiki 🗂️", "Perbaiki"),
            ReminderSeed(30, 500030, "Salinan yang cocok siap ditinjau", "Lihat"),
            ReminderSeed(30, 500030, "Gabungkan item duplikat untuk mendapatkan kembali ruang", "Bebaskan"),
            ReminderSeed(30, 500030, "Hapus foto yang tidak diinginkan untuk mengosongkan ruang 🗑️", "Hapus"),
            ReminderSeed(30, 500030, "Mulai pembersihan mendalam untuk galeri Anda", "Mulai"),
            ReminderSeed(30, 500030, "File sampah tersembunyi siap dibersihkan 🧽", "Bersihkan"),
            ReminderSeed(30, 500030, "Data yang tidak terpakai ditemukan saat pemindaian", "Buang"),
            ReminderSeed(30, 500030, "Jalankan pemindaian mendalam untuk merapikan penyimpanan", "Pindai"),
            ReminderSeed(1, 500001, "Foto yang dihapus mungkin masih bisa dipulihkan 📷", "Pulihkan"),
            ReminderSeed(1, 500001, "Kembalikan foto yang hilang selagi masih tersedia", "Kembalikan"),
            ReminderSeed(1, 500001, "Gambar yang bisa dipulihkan ditemukan saat pemindaian 🖼️", "Simpan"),
            ReminderSeed(1, 500001, "Hasil pemindaian siap untuk meninjau foto", "Lihat"),
            ReminderSeed(1, 500001, "Periksa sekarang foto yang masih bisa dipulihkan", "Periksa"),
            ReminderSeed(2, 500002, "Video yang bisa dipulihkan ditemukan saat pemindaian 🎬", "Simpan"),
            ReminderSeed(2, 500002, "Kembalikan video yang hilang dari hasil pemindaian terbaru", "Kembalikan"),
            ReminderSeed(2, 500002, "Video mungkin masih tersedia untuk ditinjau", "Lihat"),
            ReminderSeed(2, 500002, "Video berukuran besar ditemukan dan bisa dipulihkan 🎞️", "Pulihkan"),
            ReminderSeed(2, 500002, "Amankan video yang bisa dipulihkan sebelum dibersihkan", "Amankan"),
            ReminderSeed(4, 500004, "Dokumen yang bisa dipulihkan ditemukan saat pemindaian 📄", "Kembalikan"),
            ReminderSeed(4, 500004, "File bisa tergantikan jika tidak disimpan", "Simpan"),
            ReminderSeed(4, 500004, "Dokumen yang hilang siap dipulihkan", "Pulihkan"),
            ReminderSeed(4, 500004, "File hasil pemindaian siap ditinjau", "Lihat"),
            ReminderSeed(4, 500004, "Periksa sekarang file terhapus yang bisa dikembalikan 📁", "Periksa"),
        ),
        "pt" to listOf(
            ReminderSeed(20, 500020, "Arquivos duplicados foram encontrados ocupando espaço 🧹", "Limpar"),
            ReminderSeed(20, 500020, "Várias cópias do mesmo arquivo estão prontas para revisão", "Verificar"),
            ReminderSeed(20, 500020, "Arquivos duplicados extras estão enchendo seu armazenamento", "Escanear"),
            ReminderSeed(20, 500020, "Libere espaço removendo arquivos duplicados", "Iniciar"),
            ReminderSeed(20, 500020, "Arquivos temporários podem ser limpos para economizar espaço", "Limpar"),
            ReminderSeed(20, 500020, "Arquivos inúteis foram encontrados e podem ser corrigidos 🗂️", "Corrigir"),
            ReminderSeed(30, 500030, "Cópias correspondentes estão prontas para revisão", "Ver"),
            ReminderSeed(30, 500030, "Una itens duplicados para recuperar espaço", "Liberar"),
            ReminderSeed(30, 500030, "Remova fotos indesejadas para liberar espaço 🗑️", "Excluir"),
            ReminderSeed(30, 500030, "Inicie uma limpeza profunda da sua galeria", "Iniciar"),
            ReminderSeed(30, 500030, "Arquivos inúteis ocultos estão prontos para limpeza 🧽", "Limpar"),
            ReminderSeed(30, 500030, "Dados sem uso foram encontrados durante a varredura", "Remover"),
            ReminderSeed(30, 500030, "Execute uma varredura profunda para organizar o armazenamento", "Escanear"),
            ReminderSeed(1, 500001, "Fotos excluídas ainda podem ser recuperáveis 📷", "Recuperar"),
            ReminderSeed(1, 500001, "Restaure fotos perdidas enquanto ainda estiverem disponíveis", "Restaurar"),
            ReminderSeed(1, 500001, "Imagens recuperáveis foram encontradas na varredura 🖼️", "Salvar"),
            ReminderSeed(1, 500001, "Os resultados da varredura estão prontos para revisar fotos", "Ver"),
            ReminderSeed(1, 500001, "Verifique agora quais fotos ainda podem ser recuperadas", "Verificar"),
            ReminderSeed(2, 500002, "Vídeos recuperáveis foram encontrados na varredura 🎬", "Salvar"),
            ReminderSeed(2, 500002, "Restaure vídeos perdidos a partir dos resultados recentes", "Restaurar"),
            ReminderSeed(2, 500002, "Os vídeos ainda podem estar disponíveis para revisão", "Ver"),
            ReminderSeed(2, 500002, "Vídeos grandes foram encontrados e podem ser recuperados 🎞️", "Recuperar"),
            ReminderSeed(2, 500002, "Proteja vídeos recuperáveis antes que sejam removidos", "Proteger"),
            ReminderSeed(4, 500004, "Documentos recuperáveis foram encontrados na varredura 📄", "Restaurar"),
            ReminderSeed(4, 500004, "Os arquivos podem ser substituídos se você não os salvar", "Salvar"),
            ReminderSeed(4, 500004, "Documentos perdidos estão prontos para recuperação", "Recuperar"),
            ReminderSeed(4, 500004, "Os arquivos analisados estão prontos para revisão", "Ver"),
            ReminderSeed(4, 500004, "Verifique agora quais arquivos excluídos podem ser restaurados 📁", "Verificar"),
        ),
    )
}
