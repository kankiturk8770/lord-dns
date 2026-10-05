# Lord DNS

DNS چنجر اندرویدی با Kotlin و Jetpack Compose. بدون روت کار می‌کنه (از VpnService محلی استفاده می‌کنه و فقط ترافیک DNS رو جابه‌جا می‌کنه، بقیه‌ی اینترنت دست نمی‌خوره).

## امکانات
- اتصال با یک لمس + تایمر اتصال
- DNSهای آماده: Cloudflare، Google، Electro، Shecan، 403.online، Begzar، AdGuard، Quad9، OpenDNS
- تست پینگ واقعی DNS (زمان پاسخ به کوئری)، فیلتر سریع‌ترین / ضد تبلیغ / ایرانی / جهانی / دلخواه
- افزودن و حذف DNS دلخواه (IPv4 و IPv6)
- اتصال خودکار (موقع باز شدن اپ و بعد از ری‌استارت گوشی)
- انتخاب خودکار سریع‌ترین DNS
- پشتیبانی IPv6
- DNS over HTTPS برای سرورهای دارای DoH
- تم‌ها: System، Light، Dark، Surfshark، Proton، Midnight، Sunset

## ساخت APK

### روش ۱: Android Studio
1. پوشه رو با Android Studio (نسخه Koala یا جدیدتر) باز کن.
2. صبر کن Gradle Sync تموم بشه (اینترنت لازمه).
3. منوی Build → Build App Bundle(s) / APK(s) → Build APK(s).
4. فایل APK توی `app/build/outputs/apk/` ساخته می‌شه.

### روش ۲: GitHub (بدون نصب چیزی)
1. پروژه رو توی یه ریپوی GitHub آپلود کن.
2. تب Actions → «Build Lord DNS APK» خودکار اجرا می‌شه.
3. وقتی تموم شد، از بخش Artifacts فایل `LordDNS-apk` رو دانلود کن.

APK ریلیز با کلید debug امضا شده تا مستقیم نصب بشه. برای انتشار توی گوگل‌پلی کلید خودت رو بذار.
