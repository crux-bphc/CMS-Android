package crux.bphc.cms.glide

import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import com.caverock.androidsvg.SVG

/**
 * Transcodes the Svg image to an Android Vector Resource with the help of AndroidSVG.
 */
class SvgDrawableTranscoder : ResourceTranscoder<SVG?, PictureDrawable?> {
    override fun transcode(toTranscode: Resource<SVG?>, options: Options): Resource<PictureDrawable?>? {
        val svg = toTranscode.get()
        val picture = svg.renderToPicture()
        val drawable = PictureDrawable(picture)
        return SimpleResource(drawable)
    }
}
