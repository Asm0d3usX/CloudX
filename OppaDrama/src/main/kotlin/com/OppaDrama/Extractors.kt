package com.oppadrama

import com.lagradost.api.Log
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.VidHidePro

// 🎬 Extractor untuk Smoothpre / EarnVids
class Smoothpre : VidHidePro() {
    // ✨ Nama server / extractor
    override var name = "EarnVids 💸"

    // 🌐 URL utama situs
    override var mainUrl = "https://smoothpre.com"
}
