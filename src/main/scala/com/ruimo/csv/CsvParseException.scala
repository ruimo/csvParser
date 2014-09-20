package com.ruimo.csv

class CsvParseException(msg: String, cause: Throwable) extends Exception {
  def this(msg: String) = this(msg, null)
}
