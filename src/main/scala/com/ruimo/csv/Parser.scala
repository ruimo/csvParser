package com.ruimo.csv

import scala.util.control.TailCalls.TailRec
import scala.util.control.TailCalls._
import scala.util.Try

// http://www.ietf.org/rfc/rfc4180.txt
object Parser {
  private[csv] def init(in: List[Char], buf: String, result: Seq[String]): TailRec[Seq[String]] = in match {
    case Nil => done(result :+ buf.toString)
    case c::tail =>
      if (c == '"') tailcall(inQuote(tail, buf, result))
      else if (c == ',') tailcall(init(tail, "", result :+ buf))
      else tailcall(init(tail, buf + c, result))
  }

  private[csv] def inQuote(in: List[Char], buf: String, result: Seq[String]): TailRec[Seq[String]] = in match {
    case Nil => throw new CsvParseException("Parse error. Quote is not closed.")
    case c::tail =>
      if (c == '"') tailcall(inQuoteQuote(tail, buf, result))
      else tailcall(inQuote(tail, buf + c, result))
  }

  private[csv] def inQuoteQuote(in: List[Char], buf: String, result: Seq[String]): TailRec[Seq[String]] = in match {
    case Nil => done(result :+ buf)
    case c::tail =>
      if (c == ',') tailcall(init(tail, "", result :+ buf))
      else if (c == '"') tailcall(inQuote(tail, buf + '"', result))
      else throw new CsvParseException("Parse error. Invalid character '" + c + "' after quote.")
  }

  def parse(s: String): Try[Seq[String]] =
    Try(if (s.isEmpty) Seq() else init(s.toList, "", Vector()).result)
}
