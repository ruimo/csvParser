package com.ruimo.csv

import scala.collection.immutable

case class CsvRecord(
  lineNo: Int,
  header: CsvHeader,
  cols: immutable.Vector[String]
) {
  def apply(key: Symbol): String = cols(header(key))
  def get(key: Symbol): Option[String] = header.get(key).map(idx => cols(idx))
}
