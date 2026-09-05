# Bebek Beslenme Takibi — Masaüstü Uygulaması

© 2026 by AKANSEL

Bebeğin beslenme, bez, uyku, büyüme, vitamin/ilaç ve hatırlatıcı kayıtlarını tutan
**saf Java (Swing)** masaüstü uygulaması. HTML, WebView veya tarayıcı motoru
kullanılmaz; tüm arayüz native Swing bileşenleriyle çizilir.

- **Dış bağımlılık yok.** Maven/Gradle gerekmez, indirilen hiçbir kütüphane yoktur.
  JSON okuyucu/yazıcı, ikonlar ve grafikler dahil her şey proje içinde.
- **Veri:** kullanıcı klasöründe her veri türü için ayrı JSON dosyası +
  istenildiğinde CSV dışa aktarım ve tam JSON yedeği.
- **Dağıtım:** `jpackage` ile kendi Java çalışma zamanını içeren gerçek `.exe`.

---

## Hızlı başlangıç

| Komut | Ne yapar |
|---|---|
| `derle.bat` | Kaynakları derler, `build\jar\BebekTakip.jar` ve uygulama simgesini üretir |
| `calistir.bat` | Uygulamayı çalıştırır (JAR yoksa önce derler) |
| `paketle.bat` | Kendi Java'sını içeren `.exe` üretir |
| `paketle.bat kurulum` | Kurulum dosyası üretir (WiX Toolset 3.x gerekir) |
| `test.bat` | Veri ve işlev testlerini çalıştırır |
| `test.bat onizleme` | Ekranların PNG görüntüsünü `build\onizleme` içine yazar |

Geliştirme için **JDK 17 veya üstü** gerekir ([adoptium.net](https://adoptium.net)).
Betikler JDK'yı önce `JAVA_HOME`, sonra bilinen kurulum klasörlerinden bulur.

Paketlenmiş `.exe` çalışırken makinede Java kurulu olması **gerekmez**.

---

## ⚠️ Klasör adı uyarısı (paketlenmiş .exe için)

Bu bilgisayarın Windows ANSI kod sayfası **1252**'dir ve `ı`, `ğ`, `ş`, `İ` gibi
karakterleri temsil edemez. `jpackage` ile üretilen `.exe`, kendi klasörünü bu kod
sayfası üzerinden bulduğu için **yolunda bu karakterlerden biri geçen bir klasörden
çalışmaz** (`could not find java.dll` hatası verir).

Bu yüzden `paketle.bat` çıktıyı varsayılan olarak **`%USERPROFILE%\BebekTakip`**
klasörüne koyar. Başka bir yer isterseniz:

```bat
paketle.bat "D:\Programlar"
```

Verdiğiniz yolda da bu karakterler bulunmamalıdır.

> Not: Bu kısıt yalnızca paketlenmiş `.exe` içindir. `calistir.bat` ile JAR olarak
> çalıştırmak, proje `Akansel Uygulamaları` klasöründe dursa bile sorunsuz çalışır.

---

## Kullanım

### Ekranlar

- **Özet** — bugünün ölçümleri (kayıt, mama, çiş, kaka, emzirme, uyku, bekleyen
  hatırlatıcı, son kilo) ve son 5 kayıt.
- **Kayıtlar** — tüm beslenme/bez kayıtlarının düzenlenebilir tablosu ve arama.
  Çift günlerin satırları (ayın 2, 4, 6…sı) bir ton koyu zeminle çizilir; böylece
  günler kaydırırken birbirinden ayrılır.
- **Uyku Takibi** — "Uyku Başlat" o anın saatiyle açık bir kayıt açar, "Uykuyu
  Bitir" kapatır. Saatler tabloda elle düzeltilebilir; bitişi başlangıçtan küçük
  girilen uyku (23:30 → 06:15) ertesi güne sayılır.
- **Büyüme** — kilo, boy ve baş çevresi. "Yeni Ölçüm" günün tarihiyle boş satır
  açar, değerler hücreye yazılır.
- **Vitamin & İlaç** — bebeğe verilenlerin günlüğü. "Vitamin / İlaç Ekle" o anın
  tarih ve saatiyle satır açar; ad, doz ve not hücreye yazılır.
- **Hatırlatıcılar** — aşı, ilaç, kontrol. En yakın tarihli üstte durur; soldaki
  kutu işaretlenince tamamlanmış sayılır.
- **İstatistikler** — son 7 günün kayıt, mama ve uyku grafikleri, gün gün toplam
  mama listesi (son 30 gün) ve genel toplamlar.

### Tabloda düzenleme

| İşlem | Nasıl |
|---|---|
| Çiş / Kaka / Sağ / Sol / Mama işaretleme | Kutuya tıklayın (veya hücreyi seçip **Boşluk**) |
| Tarih değiştirme | Tarih hücresine tıklayın, açılan takvimden seçin |
| Saat değiştirme | Hücreye çift tıklayıp `SS:DD` yazın (geçersiz saat kırmızı çerçeveyle reddedilir) |
| Metin ve sayı alanları | Hücreye çift tıklayıp yazın |
| Satır silme | Satırın üstüne gelin, sağdaki çöp kutusuna tıklayın (veya **Delete**) |

Aynı düzen uyku, büyüme, vitamin/ilaç ve hatırlatıcı tablolarında da geçerlidir.

Mama miktarı, mama notundaki ilk sayıdan okunur: `120`, `120 ml`, `90ml`, `7,5`
yazımlarının hepsi tanınır. Mama kutusu işaretli değilse miktar toplama katılmaz.

Beslenme tablosundaki değişiklikler yazmayı bıraktıktan ~0,6 saniye sonra otomatik
kaydedilir; öbür ekranlardaki değişiklikler anında yazılır. Pencere kapanırken de
kaydedilir.

### Sistem tepsisi

Görev çubuğu tepsisindeki bebek simgesine çift tıklamak pencereyi öne getirir;
sağ tık menüsünden pencere açılmadan yeni kayıt eklenebilir, uyku ekranına
geçilebilir veya uygulamadan çıkılabilir.

### Klavye kısayolları

| Kısayol | İşlev |
|---|---|
| `Ctrl+N` | Yeni kayıt |
| `Ctrl+F` | Kayıtlar ekranında aramaya git |
| `Ctrl+S` | Hemen kaydet |
| `Ctrl+E` | CSV olarak dışa aktar |
| `Ctrl+1 … 7` | Özet / Kayıtlar / Uyku / Büyüme / Vitamin & İlaç / Hatırlatıcılar / İstatistikler |
| `Boşluk` | Seçili işaret kutusunu değiştir |
| `Delete` | Seçili satırı sil |

---

## Veri

Her veri türü kendi dosyasında tutulur:

```
%USERPROFILE%\.bebek-takip\kayitlar.json          beslenme ve bez
%USERPROFILE%\.bebek-takip\uykular.json           uyku
%USERPROFILE%\.bebek-takip\buyumeler.json         büyüme ölçümleri
%USERPROFILE%\.bebek-takip\ilaclar.json           vitamin ve ilaçlar
%USERPROFILE%\.bebek-takip\hatirlaticilar.json    hatırlatıcılar
```

Yazma **atomiktir**: önce `.tmp` dosyasına yazılır, sonra yerine taşınır;
bir önceki sürüm `.bak` olarak saklanır. Dosyalardan biri bozulursa uygulama
çökmez, uyarı gösterip o listeyi boş açar; öbür veri türleri etkilenmez.

**Dışa Aktar** menüsünden:

- **CSV** — beslenme kayıtları; noktalı virgülle ayrılmış, BOM'lu UTF-8. Türkçe
  Excel'de çift tıklayınca doğru açılır; sayısal mama miktarı ayrı bir sütun
  olarak da yazılır.
- **JSON yedek** — dört veri türünü birden içeren tam yedek.
- **JSON'dan içe aktar** — yedekleri geri yükler. Eski, yalnızca beslenme içeren
  yedekler ve tarayıcı sürümünün `date`/`time` alan adları da tanınır. İçe
  aktarırken "mevcuda ekle" veya "hepsini değiştir" seçebilirsiniz.

---

## Proje yapısı

```
java/
  src/com/akansel/bebektakip/
    App.java                    giriş noktası, görünüm ve yazı tipi ayarları
    model/
      Kayit.java                beslenme kaydı + tarih/saat/sayı çözümleme
      UykuKayit.java            uyku kaydı, gece aşan süre hesabı
      BuyumeKayit.java          kilo / boy / baş çevresi ölçümü
      IlacKayit.java            verilen vitamin / ilaç kaydı
      Hatirlatici.java          aşı, ilaç, randevu hatırlatması
    depo/
      Json.java                 bağımlılıksız JSON okuyucu/yazıcı
      KayitDeposu.java          yükleme, atomik kaydetme, yedek, sorgular
      DisaAktarim.java          CSV ve JSON dışa/içe aktarım
    ui/
      Tema.java                 renkler, yazı tipleri, Türkçe biçimlendirme
      Ikonlar.java              vektör ikonlar (emoji/resim dosyası yok)
      UygulamaIkonu.java        uygulama simgesi çizimi
      AnaPencere.java           pencere, üst şerit, menü, kısayollar, tepsi
      YanMenu.java              sol gezinme şeridi
      OzetPanel.java            özet ekranı
      KayitlarPanel.java        kayıt tablosu ekranı
      UykuPanel.java            uyku ekranı
      BuyumePanel.java          büyüme ekranı
      IlacPanel.java            vitamin ve ilaç ekranı
      HatirlaticiPanel.java     hatırlatıcı ekranı
      IstatistiklerPanel.java   grafikler ve toplamlar
      bilesen/                  kart, buton, arama, takvim, grafik, boş durum
      tablo/                    tablo modelleri, hücre çizicileri, tablo davranışı
    arac/IkonUret.java          jpackage için .ico üretimi
  test/                         veri testleri, işlev testleri, ekran görüntüsü aracı
  derle.bat calistir.bat paketle.bat test.bat jdk-bul.bat
```

Arayüzde emoji veya resim dosyası kullanılmaz; bütün ikonlar `Graphics2D` ile
çizildiği için her Windows kurulumunda aynı görünür ve DPI ölçeklemede bozulmaz.

---

## Tasarım notları

- Tarih girişi metin yerine **açılır takvimle** yapılır; geçersiz tarih girilemez.
- Saat elle yazılır ama doğrulanır — geçersiz değer kırmızı çerçeveyle reddedilir.
- Silmeden önce **onay** sorulur; bu kural bütün ekranlarda aynıdır.
- Kayıtlar tarih + saate göre **sıralı** tutulur, her kaydın kendi kimliği (`id`) vardır.
  Hatırlatıcılar ters yönde sıralanır: en yakın iş en üstte.
- Tablolar ekrandaki satırlardan değil **doğrudan veri modelinden** çalışır. Bu ayrım
  önemli: arama açıkken bir hücreyi düzenlemek yalnızca o kaydı değiştirir, filtrenin
  dışında kalan kayıtlara dokunmaz.
- Gece yarısı geçildiğinde "bugün" ölçümleri kendiliğinden tazelenir.
- Uykuda bitiş saati başlangıçtan küçükse süre ertesi güne taşarak hesaplanır;
  "Uyku Başlat" açıkken ikinci bir kayıt açılmasına izin verilmez.
- Beslenme tablolarında çift günlerin satır zemini bir ton koyudur
  (`Tema.SATIR_KOYU`); renk satıra değil kaydın tarihine bağlıdır, arama
  açıkken de şaşmaz.
- İstatistikler haftalık mama ve uyku grafikleri, gün gün toplam mama listesi,
  toplam mama miktarı, toplam uyku, kayıtlı gün sayısı ve günlük ortalama içerir.

---

## Doğrulama

`test.bat` iki takım çalıştırır:

- **Veri katmanı (101 kontrol)** — JSON gidiş-dönüşü ve kaçış dizileri, mama miktarı
  çözümleme, eski alan adlarıyla (`date`/`time`) yazılmış dosyaların okunması, atomik
  kaydetme ve yedek, bozuk dosyaya dayanıklılık, gece yarısını aşan uyku süresi,
  virgüllü ölçü değerleri, vitamin/ilaç kayıtları, beş dosyanın birlikte kaydedilip
  yüklenmesi, CSV kaçışları ve `.ico` yapısı.
- **İşlevsel (41 kontrol)** — gerçek pencere ekran dışında açılır; kayıt ekleme,
  işaretleme, hücre düzenleme, gecikmeli kaydetmenin diske yansıması, arama, silme,
  uyku/büyüme/ilaç/hatırlatıcı akışları, ekran geçişleri ve özet tablosunun 5 satır
  sınırı sınanır.

`test.bat onizleme` dokuz ekran görüntüsünü PNG olarak üretir (ekranın DPI ölçeği
dikkate alınarak), böylece arayüz değişiklikleri gözle karşılaştırılabilir.

---

## Telif

**© 2026 by AKANSEL**

Metin tek yerde, `Tema.TELIF` sabitinde tanımlıdır ve üç yerde görünür:

- sol menünün altında,
- **Dışa Aktar → Hakkında** penceresinde,
- paketlenmiş `.exe` dosyasının Özellikler → Ayrıntılar sekmesinde
  (`paketle.bat` içindeki `--copyright` bayrağı).

Değiştirmek için `src/com/akansel/bebektakip/ui/Tema.java` içindeki `TELIF`
sabitini ve `paketle.bat` içindeki iki `--copyright` satırını güncelleyin.
