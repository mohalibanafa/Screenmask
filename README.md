# قناع الشاشة | Screen Mask `v3.3.2` 🛡️📱

تطبيق أندرويد متطور، خفيف، ومفتوح المصدر ولا يتطلب صلاحيات الروت (No Root)، مصمم لتغطية وتعتيم المناطق المتضررة من الشاشة مثل البكسلات الميتة (Dead Pixels)، الشروخ والكسور، تسريب الإضاءة، أو وميض وحرق الشاشة، من خلال أقنعة ومجسمات عائمة مستقلة مع تحكم مرن وكامل.

---

## 🌟 أبرز الوظائف والمميزات (Features)

- **بدون روت (No Root Required)**: يعمل بشكل رسمي وآمن تماماً عبر واجهات أندرويد القياسية لنظام النوافذ العائمة (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`).
- **أشكال هندسية متعددة وسريعة**:
  - ⬛ **مستطيل (Rectangle)**: لتغطية الخطوط أو أطراف الشاشة أو حواف التطبيقات.
  - 🔵 **دائرة (Circle)**: لتغطية النقاط المحروقة أو البقع الدائرية.
  - ▱ **شكل حر متوازي الأضلاع (Free Quad / Parallelogram)**: مع 4 مقابض مستقلة للأركان لتغطية الشروخ والكسور المائلة بدقة متناهية.
- **وضع اللمس المباشر (Touch-Through Mode)**:
  - **عند التفعيل**: يسمح للمسات والنقرات بالمرور من خلال المجسم والتفاعل مع التطبيقات والشاشة تحته بسلاسة تامة.
  - **عند التعطيل**: يحجب اللمس تماماً ويعترض أي نقرات عشوائية لحماية المناطق الحساسة من اللمس الخاطئ.
- **وضع المعاينة المصغر (Preview Eye Mode)**: إمكانية تصغير شريط التحكم بنقرة واحدة إلى فقاعة عائمة خفيفة بأيقونة المعاينة (👁️) لمشاهدة شكل الشاشة في الوقت الفعلي أثناء استخدام الهاتف بدون عوائق.
- **شريط تحكم عائم ذكي (Floating Control Toolbar)**:
  - إضافة الأشكال وتكرارها وحذفها مباشرة خارج التطبيق.
  - منتقي ألوان متقدم يدعم لوحة الألوان المخصصة (Hex) وشريط الشفافية (Alpha).
  - سلايدرات دقيقة لضبط العرض والارتفاع والموضع الأفقي والعمودي.
- **حفظ تلقائي ودائم في قاعدة بيانات Room**: تُحفظ أبعاد ومواقع المجسمات كنسب مئوية (Screen Ratios) لضمان ثبات موقعها حتى بعد إعادة تشغيل الهاتف، تغيير اتجاه الشاشة (أفقي/عمودي)، أو اختلاف أشرطة النظام.
- **أداء خفيف وخدمة مستمرة**: يعمل عبر خدمة أمامية موثوقة (Foreground Service) مع إشعار دائم وبلاطة إعدادات سريعة (Quick Settings Tile).

---

## 📱 English Overview

**Screen Mask** is a high-performance Android overlay utility that places customizable shapes over damaged, burned-in, flickering, or cracked screen regions.

- **Dynamic Shapes**: Rectangles, Circles, and Free-Corner Parallelograms with interactive handles.
- **Touch-Through & Block Support**: Full control over whether touch inputs interact with underlying apps or get shielded.
- **Minimized Preview Bubble**: Collapse the toolbar into an unobtrusive floating eye icon preview badge.
- **Precision Customization**: Full ARGB Color Picker, Opacity slider, X/Y placement sliders, and shape duplication.
- **Zero Privacy Intrusions**: Does not capture screens, read accessibility events, or log private data.

---

## 🛠️ بنية المشروع والتقنيات المستخدمة (Architecture)

- **لغة البرمجة**: Kotlin 100%
- **واجهة المستخدم**: Jetpack Compose (داخل التطبيق) + Custom Canvas Views (للطبقات العائمة)
- **قاعدة البيانات**: Room Database (`MaskEntity`, `MaskDao`, `AppDatabase`)
- **الخدمات الخلفية**: `OverlayService` (Foreground Service) + `ScreenMaskTileService` (Quick Settings)

```
app/src/main/java/com/example/
├── MainActivity.kt                  # واجهة التطبيق الرئيسية (Compose)
├── data/
│   ├── MaskEntity.kt                # كائن بيانات الشكل وموقعه وخصائصه
│   ├── MaskDao.kt                   # عمليات قاعدة البيانات
│   ├── AppDatabase.kt               # قاعدة بيانات Room
│   ├── MaskRepository.kt            # مستودع إدارة البيانات
│   └── AppSettingsRepository.kt     # إعدادات التطبيق والتفضيلات
├── service/
│   ├── OverlayService.kt            # الخدمة الأمامية لإدارة النوافذ العائمة
│   ├── OverlayMaskView.kt           # الرسم التفاعلي للأشكال ومقابض التحكم
│   ├── FloatingControlView.kt       # شريط الأدوات ولوحة الإعدادات العائمة
│   ├── AdvancedColorPickerView.kt   # منتقي الألوان والشفافية المتقدم
│   └── ScreenMaskTileService.kt     # خدمة الإعدادات السريعة (Tile)
├── receiver/
│   └── BootReceiver.kt              # استعادة الأشكال تلقائياً عند إقلاع الجهاز
└── ui/
    └── ScreenMaskApp.kt             # واجهة إدارة الأشكال والإعدادات
```

---

## 🚀 البناء والتثبيت (Build)

```bash
gradle :app:assembleDebug
```
ملف الـ APK الناتج: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 الصلاحيات والأمان (Permissions)

1. **الظهور فوق التطبيقات الأخرى (`SYSTEM_ALERT_WINDOW`)**: لعرض وتثبيت الأقنعة العائمة على الشاشة.
2. **الخدمة الأمامية (`FOREGROUND_SERVICE`)**: لضمان استمرار عمل الأقنعة دون إغلاقها من قِبل موفر الطاقة في النظام.
3. **تشغيل عند بدء التشغيل (`RECEIVE_BOOT_COMPLETED`)**: لتفعيل الأقنعة تلقائياً بعد إعادة التشغيل إذا تم تفعيل الخيار.

---

## 📄 الترخيص (License)
هذا المشروع مفتوح المصدر ومرخص تحت رخصة [MIT License](LICENSE).
