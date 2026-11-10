package com.grantlittman.wearapp.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.grantlittman.wearapp.R
import com.grantlittman.wearapp.presentation.MainActivity

/**
 * Watch face complication — icon-only quick launch for PulseTimer.
 * Keeps SMALL_IMAGE for color slots and supports monochrome icon slots as well.
 */
class PulseTimerComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return buildComplication(type)
    }

    override suspend fun onComplicationRequest(
        request: ComplicationRequest
    ): ComplicationData? {
        return buildComplication(request.complicationType)
    }

    private fun buildComplication(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SMALL_IMAGE &&
            type != ComplicationType.MONOCHROMATIC_IMAGE
        ) return null

        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeIcon = Icon.createWithResource(this, R.drawable.ic_complication)
        val ambientIcon = Icon.createWithResource(this, R.drawable.ic_complication_ambient)

        if (type == ComplicationType.MONOCHROMATIC_IMAGE) {
            // Reuse the white transparent artwork so the watch face can tint it.
            return MonochromaticImageComplicationData.Builder(
                monochromaticImage = MonochromaticImage.Builder(ambientIcon)
                    .setAmbientImage(ambientIcon)
                    .build(),
                contentDescription = PlainComplicationText.Builder("Open PulseTimer").build()
            )
                .setTapAction(launchIntent)
                .build()
        }

        return SmallImageComplicationData.Builder(
            smallImage = SmallImage.Builder(activeIcon, SmallImageType.ICON)
                .setAmbientImage(ambientIcon)
                .build(),
            contentDescription = PlainComplicationText.Builder("Open PulseTimer").build()
        )
            .setTapAction(launchIntent)
            .build()
    }
}
