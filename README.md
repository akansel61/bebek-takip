<div align="center">

<img src="https://github.com/akansel61/bebek-takip/releases/download/v1.3/logo.png" alt="Bebek Takip" width="88">

# Bebek Takip

Bebeğin beslenme, bez, uyku, büyüme, vitamin/ilaç ve ateş kayıtlarını tutan, istenen dönemin raporunu PDF ya da Excel olarak çıkaran bir Windows masaüstü uygulaması.

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Windows-0078D6?logo=windows&logoColor=white)
![Bağımlılık](https://img.shields.io/badge/Ba%C4%9F%C4%B1ml%C4%B1l%C4%B1k-Yok-2ea44f)
![Sürüm](https://img.shields.io/badge/S%C3%BCr%C3%BCm-1.3-blue)

<img src="https://github.com/akansel61/bebek-takip/releases/download/v1.3/ozet.png" alt="Özet ekranı" width="880">

</div>

## Uygulama ne yapıyor

Yeni doğan bir bebekte gün içinde kaç kez emzirildiği, ne kadar mama verildiği,
kaç bez değiştirildiği çabuk karışıyor. Doktor kontrolünde de ilk sorulan şey bu
oluyor. Uygulama tam olarak bunu kayıt altına alıyor.

Her satır tek bir olayı temsil ediyor: tarih, saat, çiş, kaka, sağ meme, sol meme,
mama, mama notu ve serbest not. **Yeni Kayıt** dediğinizde o anın tarih ve saatiyle
boş bir satır açılıyor, geri kalanını tabloda tek tıkla işaretliyorsunuz. Tarih
hücresine basınca takvim açılıyor, saat elle yazılıyor ama geçersiz değer kabul
edilmiyor. Yazmayı bıraktıktan yaklaşık yarım saniye sonra kendiliğinden diske
yazıyor; ayrıca kaydet demeye gerek yok.

Mama miktarını ayrı bir alan olarak sormak yerine mama notundan okuyor. `120`,
`120 ml`, `90ml`, `7,5` yazımlarının hepsi tanınıyor; sayı bulunamazsa o kayıt
toplama katılmıyor. Böylece "yarısını içti" gibi bir not da yazabiliyorsunuz.

## Ekranlar

Dokuz ekran var:

- **Özet** — bugünün ölçümleri (kayıt, mama, çiş, kaka, emzirme, uyku, bekleyen
  hatırlatıcı, son kilo) ve son beş kayıt.
- **Kayıtlar** — tüm geçmişin düzenlenebilir tablosu; not, mama notu, tarih ve
  saat içinde arama yapılabiliyor. Günler kolay ayırt edilsin diye çift günlerin
  satırları (ayın 2, 4, 6…sı) bir ton koyu zeminle çiziliyor.
- **Uyku Takibi** — bebek uyuyunca **Uyku Başlat**, uyanınca **Uykuyu Bitir**.
  Gece yarısını aşan uykular doğru hesaplanıyor; saatler sonradan elle
  düzeltilebiliyor.
- **Büyüme** — kilo, boy ve baş çevresi ölçümleri. `4,2` ve `4.2` yazımlarının
  ikisi de kabul ediliyor.
- **Vitamin & İlaç** — bebeğe verilen vitamin ve ilaçların günlüğü. **Vitamin
  Ekle** ve **İlaç Ekle** ayrı düğmeler; aynı türün son kaydındaki ad ve doz
  hazır geliyor, çünkü çoğu gün aynı şey veriliyor. Tür sütununa tıklamak
  vitamin ile ilaç arasında geçiş yapıyor.
- **Ateş Takibi** — hastalık günleri için. **Ateş Ölç** o anın saatiyle satır
  açıp doğrudan derece hücresine geçiyor; ölçüm yeri (koltuk altı, kulak, alın…)
  ve belirtiler (öksürük, kusma, ishal, döküntü…) hücreye tıklayınca açılan
  listeden seçiliyor. Derece seviyesine göre renkleniyor, üstteki grafik son üç
  günün seyrini 37,5 ve 38 °C eşikleriyle birlikte gösteriyor.
- **Hatırlatıcılar** — aşı, ilaç, kontrol randevusu. En yakın tarihli iş üstte
  duruyor; yapılan iş tek tıkla işaretleniyor.
- **İstatistikler** — son yedi günün kayıt, mama ve uyku dağılımı çubuk grafik
  olarak, gün gün toplam mama listesi ve genel toplamlar.
- **Rapor** — günlük, haftalık, aylık ya da istenen tarih aralığı için mama,
  vitamin, ilaç ve ateş raporu. Sayfalar ekranda anında önizleniyor; **PDF
  Kaydet**, **Excel Kaydet** ve **Yazdır** aynı raporu üretiyor.

<table>
<tr>
<td width="50%"><img src="https://github.com/akansel61/bebek-takip/releases/download/v1.3/kayitlar.png" alt="Kayıtlar ekranı"></td>
<td width="50%"><img src="https://github.com/akansel61/bebek-takip/releases/download/v1.3/istatistikler.png" alt="İstatistikler ekranı"></td>
</tr>
<tr>
<td width="50%"><img src="https://github.com/akansel61/bebek-takip/releases/download/v1.3/ates.png" alt="Ateş Takibi ekranı"></td>
<td width="50%"><img src="https://github.com/akansel61/bebek-takip/releases/download/v1.3/rapor.png" alt="Rapor ekranı"></td>
</tr>
</table>

Gece beslemesi sırasında gün değişirse "bugün" ölçümleri kendiliğinden sıfırlanıp
yeni güne geçiyor; uygulamayı kapatıp açmaya gerek yok. Görev çubuğu tepsisindeki
simgeden pencere açılmadan yeni kayıt eklenebiliyor.

Kayıtlar CSV olarak dışa aktarılabiliyor. Ayraç noktalı virgül, kodlama BOM'lu
UTF-8 — yani Türkçe Excel'de çift tıklayınca sütunlar doğru yerine oturuyor.
JSON yedeği bütün veri türlerini birden içeriyor; geri yüklenirken "mevcuda
ekle" veya "hepsini değiştir" seçiliyor, eski yedekler de okunuyor.

## Rapor

Doktor kontrolüne ya da hastalık sonrası muayeneye götürmek için hazırlandı.
Dönem dört biçimde seçiliyor: **Günlük**, **Haftalık** (pazartesi–pazar),
**Aylık** ve **Özel aralık**. Oklarla bir önceki/sonraki döneme geçiliyor, tarih
düğmesi takvimi açıyor. Rapora girecek bölümler (**Mama**, **Vitamin**, **İlaç**,
**Ateş**) tek tek açılıp kapatılabiliyor.

- **PDF** — A4 sayfa: başlık, özet kutuları (toplam mama, vitamin ve ilaç
  sayısı, en yüksek ateş), gün gün özet tablosu ve her bölümün ayrıntı tablosu.
  Ateş satırları seviyesine göre renkleniyor, görülen belirtiler sayılıyor.
  Uzun tablolar sayfaya sığmazsa başlık satırı tekrarlanarak devam ediyor.
- **Excel (.xlsx)** — Özet, Mama, Vitamin, İlaç ve Ateş sayfaları. Tarih ve
  saatler Excel'in kendi biçiminde olduğu için sıralama ve filtre doğrudan
  çalışıyor; özetteki toplamlar formül.
- **Yazdır** — Windows'un yazdırma penceresi açılıyor; önizlemede görülen sayfa
  neyse kâğıda o çıkıyor.

İçinde bulunulan ayın raporu bugünde kesiliyor; gelecek günler boş satır olarak
listelenmiyor.

## Nasıl yazıldı

Arayüz tamamen Swing ile kuruldu; HTML, WebView ya da Electron benzeri bir katman yok.
Kod üç katmana ayrılmış ve bağımlılık tek yönlü akıyor:

```
com.akansel.bebektakip
├── model/       Kayit, UykuKayit, BuyumeKayit, IlacKayit, AtesKayit, Hatirlatici
├── depo/        Json, KayitDeposu, DisaAktarim — okuma, yazma, dışa aktarım
├── rapor/       Donem, RaporVerisi, RaporDizici, PdfYazici, ExcelYazici
├── ui/          Tema, Ikonlar, AnaPencere ve dokuz ekran
│   ├── bilesen/     kart, buton, arama kutusu, takvim, grafikler, sayfa önizleme
│   └── tablo/       tablo modeli, hücre çizicileri, tablo davranışı
└── arac/        IkonUret — derleme sırasında .ico üretir
```

`ui` paketi `depo`, `rapor` ve `model`'i tanıyor, ters yönde tek bir import yok.
Veri ve rapor katmanında `javax.swing` de geçmiyor; bu yüzden depo, JSON
çözümleyici, PDF ve Excel üretimi pencere açmadan sınanabiliyor.

Tek gerçek kaynak `KayitDeposu`'nun bellekte tuttuğu listeler. Tablo modelleri
bunların kopyasını çıkarmıyor, aynı nesnelere referans veriyor — bir hücre
düzenlendiğinde doğrudan kayıt değişiyor. Beslenme tablosunda diske yazma 600 ms
geciktiriliyor, böylece art arda gelen tuş vuruşları tek yazmada toplanıyor.

Dışarıdan hiçbir kütüphane çekilmiyor; JSON okuyucu ve yazıcı da dahil her şey proje
içinde, proje tek bir `javac` komutuyla derleniyor. PDF de kütüphanesiz yazılıyor:
rapor önce punto cinsinden çizim komutlarına dökülüyor, aynı liste hem ekrana ve
yazıcıya hem PDF'e çiziliyor. Türkçe harfler her bilgisayarda doğru görünsün diye
sistemdeki Segoe UI'nin yalnızca kullanılan glifleri PDF'e gömülüyor. `.xlsx`
dosyası da Java'nın kendi zip desteğiyle üretiliyor. Diske yazma atomik: önce `.tmp`
dosyasına yazılıyor, sonra asıl dosyanın yerine taşınıyor, bir önceki sürüm de `.bak`
olarak kalıyor.

Dağıtım için `jpackage`'ın `app-image` biçimi kullanılıyor. Kurulum paketi üretmek
WiX Toolset isterken app-image hiçbir ek araç gerektirmiyor. Çalışma zamanında
`java.base` ve `java.desktop` dışında yalnızca `jdk.localedata` var; o da sadece
Türkçe ve İngilizce verisiyle. Paket 66 MB. `jdk.localedata` olmadan paketlenmiş
program ay ve gün adlarını İngilizce yazıyor, `38,6` yerine `38.6` gösteriyordu
(1.2 paketinde böyleydi).

## Kurulum

Hazır paketi [Releases sayfasından](https://github.com/akansel61/bebek-takip/releases/latest)
indirin: `BebekTakip-1.3-windows.zip` dosyasını açıp `Bebek Takip.exe`'yi çalıştırmak
yeterli. Paket kendi Java çalışma zamanını taşır, hedef bilgisayarda Java kurulu
olmasına gerek yoktur; klasörü olduğu gibi kopyalayabilirsiniz.

Kaynaktan derlemek için JDK 17 veya üstü yeterli:

```bat
git clone https://github.com/akansel61/bebek-takip.git
cd bebek-takip\java

derle.bat        :: derle  ->  build\jar\BebekTakip.jar
calistir.bat     :: çalıştır
paketle.bat      :: .exe üret
test.bat         :: doğrulama testlerini çalıştır
```

> [!NOTE]
> `paketle.bat` çıktıyı varsayılan olarak `%USERPROFILE%\BebekTakip` klasörüne koyar.
> Üretilen `.exe`, yolunda Windows ANSI kod sayfasında bulunmayan bir karakter
> (`ı`, `ğ`, `ş`) geçen klasörden çalışmıyor — jpackage başlatıcısı kendi klasörünü
> o kod sayfası üzerinden çözdüğü için `java.dll`'i bulamıyor. Bu yüzden hedef
> klasör ASCII seçildi.

## Klavye kısayolları

| Kısayol | İşlev | | Kısayol | İşlev |
|---|---|---|---|---|
| <kbd>Ctrl</kbd>+<kbd>N</kbd> | Yeni kayıt | | <kbd>Ctrl</kbd>+<kbd>E</kbd> | CSV dışa aktar |
| <kbd>Ctrl</kbd>+<kbd>F</kbd> | Aramaya git | | <kbd>Ctrl</kbd>+<kbd>1..9</kbd> | Ekranlar arası geçiş |
| <kbd>Ctrl</kbd>+<kbd>S</kbd> | Hemen kaydet | | <kbd>Ctrl</kbd>+<kbd>P</kbd> | Rapor ekranı |
| <kbd>Space</kbd> / <kbd>Del</kbd> | İşaretle / satır sil | | | |

## Veri

Kayıtlar kullanıcı klasöründe, her veri türü kendi dosyasında düz JSON olarak duruyor:

```
%USERPROFILE%\.bebek-takip\kayitlar.json          beslenme ve bez
%USERPROFILE%\.bebek-takip\uykular.json           uyku
%USERPROFILE%\.bebek-takip\buyumeler.json         büyüme ölçümleri
%USERPROFILE%\.bebek-takip\ilaclar.json           vitamin ve ilaçlar
%USERPROFILE%\.bebek-takip\atesler.json           ateş ölçümleri
%USERPROFILE%\.bebek-takip\hatirlaticilar.json    hatırlatıcılar
```

```json
{
  "surum": 1,
  "kayitlar": [
    {
      "tarih": "2026-08-26", "saat": "14:05",
      "cis": true, "kaka": false,
      "sagMeme": false, "solMeme": false,
      "mama": true, "mamaNotu": "140 ml",
      "not": "Uyuyarak içti"
    }
  ]
}
```

Dosyalardan biri bozulursa uygulama çökmüyor; uyarı gösterip o listeyi boş açıyor,
`.bak` dosyasından elle geri dönebiliyorsunuz. Öbür veri türleri etkilenmiyor.

## Proje yapısı

```
bebek-takip/
└── java/
    ├── src/       model, depo, rapor, arayüz
    ├── test/      veri ve işlev testleri, ekran görüntüsü aracı
    └── *.bat      derle / calistir / paketle / test
```

Mimari, veri biçimi, dağıtım notları ve test kapsamı için: [java/README.md](java/README.md)

<div align="center">
<br>
<sub>© 2026 by AKANSEL</sub>
</div>
