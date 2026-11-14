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

    val pinToTop get() = t("Pin to top", "سنجاق به بالا")
    val unpin get() = t("Unpin", "برداشتن سنجاق")
    val chooseIcon get() = t("Icon", "آیکون")
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
        "The song's file on your device is renamed too.",
        "نام فایل آهنگ روی دستگاه شما هم تغییر می‌کند.",
    )
    val renameFailed get() = t("Couldn't rename the file.", "تغییر نام فایل انجام نشد.")

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

    val groupPlayback get() = t("Playback", "پخش")
    val groupApp get() = t("App", "برنامه")
    val groupLibrary get() = t("Library", "کتابخانه")
    val settingsAppearance get() = t("Appearance", "ظاهر")
    val settingsAppearanceHint get() = t("Language, song progress look and theme.", "زبان، ظاهر نوار آهنگ و پوسته.")
    fun hiddenCount(n: Int) = t(if (n == 1) "1 folder hidden" else "$n folders hidden", "${num(n)} پوشه پنهان")

    // ---- Hidden folders ------------------------------------------------------------------------

    val hideFolder get() = t("Hide folder", "پنهان کردن پوشه")
    fun hideFolderBody(name: String, n: Int) = t(
        "The $n songs in \"$name\" will not show up in the app. You can bring the folder back from Settings.",
        "${num(n)} آهنگِ «$name» در برنامه نمایش داده نمی‌شود. می‌توانید پوشه را از تنظیمات برگردانید.",
    )
    val hide get() = t("Hide", "پنهان کن")
    val show get() = t("Show", "نمایش")
    val hiddenFoldersTitle get() = t("Hidden folders", "پوشه‌های پنهان")
    val hiddenFoldersHint get() = t(
        "Songs in these folders do not show up in the app. Long-press a folder, or open it and tap the eye, to hide it.",
        "آهنگ‌های این پوشه‌ها در برنامه نمایش داده نمی‌شوند. برای پنهان کردن، روی پوشه نگه دارید یا آن را باز کنید و روی آیکون چشم بزنید.",
    )
    val noHiddenFolders get() = t("No hidden folders.", "پوشه‌ای پنهان نشده است.")

    // ---- Sound and sleep timer -----------------------------------------------------------------

    val soundTitle get() = t("Sound & timer", "صدا و تایمر")
    val soundEntryHint get() = t("Sleep timer, equalizer, bass, speed and smooth song changes.", "تایمر خواب، اکولایزر، بیس، سرعت و تغییر نرم آهنگ.")
    val sleepTitle get() = t("Sleep timer", "تایمر خواب")
    val sleepHint get() = t("Stops the music when the time is up, fading it out gently.", "وقتی زمان تمام شد، موسیقی با کم‌شدن نرم صدا قطع می‌شود.")
    fun sleepMinutes(n: Int) = t("$n min", "${num(n)} دقیقه")
    val sleepEndOfSong get() = t("End of this song", "پایان این آهنگ")
    val sleepOff get() = t("Turn off", "خاموش کردن")
    fun sleepStopsIn(clock: String) = t("Stops in $clock", "توقف تا ${digits(clock)} دیگر")
    val sleepStopsAtEnd get() = t("Stops when this song ends", "با پایان این آهنگ متوقف می‌شود")
    val speedTitle get() = t("Playback speed", "سرعت پخش")
    val speedHint get() = t("Faster or slower, without changing the voice pitch.", "سریع‌تر یا کندتر، بدون تغییر در زیر و بمی صدا.")
    fun speedLabel(v: Float): String {
        val txt = if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString().trimEnd('0')
        return digits(txt) + "×"
    }
    val eqTitle get() = t("Equalizer", "اکولایزر")
    val eqUnsupported get() = t("This device does not offer an equalizer.", "این دستگاه اکولایزر ندارد.")
    val presetFlat get() = t("Flat", "عادی")
    val presetBass get() = t("Bass", "بیس")
    val presetTreble get() = t("Treble", "تیز")
    val presetVocal get() = t("Vocal", "صدا")
    val presetRock get() = t("Rock", "راک")
    val presetChill get() = t("Chill", "آرام")
    val bassTitle get() = t("Bass boost", "تقویت بیس")
    val bassUnsupported get() = t("This device does not support bass boost.", "این دستگاه از تقویت بیس پشتیبانی نمی‌کند.")
    fun freqLabel(hz: Int): String =
        if (hz >= 1000) digits(String.format(java.util.Locale.US, "%.1f", hz / 1000f).removeSuffix(".0")) + t(" kHz", " کیلوهرتز")
        else digits(hz.toString()) + t(" Hz", " هرتز")
    val crossfadeTitle get() = t("Smooth song changes", "تغییر نرم آهنگ")
    val crossfadeHint get() = t(
        "Fades the end of a song out and the next one in. The two songs do not overlap.",
        "پایان آهنگ را کم و آهنگ بعدی را به‌تدریج زیاد می‌کند. دو آهنگ روی هم پخش نمی‌شوند.",
    )
    fun crossfadeSecs(n: Int) = t("$n s", "${num(n)} ثانیه")

    // ---- Selecting several songs ---------------------------------------------------------------

    fun selectedCount(n: Int) = t("$n selected", "${num(n)} انتخاب‌شده")
    val selectAll get() = t("All", "همه")
    val pillCategory get() = t("Category", "دسته‌بندی")
    val pillFavorite get() = t("Favorite", "علاقه‌مندی")
    val pillRemove get() = t("Remove", "حذف از دسته")

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
