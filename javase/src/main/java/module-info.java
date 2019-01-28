/**
 * @author prelle
 *
 */
module jcodec.javase {
	exports org.jcodec.api.awt;
	exports org.jcodec.codecs.pngawt;
	exports org.jcodec.javase.scale;
	exports org.jcodec.javase.common;
	exports org.jcodec.api.transcode.filter;

	requires java.desktop;
	requires jcodec;
}