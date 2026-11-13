package com.imhamiddev.musicplayer.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.data.AppLanguage
import com.imhamiddev.musicplayer.data.Song

/**
 * Every piece of UI text, in English and Persian side by side. [rtl] is true for Persian.
 * "‌" is the zero-width non-joiner Persian needs inside compound words.
 */
class Strings(val rtl: Boolean) {

    private fun t(en: String, fa: String): String = if (rtl) fa else en

    // ---- Formatting helpers --------------------------------------------------------------------

    /** Latin digits become Persian digits when the language is Persian. */
    fun digits(s: String): String {
        if (!rtl) return s
        val out = StringBuilder(s.length)
        for (ch in s) out.append(if (ch in '0'..'9') '۰' + (ch - '0') else ch)
        return out.toString()
    }

    fun num(n: Int): String = digits(n.toString())

    /** m:ss */
    fun time(ms: Long): String = digits(fmt(ms))

    /** Letter spacing breaks the joined letters of Persian script, so it is dropped there. */
    fun ls(x: TextUnit): TextUnit = if (rtl) 0.sp else x

    /** Same, but also for Persian/Arabic song titles shown inside the English UI. */
    fun ls(text: String, x: TextUnit): TextUnit =
        if (rtl || text.any { it in '\u0600'..'\u06FF' }) 0.sp else x

    /** "1h 25m" / "12m" / "40s" */
    fun duration(ms: Long): String {
        val totalSec = ms / 1000L
        if (totalSec < 60L) return t("${totalSec}s", "${digits(totalSec.toString())} ثانیه")
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        if (h == 0L) return t("${m}m", "${digits(m.toString())} دقیقه")
        return t("${h}h ${m}m", "${digits(h.toString())} ساعت و ${digits(m.toString())} دقیقه")
    }

    fun artist(a: String): String = if (a == Song.UNKNOWN_ARTIST) unknownArtist else a
    fun album(a: String): String = if (a == Song.UNKNOWN_ALBUM) unknownAlbum else a

    // ---- Library -------------------------------------------------------------------------------

    val library get() = t("Library", "کتابخانه")
    val tabAll get() = t("All", "همه")
    val tabFolders get() = t("Folders", "پوشه‌ها")
    val tabFavorite get() = t("Favorite", "علاقه‌مندی")
    val tabRecent get() = t("Recent", "اخیر")
    val tabCategories get() = t("Lists", "دسته‌ها")
    val searchHint get() = t("Search songs, artists, albums", "جستجوی آهنگ، خواننده یا آلبوم")

    fun results(n: Int): String = t(if (n == 1) "1 result" else "$n results", "${num(n)} نتیجه")
    fun songs(n: Int): String = t(if (n == 1) "1 song" else "$n songs", "${num(n)} آهنگ")
    fun folders(n: Int): String = t(if (n == 1) "1 folder" else "$n folders", "${num(n)} پوشه")
    fun categoriesCount(n: Int): String = t(if (n == 1) "1 category" else "$n categories", "${num(n)} دسته")
    fun favorites(n: Int): String = t(if (n == 1) "1 favorite" else "$n favorites", "${num(n)} علاقه‌مندی")

    val emptyMusicTitle get() = t("No music found", "موزیکی پیدا نشد")
    val emptyMusicBody get() = t(
        "Add some audio files to your device and they will show up here.",
        "چند فایل صوتی به دستگاه خود اضافه کنید تا اینجا نمایش داده شوند.",
    )
    val emptyFoldersTitle get() = t("No folders", "پوشه‌ای وجود ندارد")
    val emptyFoldersBody get() = t(
        "Folders that contain music will appear here.",
        "پوشه‌هایی که موزیک دارند اینجا نمایش داده می‌شوند.",
    )
    val emptyFolderTitle get() = t("Empty folder", "پوشه‌ی خالی")
    val emptyFolderBody get() = t("There is no music in this folder.", "در این پوشه موزیکی وجود ندارد.")
    val emptyFavTitle get() = t("No favorites yet", "هنوز علاقه‌مندی‌ای نیست")
    val emptyFavBody get() = t(
        "Tap the heart on any song to keep it here.",
        "روی قلب هر آهنگ بزنید تا اینجا نگه داشته شود.",
    )
    val emptyRecentTitle get() = t("Nothing played yet", "هنوز چیزی پخش نشده")
    val emptyRecentBody get() = t("Songs you play will show up here.", "آهنگ‌هایی که پخش می‌کنید اینجا نمایش داده می‌شوند.")
    val noResultsTitle get() = t("No results", "نتیجه‌ای پیدا نشد")
    fun noResultsBody(q: String): String = t(
        "Nothing matches “$q”. Try a different title or artist.",
        "موردی با «$q» مطابقت ندارد. عنوان یا خواننده‌ی دیگری را امتحان کنید.",
    )

    val sortBy get() = t("Sort by", "مرتب‌سازی بر اساس")
    val sortAlpha get() = t("Alphabetical", "حروف الفبا")
    val sortAZ get() = t("A to Z", "الف تا ی")
    val sortZA get() = t("Z to A", "ی تا الف")
    val sortDate get() = t("Date added", "تاریخ افزودن")
    val sortOldest get() = t("Oldest first", "قدیمی‌ترین اول")
    val sortNewest get() = t("Newest first", "جدیدترین اول")

    val storage get() = t("Storage", "حافظه")
    val internalStorage get() = t("Internal storage", "حافظه‌ی داخلی")
    val unknownArtist get() = t("Unknown artist", "خواننده‌ی ناشناس")
    val unknownAlbum get() = t("Unknown album", "آلبوم ناشناس")

    val locateSong get() = t("Go to the playing song", "رفتن به آهنگ در حال پخش")

    // ---- Player --------------------------------------------------------------------------------

    val nowPlaying get() = t("NOW PLAYING", "در حال پخش")

    // ---- Categories ----------------------------------------------------------------------------

    val categories get() = t("Categories", "دسته‌بندی‌ها")
    val newCategory get() = t("New category", "دسته‌بندی جدید")
    val categoryName get() = t("Category name", "نام دسته‌بندی")
    val categoryNameHint get() = t("e.g. Gym, Chill", "مثلاً باشگاه، آرامش")
    val create get() = t("Create", "ساخت")
    val addToCategory get() = t("Add to category", "افزودن به دسته‌بندی")
    val addToCategoryNote get() = t("Tap a category to add or remove this song.", "برای افزودن یا حذف این آهنگ، روی یک دسته‌بندی بزنید.")
    val noCategoriesYet get() = t("No categories yet. Create one below.", "هنوز دسته‌بندی‌ای ندارید. پایین یکی بسازید.")
    val addSongs get() = t("Add songs", "افزودن آهنگ")
    val renameCategory get() = t("Rename category", "تغییر نام دسته‌بندی")
    val deleteCategory get() = t("Delete category", "حذف دسته‌بندی")
    fun deleteCategoryBody(name: String) = t(
        "\"$name\" will be deleted. Your songs stay on your device.",
        "«$name» حذف می‌شود. آهنگ‌های شما روی دستگاه باقی می‌مانند.",
    )
    val emptyCategoriesTitle get() = t("No categories", "دسته‌بندی‌ای نیست")
    val emptyCategoriesBody get() = t("Create one, like Gym or Chill, and put your songs in it.", "یکی بسازید، مثل باشگاه یا آرامش، و آهنگ‌هایتان را داخلش بگذارید.")
    val emptyCategoryTitle get() = t("This category is empty", "این دسته‌بندی خالی است")
    val emptyCategoryBody get() = t("Use Add songs, or the ⋮ menu on any song.", "از «افزودن آهنگ» یا منوی ⋮ هر آهنگ استفاده کنید.")
    val done get() = t("Done", "تمام")

    // ---- Song menu and dialogs -----------------------------------------------------------------

    val playNext get() = t("Play next", "پخش بعدی")
    val addFav get() = t("Add to favorites", "افزودن به علاقه‌مندی‌ها")
    val removeFav get() = t("Remove from favorites", "حذف از علاقه‌مندی‌ها")
    val rename get() = t("Rename", "تغییر نام")
    val share get() = t("Share", "اشتراک‌گذاری")
    val details get() = t("Details", "جزئیات")
    val delete get() = t("Delete", "حذف")
    val save get() = t("Save", "ذخیره")
    val cancel get() = t("Cancel", "انصراف")
    val close get() = t("Close", "بستن")

    val renameNote get() = t(
        "Changes the title shown in this app. The file on your device is not modified.",
        "عنوان نمایش‌داده‌شده در این برنامه تغییر می‌کند. فایل روی دستگاه شما تغییری نمی‌کند.",
    )

    val detailTitle get() = t("Title", "عنوان")
    val detailArtist get() = t("Artist", "خواننده")
    val detailAlbum get() = t("Album", "آلبوم")
    val detailDuration get() = t("Duration", "مدت")
    val detailFormat get() = t("Format", "فرمت")
    val detailAdded get() = t("Added", "تاریخ افزودن")
    val detailLocation get() = t("Location", "مسیر")
    val unknown get() = t("Unknown", "نامشخص")

    val deleteTitle get() = t("Delete song?", "آهنگ حذف شود؟")
    fun deleteBody(title: String): String = t(
        "“$title” will be permanently deleted from your device.",
        "«$title» برای همیشه از دستگاه شما حذف می‌شود.",
    )
    val cantDelete get() = t("Can't delete this file", "امکان حذف این فایل وجود ندارد")

    // ---- Permissions and lock screen -----------------------------------------------------------

    val lockPlayerTitle get() = t("Lock screen player", "پخش‌کننده‌ی صفحه‌ی قفل")
    val lockPlayerBody get() = t(
        "Allow full-screen notifications so the player can take over your lock screen while music is playing.",
        "اعلان‌های تمام‌صفحه را مجاز کنید تا هنگام پخش موزیک، پخش‌کننده روی صفحه‌ی قفل نمایش داده شود.",
    )
    val allow get() = t("Allow", "اجازه")
    val notNow get() = t("Not now", "فعلاً نه")
    val swipeToUnlock get() = t("Swipe up to unlock", "برای باز کردن قفل، به بالا بکشید")
    val nowPlayingShort get() = t("Now playing", "در حال پخش")
    val appName get() = t("Music Player", "موزیک پلیر")

    val permTitle get() = t("Your music, beautifully played", "موزیک شما، زیبا پخش می‌شود")
    val permDenied get() = t(
        "Access was denied. Open settings and allow audio access to see your songs.",
        "دسترسی رد شد. تنظیمات را باز کنید و اجازه‌ی دسترسی به فایل‌های صوتی را بدهید.",
    )
    val permAsk get() = t(
        "Allow access to the audio files on your device. Videos are never shown.",
        "به فایل‌های صوتی دستگاه خود دسترسی بدهید. ویدیوها هرگز نمایش داده نمی‌شوند.",
    )
    val openSettings get() = t("Open settings", "باز کردن تنظیمات")
    val allowAccess get() = t("Allow access", "دادن دسترسی")

    // ---- Update banner -------------------------------------------------------------------------

    fun updateAvailable(v: String): String = t("Version $v is available", "نسخه‌ی $v در دسترس است")
    fun updateDownloading(percent: Int): String = t("Downloading… $percent%", "در حال دانلود… ${num(percent)}٪")
    fun updateReady(v: String): String = t("Version $v is ready to install", "نسخه‌ی $v آماده‌ی نصب است")
    val updateFailed get() = t("Download failed", "دانلود ناموفق بود")
    val download get() = t("Download", "دانلود")
    val install get() = t("Install", "نصب")
    val retry get() = t("Retry", "تلاش دوباره")

    // ---- Settings ------------------------------------------------------------------------------

    val settings get() = t("Settings", "تنظیمات")
    val settingsLanguage get() = t("Language", "زبان")
    val settingsLanguageHint get() = t("Changes the language of the whole app.", "زبان کل برنامه را تغییر می‌دهد.")
    val settingsProgress get() = t("Song progress", "نوار پیشرفت آهنگ")
    val settingsProgressHint get() = t(
        "How the progress is drawn in the player. You can still drag it to seek.",
        "ظاهر نوار پیشرفت در صفحه‌ی پخش. همچنان می‌توانید با کشیدن آن جلو و عقب بروید.",
    )
    val progressWaveform get() = t("Waveform", "موج صوتی")
    val progressLine get() = t("Simple line", "خط ساده")
    val settingsTheme get() = t("Theme", "پوسته")
    val themeSystem get() = t("System", "سیستم")
    val themeLight get() = t("Light", "روشن")
    val themeDark get() = t("Dark", "تاریک")

    // ---- Listening stats -----------------------------------------------------------------------

    val statsTitle get() = t("Listening stats", "آمار گوش دادن")
    val periodToday get() = t("Today", "امروز")
    val periodWeek get() = t("Last 7 days", "۷ روز اخیر")
    val periodMonth get() = t("Last 30 days", "۳۰ روز اخیر")
    val statsSongsPlayed get() = t("songs played", "آهنگ پخش‌شده")
    val statsTime get() = t("listening time", "زمان گوش دادن")
    val statsTop get() = t("Top 10 songs", "۱۰ آهنگ برتر")
    fun playsCount(n: Int): String = t(if (n == 1) "1 play" else "$n plays", "${num(n)} بار")
    val statsEmptyTitle get() = t("No listening data yet", "هنوز آماری ثبت نشده")
    val statsEmptyBody get() = t(
        "Play some music and your stats will show up here.",
        "چند آهنگ پخش کنید تا آمار شما اینجا نمایش داده شود.",
    )
}

val EnglishStrings = Strings(rtl = false)
val PersianStrings = Strings(rtl = true)

fun stringsFor(language: AppLanguage): Strings =
    if (language == AppLanguage.Persian) PersianStrings else EnglishStrings

val LocalStrings = staticCompositionLocalOf { EnglishStrings }
