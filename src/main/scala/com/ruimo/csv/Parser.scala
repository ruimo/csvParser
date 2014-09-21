package com.ruimo.csv

import scala.util.control.TailCalls.TailRec
import scala.util.control.TailCalls._
import scala.util.{Try, Success, Failure}

// http://www.ietf.org/rfc/rfc4180.txt
object Parser {
  class IteratorWithLineNo(z: Iterator[Char]) extends Iterator[Char] {
    private var lineNo = 1

    def currentLineNo: Int = lineNo
    override def hasNext: Boolean = z.hasNext
    override def next(): Char = {
      val c = z.next()
      if (c == '\n') {
        lineNo += 1
      }
      c
    }
  }

  private[csv] def init(
    in: IteratorWithLineNo, buf: String, result: Seq[String]
  ): TailRec[Seq[String]] = if (in.hasNext) {
    val c = in.next
    if (c == '\r') tailcall(init(in, buf, result))
    else if (c == '\n') done(result :+ buf)
    else if (c == '"') tailcall(inQuote(in, buf, result))
    else if (c == ',') tailcall(init(in, "", result :+ buf))
    else tailcall(init(in, buf + c, result))
  } else {
    done(result :+ buf)
  }

  private[csv] def inQuote(
    in: IteratorWithLineNo, buf: String, result: Seq[String]
  ): TailRec[Seq[String]] = if (in.hasNext) {
    val c = in.next
    if (c == '"') tailcall(inQuoteQuote(in, buf, result))
    else tailcall(inQuote(in, buf + c, result))
  } else {
    throw new CsvParseException("Parse error. Quote is not closed.", in.currentLineNo)
  }

  private[csv] def inQuoteQuote(
    in: IteratorWithLineNo, buf: String, result: Seq[String]
  ): TailRec[Seq[String]] = if (in.hasNext) {
    val c = in.next
    if (c == ',') tailcall(init(in, "", result :+ buf))
    else if (c == '\r') tailcall(inQuoteQuote(in, buf, result))
    else if (c == '\n') done(result :+ buf)
    else if (c == '"') tailcall(inQuote(in, buf + '"', result))
    else throw new CsvParseException("Parse error. Invalid character '" + c + "' after quote.", in.currentLineNo)
  } else {
    done(result :+ buf)
  }

  def parseOneLine(s: String): Try[Seq[String]] = parseOneLine(s.toIterator)

  def parseOneLine(in: Iterator[Char]): Try[Seq[String]] =
    Try(if (! in.hasNext) Seq() else init(new IteratorWithLineNo(in), "", Vector()).result)

  def parseLines(in: Iterator[Char]): Iterator[Try[Seq[String]]] = if (in.hasNext) {
    new Iterator[Try[Seq[String]]] {
      private var buf: Option[Try[Seq[String]]] = None

      override def hasNext: Boolean = {
        if (buf.isEmpty) {
          parseOneLine(in) match {
            case s: Success[Seq[String]] => {
              if (s.get.isEmpty) buf = None
              else buf = Some(s)
            }
            case f: Failure[Seq[String]] => buf = Some(f)
          }
        }

        buf.isDefined
      }

      override def next(): Try[Seq[String]] = if (hasNext) {
        try {
          buf.get 
        }
        finally {
          buf = None
        }
      } else throw new NoSuchElementException
    }
  }
  else {
    Iterator.empty
  }
}
