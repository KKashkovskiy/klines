package com.klines.dataflow

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.scaladsl.{Sink, Source, Tcp}
import akka.util.ByteString
import com.klines.ActorSystemImplicits._
import spray.json._

object Upstream extends SprayJsonSupport with DefaultJsonProtocol {
    Source.maybe[ByteString]
        .via(Tcp().outgoingConnection("127.0.0.1", 5555))
        .to(Sink.foreach(a => parseAndPublish(a))).run()

    def parseAndPublish(byteString: ByteString) = {
        val length = byteString.take(2).toByteBuffer.getShort
        val timestamp = byteString.slice(2, 10).toByteBuffer.getLong
        val tickerLen = byteString.slice(10, 12).toByteBuffer.getShort
        val ticker = byteString.slice(12, 12 + tickerLen).decodeString("US-ASCII")
        val price = byteString.slice(length - 10, length - 2).toByteBuffer.getDouble
        val size = byteString.slice(length - 2, length + 2).toByteBuffer.getInt

        system.eventStream.publish(Trade(timestamp, ticker, price, size))
    }
}

case class Trade(timestamp: Long, ticker: String, price: Double, size: Int)
