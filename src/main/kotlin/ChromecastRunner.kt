import org.slf4j.Logger
import org.slf4j.LoggerFactory
import su.litvak.chromecast.api.v2.ChromeCast
import su.litvak.chromecast.api.v2.ChromeCastConnectionEvent
import su.litvak.chromecast.api.v2.ChromeCastConnectionEventListener
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEvent
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEventListener
import su.litvak.chromecast.api.v2.ChromeCasts
import su.litvak.chromecast.api.v2.ChromeCastsListener
import su.litvak.chromecast.api.v2.Media
import su.litvak.chromecast.api.v2.MediaStatus
import su.litvak.chromecast.api.v2.Status
import java.util.Timer
import kotlin.concurrent.thread
import kotlin.concurrent.timer

class ChromecastRunner(val logger: Logger) : ChromeCastsListener {
  val listeners: MutableMap<String, ChromecastListener> = LinkedHashMap()

  fun start() {
    logger.info("Looking for ChromeCasts...")
    ChromeCasts.registerListener(this)
    ChromeCasts.startDiscovery()
  }

  override fun newChromeCastDiscovered(chromeCast: ChromeCast) {
    logger.info("Found a chromecast: {}", chromeCast.title)

    val listener = ChromecastListener(logger, chromeCast)
    listeners[chromeCast.address] = listener

    listener.initialize()
  }

  override fun chromeCastRemoved(chromeCast: ChromeCast) {
    logger.info("Killed a chromecast: {}", chromeCast.title)
    listeners[chromeCast.address]?.destroy()
  }
}

class ChromecastListener(val logger: Logger, val chromeCast: ChromeCast) : ChromeCastSpontaneousEventListener, ChromeCastConnectionEventListener {
  var timer: Timer? = null

  fun initialize() {
    logger.info("initializing")
    chromeCast.registerListener(this)
    chromeCast.registerConnectionListener(this)

    val status = chromeCast.status
    if (status != null) {
      startTimer()
    }
  }

  fun destroy() {
    stopTimer()
    chromeCast.unregisterListener(this)
    chromeCast.unregisterConnectionListener(this)
  }

  fun startTimer() {
    this.timer = timer("timer for ${chromeCast.name}", false, 0, 1000) {
      val status = chromeCast.status
      printStatus(status)

      val runningApp = status.runningApp
      if (runningApp != null) {
        val mediaStatus: MediaStatus? = chromeCast.mediaStatus
        if (mediaStatus != null) {
          printMetaData(mediaStatus)
        }
      }
    }
  }

  fun stopTimer() {
    timer?.cancel()
    timer = null
  }

  override fun connectionEventReceived(event: ChromeCastConnectionEvent) {
    if (event.isConnected) {
      // Let the timer be a timer.
    } else {
      stopTimer()
    }
  }

  override fun spontaneousEventReceived(event: ChromeCastSpontaneousEvent) {
    val type = event.type
    val data = event.getData(type.dataClass)
    logger.info("{} Received a spontaneous event: {}", chromeCast.title, data)

    when (data) {
      is Status -> {
        printStatus(data)
      }
      is MediaStatus -> {
        printMetaData(data)
      }
      else -> {
        logger.info("Not handling message: $data")
      }
    }
  }

  private fun printStatus(status: Status) {
    logger.info("{} Received status update", chromeCast.title)
    logger.info("  app: {}", status.runningApp?.name)
    logger.info("  volume: {}", status.volume.level)
  }

  private fun printMetaData(mediaStatus: MediaStatus) {
    val media: Media? = mediaStatus.media
    val metadata = media?.metadata

    logger.info("  currentTime: {}", mediaStatus.currentTime.toInt())
    logger.info("  duration: {}", media?.duration?.toInt())

    logger.info("  artist: {}", metadata?.getOrDefault(Media.METADATA_ALBUM_ARTIST, ""))
    logger.info("  title: {}", metadata?.getOrDefault(Media.METADATA_TITLE, ""))
    logger.info("  album: {}", metadata?.getOrDefault(Media.METADATA_ALBUM_NAME, ""))
  }
}

fun main(args: Array<String>) {
  System.setProperty("org.slf4j.simpleloggerger.defaultloggerLevel", "debug")
  System.setProperty("org.slf4j.simpleloggerger.showDateTime", "true")
  val logger = LoggerFactory.getLogger(ChromecastRunner::class.java)

  // Start it up.
  ChromecastRunner(logger).start()
}
