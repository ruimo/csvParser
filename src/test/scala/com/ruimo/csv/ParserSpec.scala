package com.ruimo.csv

import org.specs2.mutable.Specification

class ParserSpec extends Specification {
  "CSV parser" should {
    "parse empty string" in {
      Parser.parse("").get.size === 0
    }

    "parse empty string" in {
      val cols = Parser.parse("abc").get
      cols.size === 1
      cols(0) === "abc"
    }

    "parse quoted string" in {
      val cols = Parser.parse("\"a b\"\"c\"").get
      cols.size === 1
      cols(0) === "a b\"c"
    }

    "parse columns" in {
      val cols = Parser.parse("\"a b\"\"c\",b,\"c\"").get
      cols.size === 3
      cols(0) === "a b\"c"
      cols(1) === "b"
      cols(2) === "c"
    }

    "tailing quote" in {
      Parser.parse("\"a b\"\"").get must throwA[CsvParseException]
    }

    "non ended quote" in {
      Parser.parse("\"a b").get must throwA[CsvParseException]
    }
  }
}
