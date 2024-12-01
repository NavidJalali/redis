package io.reverie.redis.protocol

import io.reverie.redis.codec.{Decoder, Encoder}
import munit.FunSuite

import java.io.InputStream

final class RESPTest extends FunSuite {
  test("encode simple string") {
    val str = "PING"
    val expected = s"+${str}\r\n".getBytes
    val result = Encoder[RESP, Array[Byte]]().encode(RESP.Str(str))
    assert(result.sameElements(expected))
  }
  test("decode simple string") {
    val str = "PING"
    val inputStream = makeInputStream(s"+${str}\r\n")

    val result = Decoder[InputStream, RESP]().decode(inputStream)
    assertEquals(result, Right(RESP.Str(str)))
  }

  test("encode error") {
    val str = "ERR"
    val expected = s"-${str}\r\n".getBytes
    val result = Encoder[RESP, Array[Byte]]().encode(RESP.Err(str))
    assert(result.sameElements(expected))
  }

  test("decode error") {
    val str = "ERR"
    val inputStream = makeInputStream(s"-${str}\r\n")

    val result = Decoder[InputStream, RESP]().decode(inputStream)
    assertEquals(result, Right(RESP.Err(str)))
  }

  test("encode integer (positive)") {
    val value = 123
    val expected = s":${value}\r\n".getBytes
    val result = Encoder[RESP, Array[Byte]]().encode(RESP.Int(value))
    assert(result.sameElements(expected))
  }

  test("decode integer (positive)") {
    val value = 123
    val inputStream = makeInputStream(s":${value}\r\n")

    val result = Decoder[InputStream, RESP]().decode(inputStream)
    assertEquals(result, Right(RESP.Int(value)))
  }

  test("encode integer (negative)") {
    val value = -123
    val expected = s":${value}\r\n".getBytes
    val result = Encoder[RESP, Array[Byte]]().encode(RESP.Int(value))
    assert(result.sameElements(expected))
  }

  test("decode integer (negative)") {
    val value = -123
    val inputStream = makeInputStream(s":${value}\r\n")

    val result = Decoder[InputStream, RESP]().decode(inputStream)
    assertEquals(result, Right(RESP.Int(value)))
  }

  test("encode bulk string") {
    val str = "Hello, World!"
    val expected = s"$$${str.length}\r\n${str}\r\n".getBytes
    val result = Encoder[RESP, Array[Byte]]().encode(RESP.Bin(str))
    assert(result.sameElements(expected))
  }

  test("decode bulk string") {
    val str = "Hello, World!"
    val inputStream = makeInputStream(s"$$${str.length}\r\n${str}\r\n")

    val result = Decoder[InputStream, RESP]().decode(inputStream)
    assertEquals(result, Right(RESP.Bin(str)))
  }

  test("encode array") {
    val values = List(RESP.Str("PING"), RESP.Int(123), RESP.Err("ERR"))
    val expected = s"*${values.length}\r\n+PING\r\n:123\r\n-ERR\r\n".getBytes
    val result = Encoder[RESP, Array[Byte]]().encode(RESP.Arr(values))
    assert(result.sameElements(expected))
  }

  test("decode array") {
    val values = List(RESP.Str("PING"), RESP.Int(123), RESP.Err("ERR"))
    val inputStream = makeInputStream(s"*${values.length}\r\n+PING\r\n:123\r\n-ERR\r\n")

    val result = Decoder[InputStream, RESP]().decode(inputStream)
    assertEquals(result, Right(RESP.Arr(values)))
  }

  private def makeInputStream(value: String) = new InputStream {
    private val bytes = value.getBytes
    private var index = 0

    override def read(): Int = {
      if (index >= bytes.length) -1
      else {
        val byte = bytes(index)
        index += 1
        byte
      }
    }
  }
}