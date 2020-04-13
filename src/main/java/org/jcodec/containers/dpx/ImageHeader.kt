package org.jcodec.containers.dpx

class ImageHeader(val orientation: Short,
                  val numberOfImageElements: Short,
                  val linesPerImageElement: Int,
                  val pixelsPerLine: Int,
                  val imageElement1: ImageElement)