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
    val metaData = media.metaData
    val metaDataHelper = metaData.metaData

    logger.info("  volume: {}", mediaStatus.volume)
    logger.info("  currentTime: {}", mediaStatus.currentTime.toInt())
    logger.info("  duration: {}", media.duration.toInt())

    when (metaDataHelper) {
      is Media.MetaData.GenericMetaData -> {
        logger.info("  artist: {}", metaDataHelper.artist())
        logger.info("  title: {}", metaDataHelper.title())
        logger.info("  subtitle: {}", metaDataHelper.subtitle())
        logger.info("  images: {}", Arrays.toString(metaDataHelper.images()))
      }
      is Media.MetaData.MovieMetaData -> {
        logger.info("  title: {}", metaDataHelper.title())
        logger.info("  subtitle: {}", metaDataHelper.subtitle())
        logger.info("  studio: {}", metaDataHelper.studio())
        logger.info("  releaseDate: {}", metaDataHelper.releaseDate())
        logger.info("  images: {}", Arrays.toString(metaDataHelper.images()))
      }
      is Media.MetaData.TvShowMetaData -> {
        logger.info("  seriesTitle: {}", metaDataHelper.seriesTitle())
        logger.info("  season: {}", metaDataHelper.seasonNumber())
        logger.info("  episode: {}", metaDataHelper.episodeNumber())
        logger.info("  title: {}", metaDataHelper.title())
        logger.info("  broadcastDate: {}", metaDataHelper.broadcastDate())
        logger.info("  releaseDate: {}", metaDataHelper.releaseDate())
        logger.info("  images: {}", Arrays.toString(metaDataHelper.images()))
      }
      is Media.MetaData.MusicTrackMetaData -> {
        logger.info("  artist: {}", metaDataHelper.artist())
        logger.info("  title: {}", metaDataHelper.title())
        logger.info("  albumTitle: {}", metaDataHelper.albumTitle())
        logger.info("  albumArtist: {}", metaDataHelper.albumArtist())
        logger.info("  composer: {}", metaDataHelper.composer())
        logger.info("  disc: {}", metaDataHelper.discNumber())
        logger.info("  track: {}", metaDataHelper.trackNumber())
        logger.info("  releaseDate: {}", metaDataHelper.releaseDate())
        logger.info("  images: {}", Arrays.toString(metaDataHelper.images()))
      }
      is Media.MetaData.PhotoMetaData -> {
        logger.info("  artist: {}", metaDataHelper.artist())
        logger.info("  title: {}", metaDataHelper.title())
        logger.info("  creationDate: {}", metaDataHelper.creationDate())
        logger.info("  height: {}", metaDataHelper.height())
        logger.info("  width: {}", metaDataHelper.width())
        logger.info("  locationName: {}", metaDataHelper.locationName())
        logger.info("  locationLat: {}", metaDataHelper.locationLatitude())
        logger.info("  locationLon: {}", metaDataHelper.locationLongitude())
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
