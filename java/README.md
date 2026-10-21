# Bebek Beslenme Takibi — Masaüstü Uygulaması

© 2026 by AKANSEL

Bebeğin beslenme, bez, uyku, büyüme, vitamin/ilaç, ateş ve hatırlatıcı kayıtlarını
tutan, seçilen dönemin raporunu PDF/Excel olarak çıkaran **saf Java (Swing)**
masaüstü uygulaması. HTML, WebView veya tarayıcı motoru
kullanılmaz; tüm arayüz native Swing bileşenleriyle çizilir.

- **Dış bağımlılık yok.** Maven/Gradle gerekmez, indirilen hiçbir kütüphane yoktur.
  JSON okuyucu/yazıcı, PDF ve Excel yazıcıları, ikonlar ve grafikler dahil her şey
  proje içinde.
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
- **Vitamin & İlaç** — bebeğe verilenlerin günlüğü. "Vitamin Ekle" ve "İlaç Ekle"
  o anın tarih ve saatiyle satır açar; aynı türün son kaydındaki ad ve doz hazır
  gelir. Tür hücresine tıklamak vitamin ↔ ilaç geçişi yapar (raporda ayrı
  bölümlerdir). v1.2'den gelen türsüz kayıtlar adlarına bakılarak ayrılır:
  adında "vit", "demir", "omega", "probiyotik" gibi bir ipucu geçen vitamin
  sayılır.
- **Ateş Takibi** — "Ateş Ölç" o anın saatiyle satır açıp derece hücresine geçer.
  Derece `38,4`, `38.4` ya da `38,4 °C` yazılabilir; 34–43 °C dışı reddedilir.
  Ölçüm yeri ve belirtiler hücreye tıklayınca açılan listeden seçilir. Üstteki
  grafik son 72 saatin ölçümlerini eşik bantlarıyla gösterir; altta ateş
  sınırları ve doktora başvurulması gereken durumlar yazılıdır.
- **Hatırlatıcılar** — aşı, ilaç, kontrol. En yakın tarihli üstte durur; soldaki
  kutu işaretlenince tamamlanmış sayılır.
- **İstatistikler** — son 7 günün kayıt, mama ve uyku grafikleri, gün gün toplam
  mama listesi (son 30 gün) ve genel toplamlar.
- **Rapor** — Günlük / Haftalık / Aylık / Özel aralık; Mama, Vitamin, İlaç ve Ateş
  bölümleri ayrı ayrı seçilir. Sayfalar anında önizlenir. "PDF Kaydet", "Excel
  Kaydet" ve "Yazdır" aynı veriyi kullanır (ayrıntı aşağıda).

### Tabloda düzenleme

| İşlem | Nasıl |
|---|---|
| Çiş / Kaka / Sağ / Sol / Mama işaretleme | Kutuya tıklayın (veya hücreyi seçip **Boşluk**) |
| Tarih değiştirme | Tarih hücresine tıklayın, açılan takvimden seçin |
| Saat değiştirme | Hücreye çift tıklayıp `SS:DD` yazın (geçersiz saat kırmızı çerçeveyle reddedilir) |
| Metin ve sayı alanları | Hücreye çift tıklayıp yazın |
| Satır silme | Satırın üstüne gelin, sağdaki çöp kutusuna tıklayın (veya **Delete**) |
| Ölçüm yeri / belirtiler (ateş) | Hücreye tıklayın; belirtilerde birden çok seçim yapılabilir |
| Vitamin ↔ ilaç | Tür hücresine tıklayın |

Aynı düzen uyku, büyüme, vitamin/ilaç, ateş ve hatırlatıcı tablolarında da geçerlidir.

Mama miktarı, mama notundaki ilk sayıdan okunur: `120`, `120 ml`, `90ml`, `7,5`
yazımlarının hepsi tanınır. Mama kutusu işaretli değilse miktar toplama katılmaz.

Beslenme tablosundaki değişiklikler yazmayı bıraktıktan ~0,6 saniye sonra otomatik
kaydedilir; öbür ekranlardaki değişiklikler anında yazılır. Pencere kapanırken de
kaydedilir.

### Sistem tepsisi

Görev çubuğu tepsisindeki bebek simgesine çift tıklamak pencereyi öne getirir;
sağ tık menüsünden pencere açılmadan yeni kayıt eklenebilir, uyku ya da ateş
ekranına geçilebilir veya uygulamadan çıkılabilir.

### Klavye kısayolları

| Kısayol | İşlev |
|---|---|
| `Ctrl+N` | Yeni kayıt |
| `Ctrl+F` | Kayıtlar ekranında aramaya git |
| `Ctrl+S` | Hemen kaydet |
| `Ctrl+E` | CSV olarak dışa aktar |
| `Ctrl+1 … 9` | Özet / Kayıtlar / Uyku / Büyüme / Vitamin & İlaç / Ateş / Hatırlatıcılar / İstatistikler / Rapor |
| `Ctrl+P` | Rapor ekranı |
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
%USERPROFILE%\.bebek-takip\atesler.json           ateş ölçümleri
%USERPROFILE%\.bebek-takip\hatirlaticilar.json    hatırlatıcılar
```

Ateş kaydının biçimi:

```json
{
  "tarih": "2026-10-20", "saat": "08:15",
  "derece": 38.6, "olcumYeri": "Kulak",
  "belirtiler": ["Öksürük", "Burun akıntısı"],
  "not": "Ateş düşürücü verildi"
}
```

Vitamin/ilaç kayıtlarına `"tur": "vitamin"` ya da `"tur": "ilac"` alanı eklendi;
alanı olmayan eski dosyalar sorunsuz okunur.

Yazma **atomiktir**: önce `.tmp` dosyasına yazılır, sonra yerine taşınır;
bir önceki sürüm `.bak` olarak saklanır. Dosyalardan biri bozulursa uygulama
çökmez, uyarı gösterip o listeyi boş açar; öbür veri türleri etkilenmez.

**Dışa Aktar** menüsünden:

- **CSV** — beslenme kayıtları; noktalı virgülle ayrılmış, BOM'lu UTF-8. Türkçe
  Excel'de çift tıklayınca doğru açılır; sayısal mama miktarı ayrı bir sütun
  olarak da yazılır.
- **JSON yedek** — bütün veri türlerini (ateş dahil) birden içeren tam yedek.
- **JSON'dan içe aktar** — yedekleri geri yükler. Eski, yalnızca beslenme içeren
  yedekler ve tarayıcı sürümünün `date`/`time` alan adları da tanınır. İçe
  aktarırken "mevcuda ekle" veya "hepsini değiştir" seçebilirsiniz.
- **Rapor (PDF / Excel / yazdır)** — rapor ekranını açar.

---

## Rapor

| Dönem | Kapsam |
|---|---|
| Günlük | seçilen gün |
| Haftalık | seçilen günü içeren pazartesi–pazar |
| Aylık | seçilen günün ayı |
| Özel aralık | iki tarih arası (oklar aralığı kendi uzunluğu kadar kaydırır) |

İçinde bulunulan dönem bugünde kesilir: ayın raporunda gelecek günler boş satır
olarak görünmez, ileri tarihli kayıtlar toplama girmez.

**PDF** — A4, dikey. İlk sayfada başlık, hazırlanma zamanı ve özet kutuları;
ardından gün gün özet tablosu (günlük raporda yok) ve seçilen her bölümün
ayrıntı tablosu. Aynı günün kayıtları tarih tekrarlanmadan gruplanır, günler
zemin tonuyla ayrılır. Mama notu yalnızca miktarı tekrar ediyorsa ("120 ml")
yazılmaz. Ateş satırlarında derece ve durum seviyesine göre renklenir, tablonun
altında belirtilerin kaç kez görüldüğü yazar. Tablo sayfaya sığmazsa başlık
satırı tekrarlanarak sonraki sayfada sürer; her sayfanın altında sayfa numarası
ve telif satırı vardır.

PDF dış kütüphane olmadan yazılır (`rapor/PdfYazici`). Yazı tipi olarak sistemin
`segoeui.ttf` / `segoeuib.ttf` dosyaları kullanılır (yoksa Arial, Tahoma,
Calibri denenir); yalnızca raporda geçen glifler alınarak PDF'e gömülür. Böylece
dosya küçük kalır (haftalık rapor ~35 KB) ve Türkçe harfler yazı tipi kurulu
olmayan bilgisayarda da doğru görünür. Metin seçilip kopyalanabilir, aranabilir.

**Excel** — `Özet` sayfasında toplamlar ve gün gün tablo (toplam satırı `SUM` /
`MAX` formülüdür); ardından seçilen her bölüm için ayrı sayfa. Tarih ve saatler
Excel tarih sayısı olarak yazılır; başlık satırı dondurulmuş, filtre açıktır.
Ateş sayfasında durum hücresi seviyesine göre renklidir.

**Yazdır** — Windows yazdırma penceresini açar; önizlemede görünen sayfalar
yazıcının basabildiği alana ölçeklenerek gönderilir. "Microsoft Print to PDF"
seçilerek de PDF alınabilir.

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
      IlacKayit.java            verilen vitamin / ilaç kaydı (türüyle)
      AtesKayit.java            ateş ölçümü, ölçüm yeri, belirtiler, seviye
      Hatirlatici.java          aşı, ilaç, randevu hatırlatması
    depo/
      Json.java                 bağımlılıksız JSON okuyucu/yazıcı
      KayitDeposu.java          yükleme, atomik kaydetme, yedek, sorgular
      DisaAktarim.java          CSV ve JSON dışa/içe aktarım
    rapor/
      Donem.java                günlük / haftalık / aylık / özel aralık
      RaporVerisi.java          dönemin kayıtları, gün gün özet, toplamlar
      RaporDizici.java          veriyi A4 sayfalara yerleştirir
      Sayfa.java                sayfanın çizim komutları (yazı, kutu, çizgi)
      SayfaCizici.java          sayfayı ekrana ve yazıcıya çizer
      RaporYazisi.java          rapor yazı tipi (sistemden .ttf bulur)
      TrueTypeYazi.java         .ttf okuma ve glif alt kümesi
      PdfYazici.java            bağımlılıksız PDF yazıcı
      ExcelYazici.java          bağımlılıksız .xlsx yazıcı
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
      AtesPanel.java            ateş ekranı: grafik, ölçüm tablosu
      HatirlaticiPanel.java     hatırlatıcı ekranı
      IstatistiklerPanel.java   grafikler ve toplamlar
      RaporPanel.java           rapor ekranı: dönem, bölümler, önizleme
      DosyaSecici.java          kaydetme penceresi, kaydedilen dosyayı açma
      bilesen/                  kart, buton, arama, takvim, grafikler, seçim
                                listesi, sayfa önizleme, boş durum
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
- Ateş sınırları: 37,5 °C altı normal, 37,5–37,9 hafif yüksek, 38 ve üstü ateş,
  39 ve üstü yüksek ateş. Uygulama tanı koymaz; ateş ekranındaki not ve rapordaki
  dipnot bunu açıkça söyler.
- Rapor tek bir çizim listesinden üretilir: önizleme, yazıcı ve PDF aynı sayfaları
  çizer. Satır kırılımları PDF'e gömülen yazı tipinin kendi ölçüleriyle hesaplanır;
  ekranda görülen ile dosyadaki birebir aynıdır.

---

## Doğrulama

`test.bat` iki takım çalıştırır:

- **Veri katmanı (213 kontrol)** — JSON gidiş-dönüşü ve kaçış dizileri, mama miktarı
  çözümleme, eski alan adlarıyla (`date`/`time`) yazılmış dosyaların okunması, atomik
  kaydetme ve yedek, bozuk dosyaya dayanıklılık, gece yarısını aşan uyku süresi,
  virgüllü ölçü değerleri, vitamin/ilaç kayıtları ve tür tahmini, ateş seviyeleri ve
  derece çözümleme, altı dosyanın birlikte kaydedilip yüklenmesi, CSV kaçışları,
  `.ico` yapısı; rapor tarafında dönem hesapları (ay ve yıl aşan haftalar dahil),
  toplamlar, sayfalara bölme ve taşma, PDF'in xref tablosu ve gömülü yazı
  tiplerindeki Türkçe glifler, `.xlsx` içindeki her XML'in geçerliliği, tarih
  sayıları ve formüller.
- **İşlevsel (61 kontrol)** — gerçek pencere ekran dışında açılır; kayıt ekleme,
  işaretleme, hücre düzenleme, gecikmeli kaydetmenin diske yansıması, arama, silme,
  uyku/büyüme/ilaç/hatırlatıcı akışları, "Ateş Ölç" ve derece girişi, vitamin/ilaç
  türüyle ekleme, rapor ekranından PDF ve Excel üretimi, ekran geçişleri ve özet
  tablosunun 5 satır sınırı sınanır.

`test.bat onizleme` on bir ekran görüntüsünü PNG olarak üretir (ekranın DPI ölçeği
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
