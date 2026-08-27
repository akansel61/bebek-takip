# Bebek Beslenme Takibi — Masaüstü Uygulaması

© 2026 by AKANSEL

Bebeğin beslenme ve bez kayıtlarını tutan **saf Java (Swing)** masaüstü uygulaması.
HTML, WebView veya tarayıcı motoru kullanılmaz; tüm arayüz native Swing
bileşenleriyle çizilir.

- **Dış bağımlılık yok.** Maven/Gradle gerekmez, indirilen hiçbir kütüphane yoktur.
  JSON okuyucu/yazıcı, ikonlar ve grafikler dahil her şey proje içinde.
- **Veri:** kullanıcı klasöründe düz JSON dosyası + istenildiğinde CSV dışa aktarım.
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

- **Dashboard** — bugünün özeti (kayıt, toplam mama, çiş, kaka, emzirme) ve son 5 kayıt.
- **Kayıtlar** — tüm kayıtların düzenlenebilir tablosu ve arama.
- **İstatistikler** — son 7 günün kayıt ve mama grafikleri, genel toplamlar.

### Tabloda düzenleme

| İşlem | Nasıl |
|---|---|
| Çiş / Kaka / Sağ / Sol / Mama işaretleme | Kutuya tıklayın (veya hücreyi seçip **Boşluk**) |
| Tarih değiştirme | Tarih hücresine tıklayın, açılan takvimden seçin |
| Saat değiştirme | Hücreye tıklayıp `SS:DD` yazın (geçersiz saat kırmızı çerçeveyle reddedilir) |
| Mama notu / Not | Hücreye tıklayıp yazın |
| Satır silme | Satırın üstüne gelin, sağdaki çöp kutusuna tıklayın (veya **Delete**) |

Mama miktarı, mama notundaki ilk sayıdan okunur: `120`, `120 ml`, `90ml`, `7,5`
yazımlarının hepsi tanınır. Mama kutusu işaretli değilse miktar toplama katılmaz.

Değişiklikler yazmayı bıraktıktan ~0,6 saniye sonra otomatik kaydedilir; pencere
kapanırken de kaydedilir.

### Klavye kısayolları

| Kısayol | İşlev |
|---|---|
| `Ctrl+N` | Yeni kayıt |
| `Ctrl+F` | Kayıtlar ekranında aramaya git |
| `Ctrl+S` | Hemen kaydet |
| `Ctrl+E` | CSV olarak dışa aktar |
| `Ctrl+1 / 2 / 3` | Dashboard / Kayıtlar / İstatistikler |
| `Boşluk` | Seçili işaret kutusunu değiştir |
| `Delete` | Seçili satırı sil |

---

## Veri

Kayıtlar şurada tutulur:

```
%USERPROFILE%\.bebek-takip\kayitlar.json
```

Yazma **atomiktir**: önce `kayitlar.json.tmp` dosyasına yazılır, sonra yerine taşınır;
bir önceki sürüm `kayitlar.json.bak` olarak saklanır. Dosya bozulursa uygulama
çökmez, uyarı gösterip boş listeyle açılır.

**Dışa Aktar** menüsünden:

- **CSV** — noktalı virgülle ayrılmış, BOM'lu UTF-8. Türkçe Excel'de çift tıklayınca
  doğru açılır; sayısal mama miktarı ayrı bir sütun olarak da yazılır.
- **JSON yedek** — tam yedek.
- **JSON'dan içe aktar** — yedekleri geri yükler. Alan adlarında hem `tarih`/`saat`
  hem de `date`/`time` yazımı tanınır. İçe aktarırken "mevcuda ekle" veya "hepsini
  değiştir" seçebilirsiniz.

---

## Proje yapısı

```
java/
  src/com/akansel/bebektakip/
    App.java                    giriş noktası, görünüm ve yazı tipi ayarları
    model/Kayit.java            tek kayıt + tarih/saat/sayı çözümleme
    store/
      Json.java                 bağımlılıksız JSON okuyucu/yazıcı
      KayitDeposu.java          yükleme, atomik kaydetme, yedek, sorgular
      DisaAktarim.java          CSV ve JSON dışa/içe aktarım
    ui/
      Tema.java                 renkler, yazı tipleri, Türkçe biçimlendirme
      Ikonlar.java              vektör ikonlar (emoji/resim dosyası yok)
      UygulamaIkonu.java        uygulama simgesi çizimi
      AnaPencere.java           pencere, üst şerit, menü, kısayollar, kaydetme
      YanMenu.java              sol gezinme şeridi
      DashboardPanel.java       özet ekranı
      KayitlarPanel.java        kayıt tablosu ekranı
      IstatistiklerPanel.java   grafikler ve toplamlar
      bilesen/                  kart, buton, arama, takvim, grafik, boş durum
      tablo/                    tablo modeli, hücre çizicileri, tablo davranışı
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
- Silmeden önce **onay** sorulur.
- Kayıtlar tarih + saate göre **sıralı** tutulur, her kaydın kendi kimliği (`id`) vardır.
- Tablo ekrandaki satırlardan değil **doğrudan veri modelinden** çalışır. Bu ayrım
  önemli: arama açıkken bir hücreyi düzenlemek yalnızca o kaydı değiştirir, filtrenin
  dışında kalan kayıtlara dokunmaz.
- Gece yarısı geçildiğinde "bugün" ölçümleri kendiliğinden tazelenir.
- İstatistikler haftalık mama grafiği, toplam mama miktarı, kayıtlı gün sayısı ve
  günlük ortalama içerir.

---

## Doğrulama

`test.bat` iki takım çalıştırır:

- **Veri katmanı (63 kontrol)** — JSON gidiş-dönüşü ve kaçış dizileri, mama miktarı
  çözümleme, eski alan adlarıyla (`date`/`time`) yazılmış dosyaların okunması, atomik
  kaydetme ve yedek, bozuk dosyaya dayanıklılık, CSV kaçışları ve `.ico` yapısı.
- **İşlevsel (34 kontrol)** — gerçek pencere ekran dışında açılır; kayıt ekleme,
  işaretleme, hücre düzenleme, gecikmeli kaydetmenin diske yansıması, arama, silme,
  ekran geçişleri ve özet tablosunun 5 satır sınırı sınanır.

`test.bat onizleme` her ekranın PNG görüntüsünü üretir (ekranın DPI ölçeği dikkate
alınarak), böylece arayüz değişiklikleri gözle karşılaştırılabilir.

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
