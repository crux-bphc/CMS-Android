package crux.bphc.cms.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.io.InputStream


/**
 * Attempts to decode an {@link InputStream} to an Glide internal representation.
 */
class SvgDecoder : ResourceDecoder<InputStream?, SVG?> {
    override fun handles(source: InputStream, options: Options): Boolean {
        return true // Assume we can decode an input stream, can't attempt a decode here
    }

    override fun decode(source: InputStream, width: Int, height: Int, options: Options):
            Resource<SVG?>? {
        return try {
            val svg = SVG.getFromInputStream(source)
            SimpleResource(svg)
        } catch (ex: SVGParseException) {
            return null; // We can't handle it, might not be an svg
        }
    }
}
