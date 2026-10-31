# Naim VPN — MVP

NordVPN-স্টাইলের নিজের VPN অ্যাপের MVP কোড। সত্যিটা পরিষ্কার: **আসল VPN চলতে আসল VPS সার্ভার লাগে** — এই রিপোতে কোড রেডি, সার্ভার যোগ করলেই Connect কাজ করবে। সার্ভার ছাড়া Connect চাপ দিলে অ্যাপ ও API দুটোই "server not configured" দেখাবে, ভুয়া কানেক্ট দেখাবে না।

## যা আছে
- `backend/server.py` — Python stdlib-only API (pip লাগে না): `/health`, `/servers`, `/login`, `/config`। লোকালে টেস্ট করা।
- `backend/servers.json` — সার্ভার লিস্ট: Singapore (Oracle, মূল পরিকল্পনা), India Mumbai (Azure trial, ১২ মাস), Singapore VPSWala Starter (যাচাই বাকি)।
- `server/setup-wireguard.sh`, `server/add-peer.sh` — VPS-এ WireGuard বসানো + peer config/QR বানানো।
- `android/` — Kotlin অ্যাপের কাঠামো: Connect/Disconnect, Kill Switch, সার্ভার লিস্ট।

## সার্ভারের সত্যি অবস্থা (2026-11-01)
- Oracle Always Free: আসল ফ্রি, কিন্তু সাইনআপে কার্ড ভেরিফিকেশন লাগে — মালিকের কার্ড নেই।
- Azure: ট্রায়াল ১২ মাস, স্থায়ী না।
- VPSWala Starter: GitHub দিয়ে ভেরিফাই করে ফ্রি দাবি করে, কিন্তু সাইটে স্পেক গরমিল (কোথাও 500MB ডিস্ক, কোথাও 25GB)। **আসল সার্ভার হাতে পেয়ে ডিস্ক/RAM যাচাই না করে VPN-এর ভরসা করা যাবে না।** GitHub ভেরিফাই ট্যাপটা মালিককেই নিজের ব্রাউজারে করতে হয় — Muse-এর GitHub কানেক্টর দিয়ে অন্য সাইটে লগইন হয় না, কানেক্টর শুধু এই রিপোতে কোড তুলতে পারে।
- GitHub নিজে কোনো সার্ভার দেয় না; এটা শুধু কোড রাখার জায়গা।

## চালানো (backend)
```bash
cd backend && python3 server.py   # PORT env, default 8787
curl localhost:8787/health
curl localhost:8787/servers
curl -X POST localhost:8787/login -d '{"email":"demo@naimvpn.local","password":"demo1234"}'
```

## পরের ধাপ
1. একটা আসল VPS হাতে পাওয়া (VPSWala ভেরিফাই / পরিবারের কার্ডে Oracle / Botkeep-জাতীয়)।
2. VPS-এ `setup-wireguard.sh` চালিয়ে IP পাওয়া।
3. `servers.json`-এ host বসিয়ে status `active` করা, peer বানিয়ে টেস্ট।
4. Android Studio / manual pipeline-এ APK বিল্ড করে ফোনে টেস্ট।
