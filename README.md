# EasyNote Account Deletion Pages

EasyNote uygulaması için hesap silme taleplerini yönetmek üzere oluşturulan web sayfaları.

## 🌐 Canlı URL'ler

- **Tüm Diller**: [https://easynote-deleteaccount.vercel.app/](https://easynote-deleteaccount.vercel.app/)

Web sayfası otomatik olarak kullanıcının tarayıcı diline göre uygun dili gösterecektir.

## 📁 Dosya Yapısı

```
├── account-delete.html      # Türkçe hesap silme sayfası
├── account-delete-en.html   # İngilizce hesap silme sayfası
├── account-delete-de.html   # Almanca hesap silme sayfası
├── vercel.json             # Vercel konfigürasyonu
├── README.md               # Bu dosya
└── vercel-deployment-guide.md # Yayınlama rehberi
```

## ✨ Özellikler

- 🌍 **Çoklu Dil Desteği**: Türkçe, İngilizce, Almanca
- 📱 **Responsive Tasarım**: Mobil ve masaüstü uyumlu
- 🎨 **Modern UI**: Gradient arka plan, gölgeler, animasyonlar
- ✅ **Form Validasyonu**: E-posta ve neden seçimi zorunlu
- 📧 **E-posta Entegrasyonu**: Otomatik e-posta oluşturma
- 🔒 **Güvenlik**: HTTPS, güvenlik başlıkları
- ⚡ **Hızlı Yükleme**: CDN optimizasyonu

## 🚀 Hızlı Başlangıç

### Yerel Geliştirme

1. Repository'yi klonlayın:
```bash
git clone https://github.com/username/easynote-account-delete.git
cd easynote-account-delete
```

2. Dosyaları bir web sunucusunda çalıştırın:
```bash
# Python ile
python -m http.server 8000

# Node.js ile
npx serve .

# PHP ile
php -S localhost:8000
```

3. Tarayıcıda açın: `http://localhost:8000`

### Vercel'de Yayınlama

1. Vercel hesabı oluşturun: [vercel.com](https://vercel.com)
2. GitHub repository'nizi bağlayın
3. Otomatik deploy edilecektir

## 📧 E-posta Şablonu

Gönderilen e-posta şu formatta olacaktır:

```
Konu: Hesap Silme Talebi - user@example.com

Hesap Silme Talebi:

E-posta: user@example.com
Neden: Gizlilik endişeleri
Açıklama: Kullanıcının açıklaması

Tarih: 01.01.2024 12:00:00
```

## 🔧 Konfigürasyon

### Vercel Ayarları

`vercel.json` dosyası şu özellikleri sağlar:

- **Routing**: Dil bazlı URL yönlendirme
- **Security Headers**: Güvenlik başlıkları
- **Static Build**: HTML dosyaları için optimizasyon

### E-posta Ayarları

Destek e-posta adresini değiştirmek için HTML dosyalarındaki `support@easynote.app` adresini güncelleyin.

## 📊 Analytics

Vercel Dashboard'da şu metrikleri görebilirsiniz:

- Sayfa görüntüleme sayısı
- Ziyaretçi lokasyonları
- Performans metrikleri
- Hata oranları

## 🔒 Güvenlik

Bu sayfalar şu güvenlik önlemlerini içerir:

- ✅ HTTPS zorunluluğu
- ✅ XSS koruması
- ✅ Clickjacking koruması
- ✅ Content sniffing koruması
- ✅ Referrer policy
- ✅ Permissions policy

## 🌍 GDPR Uyumluluğu

- ✅ Kullanıcı onayı
- ✅ Veri silme hakkı
- ✅ Şeffaf işlem
- ✅ Erişilebilir süreç

## 🤝 Katkıda Bulunma

1. Repository'yi fork edin
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Değişikliklerinizi commit edin (`git commit -m 'Add amazing feature'`)
4. Branch'inizi push edin (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## 📝 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için `LICENSE` dosyasına bakın.

## 📞 Destek

Herhangi bir sorun için:

- 📧 E-posta: support@easynote.app
- 📱 Uygulama içi: Ayarlar > Hesabı Sil
- 🐛 Issues: GitHub Issues sayfası

## 🔄 Güncellemeler

### v1.0.0 (2024-01-01)
- ✅ İlk sürüm
- ✅ Çoklu dil desteği
- ✅ Responsive tasarım
- ✅ Vercel deployment

---

**EasyNote** - Günlük notlarını hızlıca ekle, düzenle ve yönet! 