package org.jcodec.common.logging

class Message(val level: LogLevel, val fileName: String, val className: String, val methodName: String, val lineNumber: Int, val message: String, val args: Array<Any>?)