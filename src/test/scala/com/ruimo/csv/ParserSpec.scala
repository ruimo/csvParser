package com.ruimo.csv

import com.ruimo.scoins.Scoping._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.util.{Failure, Success, Try}
import java.nio.file.Files
import java.nio.charset.Charset

class ParserSpec extends AnyFlatSpec with should.Matchers {
  it should "parse empty string" in {
    assert(Parser.parseOneLine("").get.size === 0)
  }

  it should "parse some string" in {
    val cols = Parser.parseOneLine("abc").get
    assert(cols.size === 1)
    assert(cols(0) === "abc")
  }

  it should "parse quoted string" in {
    val cols = Parser.parseOneLine("\"a b\"\"c\"").get
    assert(cols.size === 1)
    assert(cols(0) === "a b\"c")
  }

  it should "parse columns" in {
    val cols = Parser.parseOneLine("\"a b\"\"c\",b,\"c\"").get
    assert(cols.size === 3)
    assert(cols(0) === "a b\"c")
    assert(cols(1) === "b")
    assert(cols(2) === "c")
  }

  it should "parse tab separated columns" in {
    val cols = Parser.parseOneLine("\"a b\"\"c\"\tb\t\"c\"", '\t').get
    assert(cols.size === 3)
    assert(cols(0) === "a b\"c")
    assert(cols(1) === "b")
    assert(cols(2) === "c")
  }

  it should "tailing quote" in {
    assertThrows[CsvParseException](Parser.parseOneLine("\"a b\"\"").get)
  }

  it should "non ended quote" in {
    assertThrows[CsvParseException](Parser.parseOneLine("\"a b").get)
  }

  it should "parse one line" in {
    val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,def".toIterator)
    assert(z.hasNext === true)
    val cols: Seq[String] = z.next.get
    assert(cols.size === 2)
    assert(cols(0) === "abc")
    assert(cols(1) === "def")
    assert(z.hasNext === false)
  }

  it should "parse one line with tab separated" in {
    val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc\tdef".toIterator, '\t')
    assert(z.hasNext === true)
    val cols: Seq[String] = z.next.get
    assert(cols.size === 2)
    assert(cols(0) === "abc")
    assert(cols(1) === "def")
    assert(z.hasNext === false)
  }

  it should "parse one line with crlf" in {
    val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,def\r\n".toIterator)
    assert(z.hasNext === true)
    val cols: Seq[String] = z.next.get
    assert(cols.size === 2)
    assert(cols(0) === "abc")
    assert(cols(1) === "def")
    assert(z.hasNext === false)
  }

  it should "parse one line with lf" in {
    val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,def\n".toIterator)
    assert(z.hasNext === true)
    val cols: Seq[String] = z.next.get
    assert(cols.size === 2)
    assert(cols(0) === "abc")
    assert(cols(1) === "def")
    assert(z.hasNext === false)
  }

  it should "parse one line with quote and crlf" in {
    val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,\"def\"\r\n".toIterator)
    assert(z.hasNext === true)
    val cols: Seq[String] = z.next.get
    assert(cols.size === 2)
    assert(cols(0) === "abc")
    assert(cols(1) === "def")
    assert(z.hasNext === false)
  }
  
  it should "parse one line with quote and lf" in {
    val z: Iterator[Try[Seq[String]]] = Parser.parseLines("abc,\"def\"\n".toIterator)
    assert(z.hasNext === true)
    val cols: Seq[String] = z.next.get
    assert(cols.size === 2)
    assert(cols(0) === "abc")
    assert(cols(1) === "def")
    assert(z.hasNext === false)
  }

  it should "parse empty line" in {
    val z: Iterator[Try[Seq[String]]] = Parser.parseLines("\r\n".toIterator)
    assert(z.hasNext === true)
    val cols: Seq[String] = z.next.get
    assert(cols.size === 1)
    assert(cols(0) === "")
    assert(z.hasNext === false)
  }

  it should "parse col with crlf" in {
    val z: Iterator[Try[Seq[String]]] = Parser.parseLines("\"ab\r\ncd\",b".toIterator)
    assert(z.hasNext === true)
    val cols: Seq[String] = z.next.get
    assert(cols.size === 2)
    assert(cols(0) === "ab\r\ncd")
    assert(cols(1) === "b")
    assert(z.hasNext === false)
  }

  it should "parse two lines" in {
    val z: Iterator[Try[Seq[String]]] = Parser.parseLines("\"ab\r\ncd\",b\r\n\"de f\"".toIterator)
    assert(z.hasNext === true)

    doWith(z.next.get) { cols =>
      assert(cols.size === 2)
      assert(cols(0) === "ab\r\ncd")
      assert(cols(1) === "b")
    }

    assert(z.hasNext === true)
    doWith(z.next.get) { cols =>
      assert(cols.size === 1)
      assert(cols(0) === "de f")
    }

    assert(z.hasNext === false)
  }

  it should "parse empty file" in {
    import com.ruimo.scoins.LoanPattern.iteratorFromReader
    import com.ruimo.csv.Parser.parseLines

    val file = Files.createTempFile(null, ".csv")
    val t = iteratorFromReader(Files.newBufferedReader(file, Charset.forName("Windows-31j"))) {
      in: Iterator[Char] =>
      val z: Iterator[Try[Seq[String]]] = parseLines(in)
      assert(z.hasNext === false)
    }
    assert(t.isSuccess === true)
  }

  it should "parse blank line" in {
    import com.ruimo.scoins.LoanPattern.iteratorFromReader
    import com.ruimo.csv.Parser.parseLines
    import java.util.Arrays.asList

    val file = Files.createTempFile(null, ".csv")
    Files.write(file, asList("", ""), Charset.forName("Windows-31j"))
    val t = iteratorFromReader(Files.newBufferedReader(file, Charset.forName("Windows-31j"))) {
      in: Iterator[Char] =>
      val z: Iterator[Try[Seq[String]]] = parseLines(in)
      assert(z.hasNext === true)
      assert(z.next.get === Seq(""))

      assert(z.hasNext === true)
      assert(z.next.get === Seq(""))

      assert(z.hasNext === false)
    }
    assert(t.isSuccess === true)
  }

  it should "parse normal lines" in {
    import com.ruimo.scoins.LoanPattern.iteratorFromReader
    import com.ruimo.csv.Parser.parseLines
    import java.util.Arrays.asList

    val file = Files.createTempFile(null, ".csv")
    Files.write(file, asList("a,\"b\"", "1,\"2 3\""), Charset.forName("Windows-31j"))
    val t = iteratorFromReader(Files.newBufferedReader(file, Charset.forName("Windows-31j"))) {
      in: Iterator[Char] =>
      val z: Iterator[Try[Seq[String]]] = parseLines(in)
      assert(z.hasNext === true)
      assert(z.next.get === Seq("a", "b"))

      assert(z.hasNext === true)
      assert(z.next.get === Seq("1", "2 3"))

      assert(z.hasNext === false)
    }
    assert(t.isSuccess === true)
  }

  it should "parse error" in {
    import com.ruimo.scoins.LoanPattern.iteratorFromReader
    import com.ruimo.csv.Parser.parseLines
    import java.util.Arrays.asList

    val file = Files.createTempFile(null, ".csv")
    Files.write(file, asList("a,\"b\"", "1,\"2 3"), Charset.forName("Windows-31j"))
    val t = iteratorFromReader(Files.newBufferedReader(file, Charset.forName("Windows-31j"))) {
      in: Iterator[Char] =>
      val z: Iterator[Try[Seq[String]]] = parseLines(in)
      assert(z.hasNext === true)
      assert(z.next.get === Seq("a", "b"))

      assert(z.hasNext === true)
      z.next match {
        case s: Success[Seq[String]] => fail
        case f: Failure[Seq[String]] => {
          assert(f.exception.asInstanceOf[CsvParseException].lineNo === 2)
        }
      }

      assert(z.hasNext === false)
    }
    t.get
    assert(t.isSuccess === true)
  }

  it should "parse with header when empty" in {
    import com.ruimo.csv.Parser._

    assert(asHeaderedCsv(Iterator.empty).hasNext === false)
  }

  it should "parse with header when error" in {
    import com.ruimo.csv.Parser._

    val it = asHeaderedCsv(Iterator.single(Failure(new Exception("Error"))))
    assert(it.hasNext === true)
    it.next() match {
      case Success(_) => fail
      case Failure(e) => assert(e.getMessage === "Error")
      }
  }

  it should "parse csv having only header" in {
    import com.ruimo.csv.Parser._

    val it = asHeaderedCsv(Iterator.single(Success(Seq("Hello", "World"))))
    assert(it.hasNext === false)
  }

  it should "parse csv read error" in {
    import com.ruimo.csv.Parser._

    val it = asHeaderedCsv(Iterator(
      Success(Seq("Hello", "World")),
      Failure(new Exception("Error"))
    ))
    assert(it.hasNext === true)
    it.next() match {
      case Success(_) => fail
        case Failure(e) => assert(e.getMessage === "Error")
    }
  }

  it should "parse csv having two lines" in {
    import com.ruimo.csv.Parser._

    val it = asHeaderedCsv(Iterator(
      Success(Seq("Name", "Age")),
      Success(Seq("Ruimo", "18")),
      Success(Seq("Uno", "16"))
    ))
    assert(it.hasNext === true)
    doWith(it.next().get) { l =>
      assert(l.lineNo === 2)
      assert(l.header.header.size === 2)
      assert(l('Name) === "Ruimo")
      assert(l('Age) === "18")
    }

    assert(it.hasNext === true)
    doWith(it.next().get) { l =>
      assert(l.lineNo === 3)
      assert(l.header.header.size === 2)
      assert(l('Name) === "Uno")
      assert(l('Age) === "16")
    }

    assert(it.hasNext === false)
  }

  it should "parse csv having header line is ok and second line is ng" in {
    import com.ruimo.csv.Parser._

    val it = asHeaderedCsv(Iterator(
      Success(Seq("Name", "Age")),
      Success(Seq("Ruimo", "18")),
      Failure(new Exception("Error"))
    ))
    assert(it.hasNext === true)
    doWith(it.next().get) { l =>
      assert(l.lineNo === 2)
      assert(l.header.header.size === 2)
      assert(l('Name) === "Ruimo")
      assert(l('Age) === "18")
    }

    assert(it.hasNext === true)
    it.next() match {
      case Success(_) => fail
        case Failure(e) => assert(e.getMessage === "Error")
    }

    assert(it.hasNext === false)
  }
}
