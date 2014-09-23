package com.ruimo.csv

import scala.collection.immutable

case class CsvHeader(
  header: immutable.Map[Symbol, Int]
) {
  def apply(key: Symbol): Int = header(key)
  def get(key: Symbol): Option[Int] = header.get(key)
}

object CsvHeader {
  def apply(cols: String*): CsvHeader = CsvHeader(
    cols.zipWithIndex.map {e => Symbol(e._1) -> e._2}.toMap
  )
  def fromSeq(cols: Seq[String]): CsvHeader = CsvHeader(
    cols.zipWithIndex.map {e => Symbol(e._1) -> e._2}.toMap
  )
}
