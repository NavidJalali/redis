package io.reverie.redis.protocol

import io.reverie.redis.codec.{Decoder, Encoder}
import munit.FunSuite

final class RESPTest extends FunSuite {
  test("encode simple string") {
    val resp = RESP.Str("hello")
    val actual = String(Encoder[RESP, Array[Byte]].encode(resp))
    val expected = "+hello\r\n"
    assertEquals(actual, expected)
  }

  test("decode simple string") {
    val raw = "+hello\r\n"
    val actual = Decoder[Array[Byte], RESP].decode(raw.getBytes)
    val expected = Right(RESP.Str("hello"))
    assertEquals(actual, expected)
  }

  test("encode error") {
    val resp = RESP.Err("error")
    val actual = String(Encoder[RESP, Array[Byte]].encode(resp))
    val expected = "-error\r\n"
    assertEquals(actual, expected)
  }

  test("decode error") {
    val raw = "-error\r\n"
    val actual = Decoder[Array[Byte], RESP].decode(raw.getBytes)
    val expected = Right(RESP.Err("error"))
    assertEquals(actual, expected)
  }

  test("encode integer") {
    val positiveResp = RESP.Int(42)
    val actualPositive = String(Encoder[RESP, Array[Byte]].encode(positiveResp))
    val expectedPositive = ":42\r\n"
    assertEquals(actualPositive, expectedPositive)

    val negativeResp = RESP.Int(-42)
    val actualNegative = String(Encoder[RESP, Array[Byte]].encode(negativeResp))
    val expectedNegative = ":-42\r\n"
    assertEquals(actualNegative, expectedNegative)
  }

  test("decode integer") {
    val rawPositive = ":42\r\n"
    val actualPositive = Decoder[Array[Byte], RESP].decode(rawPositive.getBytes)
    val expectedPositive = Right(RESP.Int(42))
    assertEquals(actualPositive, expectedPositive)

    val rawNegative = ":-42\r\n"
    val actualNegative = Decoder[Array[Byte], RESP].decode(rawNegative.getBytes)
    val expectedNegative = Right(RESP.Int(-42))
    assertEquals(actualNegative, expectedNegative)
  }

  test("encode bulk string") {
    val resp = RESP.Bin("hello")
    val actual = String(Encoder[RESP, Array[Byte]].encode(resp))
    val expected = "$5\r\nhello\r\n"
    assertEquals(actual, expected)
  }

  test("decode bulk string") {
    val raw = "$5\r\nhello\r\n"
    val actual = Decoder[Array[Byte], RESP].decode(raw.getBytes)
    val expected = Right(RESP.Bin("hello"))
    assertEquals(actual, expected)
  }

  test("encode bulk empty string") {
    val resp = RESP.Bin("")
    val actual = String(Encoder[RESP, Array[Byte]].encode(resp))
    val expected = "$0\r\n\r\n"
    assertEquals(actual, expected)
  }

  test("decode bulk empty string") {
    val raw = "$0\r\n\r\n"
    val actual = Decoder[Array[Byte], RESP].decode(raw.getBytes)
    val expected = Right(RESP.Bin(""))
    assertEquals(actual, expected)
  }
}