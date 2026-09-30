# Lockscreen Notification Touch Blocker v1.0.0

Android 16 / Project Infinity için küçük LSPosed modülü.

## Ne yapar?
- Yalnızca `com.android.systemui` scope kullanır.
- Cihaz kilitliyken notification stack içindeki gerçek bildirim kartı üzerinde başlayan hareketi tüketir.
- Bildirime dokunma, sağa/sola kaydırarak silme, uzun basma ve genişletme hareketlerini engeller.
- Saat, parmak izi, kilit ekranının boş alanı ve bildirim dışındaki SystemUI alanlarına dokunmaz.
- Kilit açıldığında bildirimler normal çalışır.

## LSPosed
1. APK'yı kur.
2. LSPosed > Modules > Lockscreen Notification Touch Blocker.
3. Scope olarak yalnızca `System UI (com.android.systemui)` seçili olmalı.
4. Modülü etkinleştir.
5. Telefonu yeniden başlat (veya SystemUI'yi yeniden başlat).

## Log kontrolü
Termux/root shell:

```sh
su -c 'logcat -d | grep -E "LNTB|LockscreenNotification" | tail -100'
```

Beklenen başlangıç satırları:
- `LNTB: loaded in SystemUI`
- `LNTB: hooks installed successfully`

Bir bildirime kilit ekranında dokununca:
- `LNTB: BLOCK DOWN ...`

## Güvenlik yaklaşımı
Keyguard tespiti başarısız olursa modül "fail open" davranır; SystemUI dokunmasını bozmak yerine engellemeyi devre dışı bırakır.
