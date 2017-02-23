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
    sep: Char, in: IteratorWithLineNo, buf: String, result: Seq[String]
  ): TailRec[Seq[String]] = if (in.hasNext) {
    val c = in.next
    if (c == '\r') tailcall(init(sep, in, buf, result))
    else if (c == '\n') done(result :+ buf)
    else if (c == '"') tailcall(inQuote(sep, in, buf, result))
    else if (c == sep) tailcall(init(sep, in, "", result :+ buf))
    else tailcall(init(sep, in, buf + c, result))
  } else {
    done(result :+ buf)
  }

  private[csv] def inQuote(
    sep: Char, in: IteratorWithLineNo, buf: String, result: Seq[String]
  ): TailRec[Seq[String]] = if (in.hasNext) {
    val c = in.next
    if (c == '"') tailcall(inQuoteQuote(sep, in, buf, result))
    else tailcall(inQuote(sep, in, buf + c, result))
  } else {
    throw new CsvParseException("Parse error. Quote is not closed.", in.currentLineNo)
  }

  private[csv] def inQuoteQuote(
    sep: Char, in: IteratorWithLineNo, buf: String, result: Seq[String]
  ): TailRec[Seq[String]] = if (in.hasNext) {
    val c = in.next
    if (c == sep) tailcall(init(sep, in, "", result :+ buf))
    else if (c == '\r') tailcall(inQuoteQuote(sep, in, buf, result))
    else if (c == '\n') done(result :+ buf)
    else if (c == '"') tailcall(inQuote(sep, in, buf + '"', result))
    else throw new CsvParseException("Parse error. Invalid character '" + c + "' after quote.", in.currentLineNo)
  } else {
    done(result :+ buf)
  }

  def parseOneLine(s: String, sep: Char = ','): Try[Seq[String]] = parseOneLine(s.toIterator, sep)

  def parseOneLine(in: Iterator[Char], sep: Char): Try[Seq[String]] =
    Try(if (! in.hasNext) Seq() else init(sep, new IteratorWithLineNo(in), "", Vector()).result)

  def parseLines(in: Iterator[Char], sep: Char = ','): Iterator[Try[Seq[String]]] = if (in.hasNext) {
    new Iterator[Try[Seq[String]]] {
      private var buf: Option[Try[Seq[String]]] = None

      override def hasNext: Boolean = {
        if (buf.isEmpty) {
          parseOneLine(in, sep) match {
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

  def asHeaderedCsv(in: Iterator[Try[Seq[String]]]): Iterator[Try[CsvRecord]] = {
    if (! in.hasNext) Iterator.empty
    else {
      in.next.map {colSeq => CsvHeader.fromSeq(colSeq)} match {
        case Failure(e) => Iterator.single(Failure[CsvRecord](e))
        case Success(header) => new Iterator[Try[CsvRecord]] {
          private var rec: Option[Try[CsvRecord]] = None
          private var lineNo = 2

          override def hasNext: Boolean = {
            if (! rec.isDefined && in.hasNext) {
              rec = Some(in.next.map {cols => CsvRecord(lineNo, header, cols.toVector)})
              lineNo += 1
            }
            rec.isDefined
          }

          override def next(): Try[CsvRecord] = if (hasNext) {
            try {
              rec.get
            }
            finally {
              rec = None
            }
          }
          else throw new NoSuchElementException
        }
      }
    }
  }
}
