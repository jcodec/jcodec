package org.jcodec.common


infix fun Byte.shl(x: Int): Int = this.toInt() shl x
infix fun Byte.and(x: Byte): Int = this.toInt() and x.toInt()
infix fun Byte.and(x: Int): Int = this.toInt() and x
infix fun Byte.or(x: Int): Int = this.toInt() or x
infix fun Short.and(x: Int): Int = this.toInt() and x
