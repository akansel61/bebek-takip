<div align="center">

<img src="https://github.com/akansel61/bebek-takip/releases/download/v1.0/logo.png" alt="Bebek Takip" width="88">

# Bebek Takip

Bebeğin beslenme ve bez kayıtlarını tutan bir Windows masaüstü uygulaması.

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Windows-0078D6?logo=windows&logoColor=white)
![Bağımlılık](https://img.shields.io/badge/Ba%C4%9F%C4%B1ml%C4%B1l%C4%B1k-Yok-2ea44f)
![Sürüm](https://img.shields.io/badge/S%C3%BCr%C3%BCm-1.0-blue)

<img src="https://github.com/akansel61/bebek-takip/releases/download/v1.0/dashboard.png" alt="Dashboard ekranı" width="880">

</div>

## Uygulama ne yapıyor

Yeni doğan bir bebekte gün içinde kaç kez emzirildiği, ne kadar mama verildiği,
kaç bez değiştirildiği çabuk karışıyor. Doktor kontrolünde de ilk sorulan şey bu
oluyor. Uygulama tam olarak bunu kayıt altına alıyor, başka bir şey yapmıyor.

Her satır tek bir olayı temsil ediyor: tarih, saat, çiş, kaka, sağ meme, sol meme,
mama, mama notu ve serbest not. **Yeni Kayıt** dediğinizde o anın tarih ve saatiyle
boş bir satır açılıyor, geri kalanını tabloda tek tıkla işaretliyorsunuz. Tarih
hücresine basınca takvim açılıyor, saat elle yazılıyor ama geçersiz değer kabul
edilmiyor. Yazmayı bıraktıktan yaklaşık yarım saniye sonra kendiliğinden diske
yazıyor; ayrıca kaydet demeye gerek yok.

Mama miktarını ayrı bir alan olarak sormak yerine mama notundan okuyor. `120`,
`120 ml`, `90ml`, `7,5` yazımlarının hepsi tanınıyor; sayı bulunamazsa o kayıt
toplama katılmıyor. Böylece "yarısını içti" gibi bir not da yazabiliyorsunuz.

Üç ekran var. **Dashboard** bugünün özetini ve son beş kaydı gösteriyor.
**Kayıtlar** tüm geçmişin düzenlenebilir tablosu — not, mama notu, tarih ve saat
içinde arama yapılabiliyor. **İstatistikler** son yedi günün kayıt ve mama
dağılımını çubuk grafik olarak, altında da genel toplamları veriyor.

<table>
<tr>
<td width="50%"><img src="https://github.com/akansel61/bebek-takip/releases/download/v1.0/kayitlar.png" alt="Kayıtlar ekranı"></td>
<td width="50%"><img src="https://github.com/akansel61/bebek-takip/releases/download/v1.0/istatistikler.png" alt="İstatistikler ekranı"></td>
</tr>
</table>

Gece beslemesi sırasında gün değişirse "bugün" ölçümleri kendiliğinden sıfırlanıp
yeni güne geçiyor; uygulamayı kapatıp açmaya gerek yok.

Kayıtlar CSV olarak dışa aktarılabiliyor. Ayraç noktalı virgül, kodlama BOM'lu
UTF-8 — yani Türkçe Excel'de çift tıklayınca sütunlar doğru yerine oturuyor.
JSON yedeği de alınabiliyor, geri yüklenirken "mevcuda ekle" veya "hepsini
değiştir" seçeniyor.

## Nasıl yazıldı

Arayüzde tek satır HTML yok. WebView, tarayıcı motoru, Electron benzeri bir katman
kullanılmadı; ekranların tamamı Swing bileşenleriyle kuruldu, kartlar, grafikler ve
ikonlar doğrudan `Graphics2D` ile çiziliyor.

İkonlar için emoji kullanılmadı. Emoji'nin Windows sürümleri arasında görünümü
değişiyor, yazı tipi bulunamadığında kutu çıkıyor ve DPI ölçeklemesinde bulanıklaşıyor.
Bunun yerine her ikon 24×24 birimlik bir ızgarada vektör olarak tanımlandı ve çizim
anında istenen boyuta ölçekleniyor — 16 piksellik menü ikonu da 256 piksellik
uygulama simgesi de aynı koddan çıkıyor.

Dışarıdan hiçbir kütüphane çekilmiyor. JSON okuyucu ve yazıcı da dahil her şey proje
içinde. Bunun pratik sebebi şuydu: makinede Maven ya da Gradle kurulu değildi ve
sırf bir JSON kütüphanesi için derleme aracı zinciri kurmak istemedim. Sonuçta proje
tek bir `javac` komutuyla derleniyor, `derle.bat` bunu yapıyor.

Tablo, ekrandaki satırlardan değil doğrudan veri modelinden çalışıyor. Bu ayrım
önemli: arama açıkken bir hücreyi düzenlemek yalnızca o kaydı değiştiriyor, filtrenin
dışında kalan kayıtlara dokunmuyor.

Diske yazma atomik. Önce `kayitlar.json.tmp` dosyasına yazılıyor, sonra asıl dosyanın
yerine taşınıyor, bir önceki sürüm de `.bak` olarak saklanıyor. Yazma sırasında
elektrik giderse elinizde ya eski ya yeni dosya kalıyor, yarım kalmış bir dosya değil.

Dağıtım için `jpackage`'ın `app-image` biçimi seçildi. Kurulum paketi (`msi`/`exe`)
üretmek WiX Toolset istiyor; app-image ise hiçbir ek araç gerektirmeden çalışan bir
`.exe` ve yanında küçültülmüş bir Java çalışma zamanı bırakıyor. Çalışma zamanı
yalnızca `java.base` ve `java.desktop` modülleriyle üretildiğinden paket 130 MB
yerine 66 MB.

## Kurulum

Paketlenmiş `.exe` kendi Java çalışma zamanını taşır, hedef bilgisayarda Java kurulu
olmasına gerek yoktur. Klasörü olduğu gibi kopyalayabilirsiniz.

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
| <kbd>Ctrl</kbd>+<kbd>F</kbd> | Aramaya git | | <kbd>Ctrl</kbd>+<kbd>1..3</kbd> | Ekranlar arası geçiş |
| <kbd>Ctrl</kbd>+<kbd>S</kbd> | Hemen kaydet | | <kbd>Space</kbd> / <kbd>Del</kbd> | İşaretle / satır sil |

## Veri

Kayıtlar kullanıcı klasöründe, düz JSON olarak duruyor:

```
%USERPROFILE%\.bebek-takip\kayitlar.json
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

Dosya bozulursa uygulama çökmüyor; uyarı gösterip boş listeyle açılıyor, `.bak`
dosyasından elle geri dönebiliyorsunuz.

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
