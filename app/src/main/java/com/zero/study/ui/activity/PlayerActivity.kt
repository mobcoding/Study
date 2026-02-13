package com.zero.study.ui.activity

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.zero.base.activity.BaseActivity
import com.zero.study.databinding.ActivityPlayerBinding

private const val string = "https://rr1---sn-jhi3-i3bk.googlevideo.com/videoplayback?expire=1766139918&ei=rtNEaaGdK7_I0-kP8qenkAs&ip=182.239.92.99&id=o-AFnlaoq6m3sVsYOrahRQPoG2-Ik2A6wrCxlxMiHlV6ge&itag=18&source=youtube&requiressl=yes&xpc=EgVo2aDSNQ%3D%3D&cps=0&met=1766118318%2C&mh=mC&mm=31%2C29&mn=sn-jhi3-i3bk%2Csn-i3belnl6&ms=au%2Crdu&mv=m&mvi=1&pl=24&rms=au%2Cau&initcwndbps=630000&bui=AYUSA3Dm2myTYVwzkMml3s3EluuyjgJVQ_3grDfg6C3kXl0POpjH_BYEHemzEnhvtAwBRkGIHxb7np6x&spc=wH4QqzTA31YlL24DvWAc&vprv=1&svpuc=1&mime=video%2Fmp4&rqh=1&cnr=14&ratebypass=yes&dur=212.787&lmt=1664099322807161&mt=1766118056&fvip=5&fexp=51552689%2C51565116%2C51565681%2C51580968&c=ANDROID&txp=5538434&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cxpc%2Cbui%2Cspc%2Cvprv%2Csvpuc%2Cmime%2Crqh%2Ccnr%2Cratebypass%2Cdur%2Clmt&sig=AJfQdSswRAIgMP1cAzUhvQztcPeJu1c34i8FzXUvNVQ8m3LNqgRjiycCIGWvBmW2hg70Qo9brkWX3SLTc0odiHGQlCFzupVWv2Rd&lsparams=cps%2Cmet%2Cmh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Crms%2Cinitcwndbps&lsig=APaTxxMwRAIgb76FzxjlPZvmaOC-_Jn-6M2djqEh8YIF8htaQp_saWoCIB-dHymsbSPa74TQJP0yyfEZ_k63_lpEGHCiX8NY0lRT"

/**
 * @date:2024/9/19 18:17
 * @path:com.zero.study.ui.activity.TakePhoto
 */
class PlayerActivity : BaseActivity<ActivityPlayerBinding>(ActivityPlayerBinding::inflate) {

    private lateinit var player: ExoPlayer
    override fun initView() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        playVideo()
    }

    override fun initData() {
    }

    override fun addListener() {
    }

    private fun playVideo() {
//        val videoUrl = "https://rr1---sn-jhi3-i3bk.googlevideo.com/videoplayback?expire=1766139918&ei=rtNEaaGdK7_I0-kP8qenkAs&ip=182.239.92.99&id=o-AFnlaoq6m3sVsYOrahRQPoG2-Ik2A6wrCxlxMiHlV6ge&itag=18&source=youtube&requiressl=yes&xpc=EgVo2aDSNQ%3D%3D&cps=0&met=1766118318%2C&mh=mC&mm=31%2C29&mn=sn-jhi3-i3bk%2Csn-i3belnl6&ms=au%2Crdu&mv=m&mvi=1&pl=24&rms=au%2Cau&initcwndbps=630000&bui=AYUSA3Dm2myTYVwzkMml3s3EluuyjgJVQ_3grDfg6C3kXl0POpjH_BYEHemzEnhvtAwBRkGIHxb7np6x&spc=wH4QqzTA31YlL24DvWAc&vprv=1&svpuc=1&mime=video%2Fmp4&rqh=1&cnr=14&ratebypass=yes&dur=212.787&lmt=1664099322807161&mt=1766118056&fvip=5&fexp=51552689%2C51565116%2C51565681%2C51580968&c=ANDROID&txp=5538434&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cxpc%2Cbui%2Cspc%2Cvprv%2Csvpuc%2Cmime%2Crqh%2Ccnr%2Cratebypass%2Cdur%2Clmt&sig=AJfQdSswRAIgMP1cAzUhvQztcPeJu1c34i8FzXUvNVQ8m3LNqgRjiycCIGWvBmW2hg70Qo9brkWX3SLTc0odiHGQlCFzupVWv2Rd&lsparams=cps%2Cmet%2Cmh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Crms%2Cinitcwndbps&lsig=APaTxxMwRAIgb76FzxjlPZvmaOC-_Jn-6M2djqEh8YIF8htaQp_saWoCIB-dHymsbSPa74TQJP0yyfEZ_k63_lpEGHCiX8NY0lRT"
//        val videoUrl = "https://rr2---sn-jhi3-i3bk.googlevideo.com/videoplayback?expire=1766140067&ei=Q9REadWoGI2GssUP4dvBmAc&ip=182.239.92.99&id=o-AFuvC0k5haMS7J_3CmIarsAM1aHdRwngvqTPbqxQEGQf&itag=18&source=youtube&requiressl=yes&xpc=EgVo2aDSNQ%3D%3D&cps=0&met=1766118467%2C&mh=ie&mm=31%2C29&mn=sn-jhi3-i3bk%2Csn-i3bssn7e&ms=au%2Crdu&mv=m&mvi=2&pl=24&rms=au%2Cau&initcwndbps=630000&bui=AYUSA3CSl63ynLUQN7OKQ2PSI-qInNHBnuEsUJwDJWi9NXkOKsGeThb1j3ER3UjBDGySoGEYBJwLAoY_&spc=wH4QqyyjBBe6tBFeqLwB&vprv=1&svpuc=1&mime=video%2Fmp4&rqh=1&cnr=14&ratebypass=yes&dur=239.490&lmt=1765548782828342&mt=1766118056&fvip=2&fexp=51552689%2C51565116%2C51565682%2C51580968&c=ANDROID&txp=4538534&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cxpc%2Cbui%2Cspc%2Cvprv%2Csvpuc%2Cmime%2Crqh%2Ccnr%2Cratebypass%2Cdur%2Clmt&sig=AJfQdSswRQIgf5quBJaZaLoAcN1Gs_tnMlJRvrgYi7H9cgPZUTTBSe0CIQDcl5PhXfxViJ3hnayFxL3Tl_ZaeZA9pjdB-462d-PSkg%3D%3D&lsparams=cps%2Cmet%2Cmh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Crms%2Cinitcwndbps&lsig=APaTxxMwRAIgWkQ3YEZWsyoQ8k25vnAqB1O6ts-dFG6rLfWNpTWO-3MCIGpynhtAV7z5ONlsnC_HLYd65sUNxGLtUviv5awL73GF"
//        val videoUrl="https://rr1---sn-jhi3-i3b6.googlevideo.com/videoplayback?expire=1766146501&ei=Ze1Eac--HYX1pt8PgfLhgQs&ip=182.239.92.99&id=o-AFDAguJXUQ38_mBUdDsg1Q_IF3txnoPDyWjvgNEvpJ-X&itag=18&source=youtube&requiressl=yes&xpc=EgVo2aDSNQ%3D%3D&cps=0&met=1766124901%2C&mh=-w&mm=31%2C29&mn=sn-jhi3-i3b6%2Csn-i3b7knsl&ms=au%2Crdu&mv=m&mvi=1&pl=24&rms=au%2Cau&gcr=hk&initcwndbps=641250&bui=AYUSA3DPjptoDqI7LgyfAzI30XCoCbeaiB4ovSf5ZJ-9HLLgZL4TZFbc9eSUvf3HehF24iJCY5o_L-2q&spc=wH4QqzrYpJh-OJpFBhvd&vprv=1&svpuc=1&mime=video%2Fmp4&rqh=1&cnr=14&ratebypass=yes&dur=243.205&lmt=1751497640089139&mt=1766124297&fvip=3&fexp=51552689%2C51565116%2C51565682%2C51580968&c=ANDROID&txp=4538534&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cxpc%2Cgcr%2Cbui%2Cspc%2Cvprv%2Csvpuc%2Cmime%2Crqh%2Ccnr%2Cratebypass%2Cdur%2Clmt&sig=AJfQdSswRgIhALCaSlS8ofi5l71lczzUwK0h8i26YziwNvFYSzCwtWN8AiEA6eKagVUgMmAhhwdtR3c9AVSzy6HfqhkPQHEFz6H3rCs%3D&lsparams=cps%2Cmet%2Cmh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Crms%2Cinitcwndbps&lsig=APaTxxMwRgIhALE1GFyK8neb-9FyjGoaKWxN5Jn5yucXg3P3w7GNssV6AiEAugaFgJZDr6ukMM31u3x_r9elHVz4gpb97EGlIfRKURM%3D"
      val videoUrl = "https://rr2---sn-jhi3-i3b6.googlevideo.com/videoplayback?expire=1769783532&ei=jGx8aZK5EYiFpt8P8c3GiA4&ip=182.239.122.21&id=o-AF-CPdYmasAHwkwWfPjUQ5om5aApUjcRt2IGgkqmxadO&itag=18&source=youtube&requiressl=yes&xpc=EgVo2aDSNQ%3D%3D&cps=681&met=1769761932%2C&mh=-5&mm=31%2C29&mn=sn-jhi3-i3b6%2Csn-oji3bc-5j&ms=au%2Crdu&mv=m&mvi=2&pl=24&rms=au%2Cau&gcr=hk&initcwndbps=581250&bui=AW-iu_rOu8OaQWTUlaMguPN0d5e5c4mBQCblsDqrOxflsz6Vxt5lHJzjLtbdlTrn0srsTHvltQbMUB6r&spc=q5xjPJK8fGdIxTjJiNYFlUr5EkLJaJ7RCkPXyzNuo8dKirJmZNlXII0x&vprv=1&svpuc=1&mime=video%2Fmp4&rqh=1&cnr=14&ratebypass=yes&dur=216.920&lmt=1762159916347924&mt=1769760775&fvip=4&fexp=51552689%2C51565115%2C51565681%2C51580968&c=ANDROID&txp=5538534&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cxpc%2Cgcr%2Cbui%2Cspc%2Cvprv%2Csvpuc%2Cmime%2Crqh%2Ccnr%2Cratebypass%2Cdur%2Clmt&sig=AJEij0EwRgIhAK1hK1A_BzoJwwy60FgFlr6UntoKgPcD0UqR-o60CBnbAiEA_OWOpv-C0DRZ5eOsZvQTuxgzWcpKu1a_1wIm17vZ0Uo%3D&lsparams=cps%2Cmet%2Cmh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Crms%2Cinitcwndbps&lsig=APaTxxMwRgIhANJaub9QpzONCo96i1pSRHC-rXbF3QQSV2xC8e0gDWASAiEAnOhfN_lMhhBlRT6sPZvYD8hmkR2xfjXHfWjq8nMw1Q0%3D"
        val mediaItem = MediaItem.fromUri(videoUrl)


        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}