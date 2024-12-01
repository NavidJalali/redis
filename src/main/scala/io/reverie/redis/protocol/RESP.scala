package io.reverie.redis.protocol

import cats.syntax.all.*
import io.reverie.redis.codec.Encoder
import io.reverie.redis.codec.Decoder
import io.reverie.redis.codec.Decoder.DecodeError

import java.io.{BufferedInputStream, BufferedReader, IOException, InputStream}
import scala.annotation.tailrec
import scala.io.{BufferedSource, Source}
import scala.util.{Using, boundary}

enum RESP {
  case Str(value: String)
  case Bin(value: String)
  case Err(value: String)
  case Int(value: scala.Int)
  case Arr(values: Seq[RESP])
}

object RESP {
  given Encoder[RESP, Array[Byte]] = {
    def write(sb: StringBuilder)(resp: RESP): StringBuilder =
      resp match
        case RESP.Str(value) =>
          sb.append(s"+$value\r\n")
        case RESP.Bin(value) =>
          sb.append(s"$$${value.length}\r\n$value\r\n")
        case RESP.Err(value) =>
          sb.append(s"-$value\r\n")
        case RESP.Int(value) =>
          sb.append(s":$value\r\n")
        case RESP.Arr(values) =>
          sb.append(s"*${values.length}\r\n")
          values.foreach(write(sb))
          sb

    Encoder.fromFunction { resp =>
      write(new StringBuilder)(resp).toString().getBytes
    }
  }

  given Decoder[InputStream, RESP] =
    Decoder.fromFunction {
      def readLine(source: BufferedReader): Either[DecodeError, String] =
        try source.readLine().asRight
        catch
          case cause: IOException => Decoder.DecodeError.ExhaustedInput(cause).asLeft
          case other => throw other

      def readChar(source: BufferedReader): Either[DecodeError, Char] =
        try source.read().toChar.asRight
        catch
          case cause: IOException => Decoder.DecodeError.ExhaustedInput(cause).asLeft
          case other => throw other

      def readExact(source: BufferedReader, length: scala.Int): Either[DecodeError, Array[Byte]] =
        val charBuffer = Array.ofDim[Char](length)
        val byteBuffer = Array.ofDim[Byte](length)

        @tailrec def go(bytesRead: scala.Int): Either[DecodeError, Array[Byte]] =
          if bytesRead == length then byteBuffer.asRight
          else
            val read = source.read(charBuffer, bytesRead, length - bytesRead)
            if read == -1 then DecodeError.ExhaustedInput("EOF during readExact").asLeft
            else
              charBuffer.mkString.getBytes.copyToArray(byteBuffer, bytesRead, read)
              go(bytesRead + read)

        go(0)

      def int(raw: String): Either[DecodeError, scala.Int] =
        raw.toIntOption
          .toRight(DecodeError.BadInput("Cannot be cast into Int", raw))

      def arr(source: BufferedReader, length: scala.Int): Either[DecodeError, Seq[RESP]] =
        boundary:
          val builder = Vector.newBuilder[RESP]
          0 until length foreach { _ =>
            resp(source) match
              case Left(error) => boundary.break(error.asLeft)
              case Right(value) => builder += value
          }
          builder.result().asRight

      def resp(reader: BufferedReader): Either[DecodeError, RESP] =
        for {
          firstChar <- readChar(reader)
          resp <-
            firstChar match
              case '+' => readLine(reader).map(RESP.Str(_))
              case '-' => readLine(reader).map(RESP.Err(_))
              case ':' => readLine(reader).flatMap(int).map(RESP.Int(_))
              case '$' =>
                readLine(reader)
                  .flatMap(int)
                  .flatMap(readExact(reader, _))
                  .map(bytes => RESP.Bin(new String(bytes)))
              case '*' =>
                readLine(reader)
                  .flatMap(int)
                  .flatMap(arr(reader, _))
                  .map(RESP.Arr(_))
              case other => DecodeError.BadInput("Unknown RESP type", other).asLeft
        } yield resp

      (is: InputStream) =>
        Using.resource(Source.fromInputStream(is)) {
          source =>
            val reader = source.bufferedReader()
            resp(reader)
        }
    }
}
