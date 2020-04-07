package org.jcodec.common


infix fun Byte.shl(x: Int): Int = this.toInt() shl x
infix fun Byte.and(x: Int): Int = this.toInt() and x
infix fun Short.and(x: Int): Int = this.toInt() and x
