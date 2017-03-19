
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import su.litvak.chromecast.api.v2.ChromeCast
import su.litvak.chromecast.api.v2.ChromeCastConnectionEvent
import su.litvak.chromecast.api.v2.ChromeCastConnectionEventListener
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEvent
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEventListener
import su.litvak.chromecast.api.v2.ChromeCasts
import su.litvak.chromecast.api.v2.ChromeCastsListener
import su.litvak.chromecast.api.v2.MediaMetadata
import su.litvak.chromecast.api.v2.MediaStatus
import java.util.Arrays

class ChromecastRunner(val logger: Logger) : ChromeCastsListener, ChromeCastSpontaneousEventListener, ChromeCastConnectionEventListener {
  fun start() {
    logger.info("Looking for ChromeCasts...")
    ChromeCasts.registerListener(this)
    ChromeCasts.startDiscovery()
  }

  override fun newChromeCastDiscovered(chromeCast: ChromeCast) {
    logger.info("Found a chromecast: {}", chromeCast.title)
    chromeCast.registerListener(this)
    chromeCast.registerConnectionListener(this)

    try {
      val runningApp = chromeCast.status.runningApp

      if (runningApp != null && !runningApp.isIdleScreen) {
        logger.info("{} is running app: {}", chromeCast.title, runningApp.name)
        printMetaData(chromeCast.mediaStatus)
      } else {
        logger.info("{} is : {}", chromeCast.title, chromeCast.status)
      }
    } catch (e: java.io.IOException) {
      logger.error("Running app failed", e)
    }
  }

  override fun chromeCastRemoved(chromeCast: ChromeCast) {
    logger.info("Killed a chromecast: {}", chromeCast.title)
    chromeCast.unregisterListener(this)
    chromeCast.unregisterConnectionListener(this)
  }

  override fun connectionEventReceived(event: ChromeCastConnectionEvent) {
    logger.info("Received a connection event: {}", event.isConnected)
  }

  override fun spontaneousEventReceived(event: ChromeCastSpontaneousEvent) {
    val type = event.type
    val data = event.getData(type.dataClass)
    logger.info("Received a spontaneous event: {}", data)

    when (data) {
      is MediaStatus -> {
        if (data.media != null) {
          printMetaData(data)
        }
      }
      else -> {
        logger.info("Not handling message: $data")
      }
    }
  }

  private fun printMetaData(mediaStatus: MediaStatus) {
    val media = mediaStatus.media
    val metadata = media.metadata

    logger.info("  volume: {}", mediaStatus.volume)
    logger.info("  currentTime: {}", mediaStatus.currentTime.toInt())
    logger.info("  duration: {}", media.duration.toInt())

    when (metadata) {
      is MediaMetadata.GenericMetaData -> {
        logger.info("  artist: {}", metadata.artist())
        logger.info("  title: {}", metadata.title())
        logger.info("  subtitle: {}", metadata.subtitle())
        logger.info("  images: {}", Arrays.toString(metadata.images()))
      }
      is MediaMetadata.MovieMetaData -> {
        logger.info("  title: {}", metadata.title())
        logger.info("  subtitle: {}", metadata.subtitle())
        logger.info("  studio: {}", metadata.studio())
        logger.info("  releaseDate: {}", metadata.releaseDate())
        logger.info("  images: {}", Arrays.toString(metadata.images()))
      }
      is MediaMetadata.TvShowMetaData -> {
        logger.info("  seriesTitle: {}", metadata.seriesTitle())
        logger.info("  season: {}", metadata.seasonNumber())
        logger.info("  episode: {}", metadata.episodeNumber())
        logger.info("  title: {}", metadata.title())
        logger.info("  broadcastDate: {}", metadata.broadcastDate())
        logger.info("  releaseDate: {}", metadata.releaseDate())
        logger.info("  images: {}", Arrays.toString(metadata.images()))
      }
      is MediaMetadata.MusicTrackMetaData -> {
        logger.info("  artist: {}", metadata.artist())
        logger.info("  title: {}", metadata.title())
        logger.info("  albumName: {}", metadata.albumName())
        logger.info("  albumArtist: {}", metadata.albumArtist())
        logger.info("  composer: {}", metadata.composer())
        logger.info("  disc: {}", metadata.discNumber())
        logger.info("  track: {}", metadata.trackNumber())
        logger.info("  releaseDate: {}", metadata.releaseDate())
        logger.info("  images: {}", Arrays.toString(metadata.images()))
      }
      is MediaMetadata.PhotoMetaData -> {
        logger.info("  artist: {}", metadata.artist())
        logger.info("  title: {}", metadata.title())
        logger.info("  creationDate: {}", metadata.creationDate())
        logger.info("  height: {}", metadata.height())
        logger.info("  width: {}", metadata.width())
        logger.info("  locationName: {}", metadata.locationName())
        logger.info("  locationLat: {}", metadata.locationLatitude())
        logger.info("  locationLon: {}", metadata.locationLongitude())
      }
    }
  }
}

object ChromecastRunnerObject {
  @JvmStatic fun main(args: Array<String>) {
    System.setProperty("org.slf4j.simpleloggerger.defaultloggerLevel", "debug")
    System.setProperty("org.slf4j.simpleloggerger.showDateTime", "true")
    val logger = LoggerFactory.getLogger(ChromecastRunner::class.java)

    // Start it up.
    ChromecastRunner(logger).start()
  }
}
