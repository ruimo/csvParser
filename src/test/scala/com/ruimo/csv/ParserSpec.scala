package com.ruimo.csv

import org.specs2.mutable.Specification
import com.ruimo.scoins.Scoping._
import scala.util.{Try, Failure, Success}
import java.nio.file.Files
import java.nio.charset.Charset

class ParserSpec extends Specification {
  "CSV parser" should {
    "parse empty string" in {
      Parser.parseOneLine("").get.size === 0
    }

    "parse empty string" in {
      val cols = Parser.parseOneLine("abc").get
      cols.size === 1
      cols(0) === "abc"
    }

    "parse quoted string" in {
      val cols = Parser.parseOneLine("\"a b\"\"c\"").get
      cols.size === 1
      cols(0) === "a b\"c"
    }

    "parse columns" in {
      val cols = Parser.parseOneLine("\"a b\"\"c\",b,\"c\"").get
      cols.size === 3
      cols(0) === "a b\"c"
      cols(1) === "b"
      cols(2) === "c"
    }

    "tailing quote" in {
      Parser.parseOneLine("\"a b\"\"").get must throwA[CsvParseException]
    }

    "non ended quote" in {
      Parser.parseOneLine("\"a b").get must throwA[CsvParseException]
    }

    "parse one line" in {
      val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,def".toIterator)
      z.hasNext === true
      val cols: Seq[String] = z.next.get
      cols.size === 2
      cols(0) === "abc"
      cols(1) === "def"
      z.hasNext === false
    }

    "parse one line with crlf" in {
      val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,def\r\n".toIterator)
      z.hasNext === true
      val cols: Seq[String] = z.next.get
      cols.size === 2
      cols(0) === "abc"
      cols(1) === "def"
      z.hasNext === false
    }

    "parse one line with lf" in {
      val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,def\n".toIterator)
      z.hasNext === true
      val cols: Seq[String] = z.next.get
      cols.size === 2
      cols(0) === "abc"
      cols(1) === "def"
      z.hasNext === false
    }

    "parse one line with quote and crlf" in {
      val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,\"def\"\r\n".toIterator)
      z.hasNext === true
      val cols: Seq[String] = z.next.get
      cols.size === 2
      cols(0) === "abc"
      cols(1) === "def"
      z.hasNext === false
    }

    "parse one line with quote and lf" in {
      val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,\"def\"\n".toIterator)
      z.hasNext === true
      val cols: Seq[String] = z.next.get
      cols.size === 2
      cols(0) === "abc"
      cols(1) === "def"
      z.hasNext === false
    }

    "parse empty line" in {
      val z: Iterator[Try[Seq[String]]] = Parser.parseLines("\r\n".toIterator)
      z.hasNext === true
      val cols: Seq[String] = z.next.get
      cols.size === 1
      cols(0) === ""
      z.hasNext === false
    }

    "parse col with crlf" in {
      val z: Iterator[Try[Seq[String]]] = Parser.parseLines("\"ab\r\ncd\",b".toIterator)
      z.hasNext === true
      val cols: Seq[String] = z.next.get
      cols.size === 2
      cols(0) === "ab\r\ncd"
      cols(1) === "b"
      z.hasNext === false
    }

    "parse two lines" in {
      val z: Iterator[Try[Seq[String]]] = Parser.parseLines("\"ab\r\ncd\",b\r\n\"de f\"".toIterator)
      z.hasNext === true

      doWith(z.next.get) { cols =>
        cols.size === 2
        cols(0) === "ab\r\ncd"
        cols(1) === "b"
      }

      z.hasNext === true
      doWith(z.next.get) { cols =>
        cols.size === 1
        cols(0) === "de f"
      }

      z.hasNext === false
    }

    "parse empty file" in {
      import com.ruimo.scoins.LoanPattern.iteratorFromReader
      import com.ruimo.csv.Parser.parseLines

      val file = Files.createTempFile(null, ".csv")
      val t = iteratorFromReader(Files.newBufferedReader(file, Charset.forName("Windows-31j"))) {
        in: Iterator[Char] => 
          val z: Iterator[Try[Seq[String]]] = parseLines(in)
          z.hasNext === false
      }
      t.isSuccess === true
    }

    "parse blank line" in {
      import com.ruimo.scoins.LoanPattern.iteratorFromReader
      import com.ruimo.csv.Parser.parseLines
      import java.util.Arrays.asList

      val file = Files.createTempFile(null, ".csv")
      Files.write(file, asList("", ""), Charset.forName("Windows-31j"))
      val t = iteratorFromReader(Files.newBufferedReader(file, Charset.forName("Windows-31j"))) {
        in: Iterator[Char] => 
          val z: Iterator[Try[Seq[String]]] = parseLines(in)
          z.hasNext === true
          z.next.get === Seq("")

          z.hasNext === true
          z.next.get === Seq("")

          z.hasNext === false
      }
      t.isSuccess === true
    }

    "parse normal lines" in {
      import com.ruimo.scoins.LoanPattern.iteratorFromReader
      import com.ruimo.csv.Parser.parseLines
      import java.util.Arrays.asList

      val file = Files.createTempFile(null, ".csv")
      Files.write(file, asList("a,\"b\"", "1,\"2 3\""), Charset.forName("Windows-31j"))
      val t = iteratorFromReader(Files.newBufferedReader(file, Charset.forName("Windows-31j"))) {
        in: Iterator[Char] => 
          val z: Iterator[Try[Seq[String]]] = parseLines(in)
          z.hasNext === true
          z.next.get === Seq("a", "b")

          z.hasNext === true
          z.next.get === Seq("1", "2 3")

          z.hasNext === false
      }
      t.isSuccess === true
    }

    "parse error" in {
      import com.ruimo.scoins.LoanPattern.iteratorFromReader
      import com.ruimo.csv.Parser.parseLines
      import java.util.Arrays.asList

      val file = Files.createTempFile(null, ".csv")
      Files.write(file, asList("a,\"b\"", "1,\"2 3"), Charset.forName("Windows-31j"))
      val t = iteratorFromReader(Files.newBufferedReader(file, Charset.forName("Windows-31j"))) {
        in: Iterator[Char] => 
          val z: Iterator[Try[Seq[String]]] = parseLines(in)
          z.hasNext === true
          z.next.get === Seq("a", "b")

          z.hasNext === true
          z.next match {
            case s: Success[Seq[String]] => failure
            case f: Failure[Seq[String]] => {
              f.exception.asInstanceOf[CsvParseException].lineNo === 2
            }
          }

          z.hasNext === false
      }
      t.get
      t.isSuccess === true
    }

    "parse with header when empty" in {
      import com.ruimo.csv.Parser._

      asHeaderedCsv(Iterator.empty).hasNext === false
    }

    "parse with header when error" in {
      import com.ruimo.csv.Parser._

      val it = asHeaderedCsv(Iterator.single(Failure(new Exception("Error"))))
      it.hasNext === true
      it.next() match {
        case Success(_) => failure
        case Failure(e) => e.getMessage === "Error"
      }
      1 === 1
    }

    "parse csv having only header" in {
      import com.ruimo.csv.Parser._

      val it = asHeaderedCsv(Iterator.single(Success(Seq("Hello", "World"))))
      it.hasNext === false
    }

    "parse csv read error" in {
      import com.ruimo.csv.Parser._

      val it = asHeaderedCsv(Iterator(
        Success(Seq("Hello", "World")),
        Failure(new Exception("Error"))
      ))
      it.hasNext === true
      it.next() match {
        case Success(_) => failure
        case Failure(e) => e.getMessage === "Error"
      }
      1 === 1
    }

    "parse csv having two lines" in {
      import com.ruimo.csv.Parser._

      val it = asHeaderedCsv(Iterator(
        Success(Seq("Name", "Age")),
        Success(Seq("Ruimo", "18")),
        Success(Seq("Uno", "16"))
      ))
      it.hasNext === true
      doWith(it.next().get) { l =>
        l.lineNo === 2
        l.header.header.size === 2
        l('Name) === "Ruimo"
        l('Age) === "18"
      }

      it.hasNext === true
      doWith(it.next().get) { l =>
        l.lineNo === 3
        l.header.header.size === 2
        l('Name) === "Uno"
        l('Age) === "16"
      }

      it.hasNext === false
    }

    "parse csv having header line is ok and second line is ng" in {
      import com.ruimo.csv.Parser._

      val it = asHeaderedCsv(Iterator(
        Success(Seq("Name", "Age")),
        Success(Seq("Ruimo", "18")),
        Failure(new Exception("Error"))
      ))
      it.hasNext === true
      doWith(it.next().get) { l =>
        l.lineNo === 2
        l.header.header.size === 2
        l('Name) === "Ruimo"
        l('Age) === "18"
      }

      it.hasNext === true
      it.next() match {
        case Success(_) => failure
        case Failure(e) => e.getMessage === "Error"
      }

      it.hasNext === false
    }
  }
}
