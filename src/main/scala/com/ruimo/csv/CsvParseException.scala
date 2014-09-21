package com.ruimo.csv

class CsvParseException(
  msg: String, cause: Throwable, val lineNo: Int
) extends Exception("line " + lineNo + ": " + msg, cause) {
  def this(msg: String, lineNo: Int) = this(msg, null, lineNo)
}
