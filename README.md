<div align="center">

<img src="https://github.com/akansel61/bebek-takip/releases/download/v1.2/logo.png" alt="Bebek Takip" width="88">

# Bebek Takip

Bebeğin beslenme, bez, uyku, büyüme ve vitamin/ilaç kayıtlarını tutan bir Windows masaüstü uygulaması.

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Windows-0078D6?logo=windows&logoColor=white)
![Bağımlılık](https://img.shields.io/badge/Ba%C4%9F%C4%B1ml%C4%B1l%C4%B1k-Yok-2ea44f)
![Sürüm](https://img.shields.io/badge/S%C3%BCr%C3%BCm-1.2-blue)

<img src="https://github.com/akansel61/bebek-takip/releases/download/v1.2/ozet.png" alt="Özet ekranı" width="880">

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

Yedi ekran var:

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
- **Vitamin & İlaç** — bebeğe verilen vitamin ve ilaçların günlüğü: ad, doz ve
  not tabloda tutulur.
- **Hatırlatıcılar** — aşı, ilaç, kontrol randevusu. En yakın tarihli iş üstte
  duruyor; yapılan iş tek tıkla işaretleniyor.
- **İstatistikler** — son yedi günün kayıt, mama ve uyku dağılımı çubuk grafik
  olarak, gün gün toplam mama listesi ve genel toplamlar.

<table>
<tr>
<td width="50%"><img src="https://github.com/akansel61/bebek-takip/releases/download/v1.2/kayitlar.png" alt="Kayıtlar ekranı"></td>
<td width="50%"><img src="https://github.com/akansel61/bebek-takip/releases/download/v1.2/istatistikler.png" alt="İstatistikler ekranı"></td>
</tr>
</table>

Gece beslemesi sırasında gün değişirse "bugün" ölçümleri kendiliğinden sıfırlanıp
yeni güne geçiyor; uygulamayı kapatıp açmaya gerek yok. Görev çubuğu tepsisindeki
simgeden pencere açılmadan yeni kayıt eklenebiliyor.

Kayıtlar CSV olarak dışa aktarılabiliyor. Ayraç noktalı virgül, kodlama BOM'lu
UTF-8 — yani Türkçe Excel'de çift tıklayınca sütunlar doğru yerine oturuyor.
JSON yedeği artık dört veri türünü birden içeriyor; geri yüklenirken "mevcuda
ekle" veya "hepsini değiştir" seçeniyor, eski yedekler de okunuyor.

## Nasıl yazıldı

Arayüz tamamen Swing ile kuruldu; HTML, WebView ya da Electron benzeri bir katman yok.
Kod üç katmana ayrılmış ve bağımlılık tek yönlü akıyor:

```
com.akansel.bebektakip
├── model/       Kayit, UykuKayit, BuyumeKayit, IlacKayit, Hatirlatici
├── depo/        Json, KayitDeposu, DisaAktarim — okuma, yazma, dışa aktarım
├── ui/          Tema, Ikonlar, AnaPencere ve yedi ekran
│   ├── bilesen/     kart, buton, arama kutusu, takvim, çubuk grafik
│   └── tablo/       tablo modeli, hücre çizicileri, tablo davranışı
└── arac/        IkonUret — derleme sırasında .ico üretir
```

`ui` paketi `depo` ve `model`'i tanıyor, ters yönde tek bir import yok. Veri
katmanında `javax.swing` de geçmiyor; bu yüzden depo ve JSON çözümleyici pencere
açmadan sınanabiliyor.

Tek gerçek kaynak `KayitDeposu`'nun bellekte tuttuğu listeler. Tablo modelleri
bunların kopyasını çıkarmıyor, aynı nesnelere referans veriyor — bir hücre
düzenlendiğinde doğrudan kayıt değişiyor. Beslenme tablosunda diske yazma 600 ms
geciktiriliyor, böylece art arda gelen tuş vuruşları tek yazmada toplanıyor.

Dışarıdan hiçbir kütüphane çekilmiyor; JSON okuyucu ve yazıcı da dahil her şey proje
içinde, proje tek bir `javac` komutuyla derleniyor. Diske yazma atomik: önce `.tmp`
dosyasına yazılıyor, sonra asıl dosyanın yerine taşınıyor, bir önceki sürüm de `.bak`
olarak kalıyor.

Dağıtım için `jpackage`'ın `app-image` biçimi kullanılıyor. Kurulum paketi üretmek
WiX Toolset isterken app-image hiçbir ek araç gerektirmiyor; çalışma zamanı yalnızca
`java.base` ve `java.desktop` modülleriyle üretildiğinden paket 66 MB.

## Kurulum

Hazır paketi [Releases sayfasından](https://github.com/akansel61/bebek-takip/releases/latest)
indirin: `BebekTakip-1.2-windows.zip` dosyasını açıp `Bebek Takip.exe`'yi çalıştırmak
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
| <kbd>Ctrl</kbd>+<kbd>F</kbd> | Aramaya git | | <kbd>Ctrl</kbd>+<kbd>1..7</kbd> | Ekranlar arası geçiş |
| <kbd>Ctrl</kbd>+<kbd>S</kbd> | Hemen kaydet | | <kbd>Space</kbd> / <kbd>Del</kbd> | İşaretle / satır sil |

## Veri

Kayıtlar kullanıcı klasöründe, her veri türü kendi dosyasında düz JSON olarak duruyor:

```
%USERPROFILE%\.bebek-takip\kayitlar.json          beslenme ve bez
%USERPROFILE%\.bebek-takip\uykular.json           uyku
%USERPROFILE%\.bebek-takip\buyumeler.json         büyüme ölçümleri
%USERPROFILE%\.bebek-takip\ilaclar.json           vitamin ve ilaçlar
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
    ├── src/       model, depo, arayüz
    ├── test/      veri ve işlev testleri, ekran görüntüsü aracı
    └── *.bat      derle / calistir / paketle / test
```

Mimari, veri biçimi, dağıtım notları ve test kapsamı için: [java/README.md](java/README.md)

<div align="center">
<br>
<sub>© 2026 by AKANSEL</sub>
</div>
