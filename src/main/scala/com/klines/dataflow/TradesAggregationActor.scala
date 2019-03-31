package com.klines.dataflow

import java.text.SimpleDateFormat
import java.util.Date

import akka.actor.{Actor, ActorRef, Props}
import com.klines.ActorSystemImplicits._

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer

class TradesAggregationActor extends Actor {

    system.eventStream.subscribe(self, classOf[Trade])

    private var lastTradeMinute: Int = -1
    private val tradesMap: TrieMap[String, ArrayBuffer[Trade]] = TrieMap[String, ArrayBuffer[Trade]]()
    private val candlesMap: TrieMap[String, ArrayBuffer[Kline]] = TrieMap[String, ArrayBuffer[Kline]]()

    override def receive: Receive = {
        case trade: Trade =>
            val minute = new Date(trade.timestamp).getMinutes
            if (lastTradeMinute == -1) lastTradeMinute = minute
            else if (lastTradeMinute == minute)
                tradesMap.put(trade.ticker, tradesMap.get(trade.ticker).fold(ArrayBuffer(trade))(_ :+ trade))
            else {
                lastTradeMinute = minute
                publishCandles(tradesMap)
                tradesMap.foreach { case (_, v) => v.clear() }
                tradesMap.put(trade.ticker, tradesMap.get(trade.ticker).fold(ArrayBuffer(trade))(_ :+ trade))
            }
        case LastTenMinutesKlines =>
            val currentTime = new Date().getTime
            val filtered = candlesMap.values.flatten.filter{ kline =>
                val format = new SimpleDateFormat("yyyy-MM-ddHH:mm:00")
                val klineTime = format.parse(kline.timestamp.dropRight(1).split("T").mkString).getTime
                currentTime - klineTime <= 10*60*1000
            }.toList

            sender() ! filtered
    }

    private def publishCandles(tradesMap: TrieMap[String, ArrayBuffer[Trade]]) = {
        val timestamp = formatTime
        tradesMap.foreach { case (ticker, trades) =>
            val candle = Kline(
                ticker,
                timestamp,
                trades.head.price,
                trades.map(_.price).max,
                trades.map(_.price).min,
                trades.last.price,
                trades.map(_.size).sum
            )
            system.eventStream.publish(candle)
            candlesMap.put(ticker, candlesMap.get(ticker).fold(ArrayBuffer(candle))(_ :+ candle))
        }
    }

    private def formatTime = {
        val format = new SimpleDateFormat("yyyy-MM-ddHH:mm:00")
        val (date, time) = format.format(new java.util.Date()).splitAt(10)
        s"${date}T${time}Z"
    }
}

object TradesAggregationActor{
    val ref: ActorRef = system.actorOf(Props(new TradesAggregationActor()))
}

case class Kline(ticker: String,
                 timestamp: String,
                 open: Double,
                 high: Double,
                 low: Double,
                 close: Double,
                 volume: Int)

case object LastTenMinutesKlines
