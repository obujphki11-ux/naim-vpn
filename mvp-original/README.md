# VPN MVP — সার্ভার + অ্যান্ড্রয়েড অ্যাপ স্ট্রাকচার

এটা NordVPN-স্টাইল অ্যাপের MVP, পুরো কমার্শিয়াল প্রোডাক্ট না।
সবার জন্য লঞ্চ করার আগে নিচের সীমাগুলো মাথায় রাখতে হবে।

## এই MVP-তে যা আছে
- ১টি WireGuard সার্ভার সেটআপ স্ক্রিপ্ট (Oracle Always Free / Azure ট্রায়াল / যেকোনো Ubuntu VPS-এ চলবে)
- প্রতি ডিভাইস/ইউজারের জন্য আলাদা Peer key বানানোর স্ক্রিপ্ট
- ছোট Backend API (Python, কোনো বাইরের লাইব্রেরি লাগে না):
  - `GET /health`
  - `GET /api/servers` — সার্ভার লিস্ট
  - `POST /api/login` — ডেমো লগইন
  - `GET /api/config?server_id=...` — লগইন করা ইউজারের WireGuard config দেয়
- Android অ্যাপ স্ট্রাকচার (Kotlin + Compose + WireGuard tunnel লাইব্রেরি):
  - সার্ভার লিস্ট, Connect/Disconnect, স্ট্যাটাস
  - `VpnService.prepare()` দিয়ে সিস্টেম অনুমতি
  - Kill switch-এর সহজ সংস্করণ: Disconnect করলে tunnel পুরো নামিয়ে দেওয়া হয়

## এই MVP-তে যা নেই (প্রোডাকশনের আগে লাগবে)
- আসল পেমেন্ট / সাবস্ক্রিপশন (Play Billing)
- আসল ডাটাবেস, ইমেইল ভেরিফিকেশন, পাসওয়ার্ড রিসেট
- HTTPS / TLS — Backend এখন HTTP-তে চলবে, আসল ইউজারের জন্য Nginx + Let's Encrypt বাধ্যতামূলক
- অটো স্কেলিং, লোড ব্যালান্সিং, সার্ভার হেলথ মনিটরিং
- Play Store-এর অর্গানাইজেশন অ্যাকাউন্ট, Privacy Policy, Data Safety ফর্ম

## ধাপ ১ — সার্ভার বানানো (তোমাকে নিজে করতে হবে)
Muse এখান থেকে তোমার Oracle/Azure অ্যাকাউন্টে লগইন করে সার্ভার বানাতে পারবে না।
তোমার কাজ:
1. Oracle Cloud-এ Singapore বা Mumbai রিজিয়নে Ubuntu 22.04 VM নাও,
   অথবা Azure-তে B1s VM নাও (মনে রাখবে: Azure ট্রায়াল ১২ মাস পর শেষ হবে)।
2. VM-এর ফায়ারওয়ালে UDP 51820 খোলো।
3. SSH দিয়ে সার্ভারে ঢুকে চালাও:
   ```bash
   sudo bash setup-wireguard.sh
   ```
   স্ক্রিপ্ট সার্ভারের key বানাবে, `wg0.conf` তৈরি করবে, IP forwarding চালু করবে।
4. সার্ভারের Public Key নোট করো। এটা `servers.json`-এ লাগবে।

## ধাপ ২ — টেস্ট ইউজারের Peer বানানো
প্রতি টেস্ট ইউজারের জন্য, প্রতি সার্ভারে আলাদা Peer লাগবে:
```bash
sudo bash add-peer.sh demo-peer sg-oracle <SERVER_PUBLIC_KEY> <SERVER_IP:51820> 10.8.0.2
```
এটা:
- Peer key বানাবে
- সার্ভারের `wg0.conf`-এ Peer যোগ করবে
- ক্লায়েন্ট config ফাইল বানাবে: `peers/demo-peer_sg-oracle.conf`
- একই config Backend-এর `backend/peers/` ফোল্ডারে কপি করতে হবে (নিচে দেখো)

Peer বাদ দিতে:
```bash
sudo bash remove-peer.sh demo-peer
```
সতর্কতা: `remove-peer.sh` এই MVP-তে শুধু config ফাইল মুছে, সার্ভারের `wg0.conf` থেকে
Peer লাইন হাতে মুছে `wg-quick down wg0 && wg-quick up wg0` করতে হবে। প্রোডাকশনে এটা API দিয়ে অটো করতে হবে।

## কার্ড না থাকলে রাস্তা — নতুন
কার্ড না থাকলে Oracle/Google-এর সাধারণ ফ্রি টায়ার নেওয়া যাবে না, কারণ ওরা ভেরিফিকেশনের জন্য কার্ড চায়।
তাই MVP-তে এখন সবার উপরে “Home Server - No Card Needed” স্লট যোগ করা হয়েছে।

কার্ড ছাড়া ৩টা বাস্তব রাস্তা:
1. **বাড়ির পুরনো PC/ল্যাপটপ দিয়ে সার্ভার (সবচেয়ে নির্ভরযোগ্য, কার্ড লাগে না)**
   পুরনো PC-তে Ubuntu বসিয়ে একই `setup-wireguard.sh` চালাও, রাউটারে UDP 51820
   তোমার PC-র দিকে ফরওয়ার্ড করো, আর পাবলিক IP বদলে গেলে DuckDNS/No-IP-এর মতো
   ফ্রি DDNS ব্যবহার করো। লোকেশন হবে বাংলাদেশের বাড়ি, বিদেশের লোকেশন হবে না,
   স্পিড হবে বাড়ির আপলোড স্পিড, আর PC চালু রাখতে হবে।
   (ইউজারের কাছে PC নেই — এই রাস্তা এখন বন্ধ।)
2. **Azure for Students (শুধু স্টুডেন্ট হলে)**
   ভেরিফায়েড ফুল-টাইম স্টুডেন্ট (১৮+) হলে কলেজ ইমেইল দিয়ে কার্ড ছাড়া ১০০ ডলার ক্রেডিট
   পাওয়া যায়, ১২ মাসের জন্য। এটা সাধারণ Azure ফ্রি অ্যাকাউন্ট না, আলাদা স্টুডেন্ট অফার।
   (ইউজার স্টুডেন্ট না — এই রাস্তাও এখন বন্ধ।)
3. **VPSWala Starter — কার্ড ছাড়া, কিন্তু শুধু টেস্টের জন্য (নতুন)**
   ওদের সাইট অনুযায়ী Starter ফ্রি: ১ vCPU ARM, ২ GB RAM, ৫০০ MB ডিস্ক, মাসে মাত্র ১০ GB
   ব্যান্ডউইথ, কার্ড লাগে না। ভেরিফিকেশন লাগে: .edu ইমেইল অথবা GitHub প্রোফাইল অথবা
   কোম্পানি ইমেইল। ১০ GB দিয়ে সবার জন্য অ্যাপ চালানো যাবে না, হালকা টেস্ট চলবে।
   এটা ছোট, স্বাধীনভাবে যাচাই হয়নি এমন প্রোভাইডার — গুরুত্বপূর্ণ ডেটা বা আসল
   ইউজারদের ট্রাফিক এর উপর ছেড়ো না, শুধু MVP টেস্টের জন্য ব্যবহার করো।
   Professional ট্রায়ালেও ৩০ দিন, ১ TB ব্যান্ডউইথ দাবি করা হয়েছে, সেটাও একই ভেরিফিকেশন।

সবার জন্য মাল্টি-কান্ট্রি পাবলিক অ্যাপ কার্ড ছাড়া চালানো যাবে না — এটা সোজা কথা।
কিন্তু MVP টেস্ট, ডেমো আর নিজের ব্যবহার বাড়ির সার্ভার দিয়েই শুরু করা যাবে,
পরে কার্ড বা স্টুডেন্ট অফার পেলে ক্লাউড সার্ভার যোগ করা যাবে।

## ফ্রি সার্ভার এড করা — নতুন
এখন MVP-তে ৫টা ফ্রি-টায়ার সার্ভার স্লট যোগ করা হয়েছে:
- Singapore, Oracle Always Free — স্থায়ী, এটাই মূল সার্ভার
- India, Oracle 2nd Account Always Free — Oracle-এর Always Free একটাই home region-এ থাকে,
  তাই দ্বিতীয় দেশের জন্য আলাদা অ্যাকাউন্ট লাগবে
- USA, Google e2-micro Always Free — ছোট সার্ভার, শুধু হালকা ব্যবহারের জন্য
- India, Azure Trial — ১২ মাস, তারপর বিল আসবে
- USA, AWS Trial — ৬ মাস বা ক্রেডিট শেষ হলে বন্ধ

গুরুত্বপূর্ণ: লিস্টে নাম যোগ করলেই সার্ভার লাইভ হয় না। প্রতিটা সার্ভারের জন্য আলাদা করে করতে হবে:
1. সেই প্রোভাইডারে VM নাও, `setup-wireguard.sh` চালাও
2. প্রতি ইউজারের জন্য `add-peer.sh` চালাও, config ফাইল `backend/peers/`-এ রাখো
3. তারপর অ্যাপে সার্ভারটা Connect হবে। config না থাকলে অ্যাপ “no config” দেখাবে।

নিজে আরো সার্ভার যোগ করতে Backend ফোল্ডারে চালাও:
```bash
bash add-server.sh de-oracle "Germany" "Germany" "Frankfurt" "Always Free (separate account)"
```
র‍্যান্ডম ফ্রি পাবলিক VPN সার্ভার (যেমন VPN Gate-এর মতো অচেনা সার্ভার) এখানে যোগ করা হয়নি,
কারণ ওগুলোর মালিক কে, ডেটা দেখছে কিনা, কখন বন্ধ হবে — কিছুই নিশ্চিত না।

## ধাপ ৩ — Backend চালানো
সার্ভারে:
```bash
cd server/backend
cp servers.json.example servers.json
cp users.json.example users.json
# servers.json-এ আসল SERVER_IP আর Public Key বসাও
# users.json-এ demo ইউজারের peer নাম ঠিক আছে কিনা দেখো
# peers/ ফোল্ডারে add-peer.sh দিয়ে বানানো .conf ফাইলগুলো রাখো
python3 app.py
```
ডিফল্ট পোর্ট: 8000। ডেমো লগইন: `demo` / `demo123` — প্রথমেই বদলাতে হবে।
`users.json`-এ পাসওয়ার্ড সরাসরি লেখা থাকে না, SHA-256 hash থাকে।
নতুন hash বানাতে: `python3 -c "import hashlib; print(hashlib.sha256(b'নতুন-পাসওয়ার্ড').hexdigest())"`

## ধাপ ৪ — Android অ্যাপ বানানো
Android Studio-তে `android/` ফোল্ডার খোলো।
1. `app/src/main/java/com/example/vpnmvp/ApiClient.kt`-এ `API_BASE_URL` বদলে
   তোমার Backend-এর আসল ঠিকানা বসাও (প্রোডাকশনে অবশ্যই `https://...`)।
2. Gradle Sync করো। WireGuard tunnel লাইব্রেরির ভার্সন Maven Central-এ চেক করে
   প্রয়োজনে আপডেট করো — এই স্ট্রাকচারে উদাহরণ হিসেবে `1.0.20211029` দেওয়া আছে।
3. Debug APK বানিয়ে নিজের ফোনে টেস্ট করো: লগইন -> সার্ভার বেছে নাও -> Connect।
4. Connect-এর সময় Android সিস্টেম VPN অনুমতি চাইবে, সেটা Allow করতে হবে।

এই VM-এ Gradle daemon আগেও ফেল করেছে, তাই এখানে APK বিল্ড করে যাচাই করা হয়নি।
Android Studio-তে বিল্ড করাই এই প্রজেক্টের সঠিক যাচাই।

## ফাইল ম্যাপ
```
vpn-mvp/
  README.md                  <- এই ফাইল
  server/
    setup-wireguard.sh       <- সার্ভার সেটআপ
    add-peer.sh              <- Peer + ক্লায়েন্ট config বানায়
    remove-peer.sh           <- Peer config মুছে
    wg0.conf.example         <- সার্ভার config-এর নমুনা
    backend/
      app.py                 <- API সার্ভার (Python stdlib)
      run.sh
      servers.json.example
      users.json.example
      peers/                 <- add-peer দিয়ে বানানো .conf এখানে রাখতে হবে
  android/
    settings.gradle, build.gradle
    app/build.gradle
    app/src/main/AndroidManifest.xml
    app/src/main/java/com/example/vpnmvp/
      MainActivity.kt        <- UI: লগইন, সার্ভার লিস্ট, Connect
      ApiClient.kt           <- Backend কল
      VpnManager.kt          <- WireGuard tunnel চালু/বন্ধ
      Models.kt
```

## সতর্কতা, আবার
- র‍্যান্ডম ফ্রি পাবলিক VPN সার্ভার এই অ্যাপে যোগ করো না।
- AWS/Azure ট্রায়াল সার্ভারকে স্থায়ী সার্ভার বলে ইউজারকে দিও না।
- “১০০% অ্যানোনিমাস / স্ট্রিমিং গ্যারান্টি” দাবি করো না।
- আসল ইউজারের ডেটা নেওয়ার আগে Privacy Policy আর Play Store নিয়ম মেনে নাও।
