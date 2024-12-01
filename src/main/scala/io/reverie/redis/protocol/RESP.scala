package io.reverie.redis.protocol

import cats.syntax.either.*
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

      (is: InputStream) =>
        Using.resource(Source.fromInputStream(is)) {
          source =>
            val reader = source.bufferedReader()
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
                      .flatMap { length =>
                        (0 until length)
                          .foldLeft(Right(Vector.empty[RESP]): Either[DecodeError, Vector[RESP]]) {
                            case (acc, _) =>
                              acc.flatMap { values =>
                                Decoder[InputStream, RESP].decode(is).map(values :+ _)
                              }
                          }
                          .map(RESP.Arr(_))
                      }
            } yield resp
        }
    }


  //        given Decoder[Array[Byte], RESP] =
  //          Decoder.utf8 emap { raw =>
  //            for {
  //              lines <- Decoder.lines.decode(raw)
  //              firstLine <-
  //                lines
  //                  .headOption
  //                  .toRight(
  //                    Decoder.DecodeError.ExhaustedInput(raw)
  //                  )
  //              firstChar <-
  //                firstLine
  //                  .headOption
  //                  .toRight(
  //                    Decoder.DecodeError.ExhaustedInput(raw)
  //                  )
  //              resp <-
  //                firstChar match
  //                  // simple string
  //                  case '+' =>
  //                    val value = firstLine.tail
  //                    RESP.Str(value).asRight
  //                  // simple error
  //                  case '-' =>
  //                    val value = firstLine.tail
  //                    RESP.Err(value).asRight
  //                  // integer
  //                  case ':' =>
  //                    firstLine.tail
  //                      .strip()
  //                      .toIntOption
  //                      .toRight(Decoder.DecodeError.BadInput("Cannot be cast into Int", firstLine))
  //                      .map(RESP.Int.apply)
  //                  case '$' =>
  //                    for {
  //                      length <-
  //                        firstLine
  //                          .tail
  //                          .toIntOption
  //                          .toRight(
  //                            Decoder.DecodeError.BadInput("Cannot parse length", firstLine)
  //                          )
  //                      data <-
  //                        Either.cond(
  //                          lines.length >= 2,
  //                          lines(1),
  //                          Decoder.DecodeError.ExhaustedInput(raw)
  //                        )
  //                      result <-
  //                        Either.cond(
  //                          data.length == length,
  //                          RESP.Bin(data),
  //                          Decoder.DecodeError.BadInput(
  //                            s"Expected $length bytes, got ${data.length}",
  //                            data
  //                          )
  //                        )
  //                    } yield result
  //                  case '*' =>
  //                    for {
  //                      length <-
  //                        firstLine
  //                          .tail
  //                          .toIntOption
  //                          .toRight(
  //                            Decoder.DecodeError.BadInput("Cannot parse length", firstLine)
  //                          )
  //
  //                      data = lines.drop(1)
  //
  //                    } yield ???
  //            } yield resp
  //          }
}
