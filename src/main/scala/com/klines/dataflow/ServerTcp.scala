package com.klines.dataflow

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp}
import akka.util.{ByteString, Timeout}
import com.klines.ActorSystemImplicits._
import spray.json._
import akka.pattern.ask
import scala.concurrent.duration._


object ServerTcp extends SprayJsonSupport with DefaultJsonProtocol {

    implicit val klineJsonFormat: RootJsonFormat[Kline] = jsonFormat7(Kline)
    implicit val timeout = Timeout(10 seconds)

    val binding = Tcp().bind("localhost", 8888).runForeach{ connect =>
        println("connected")
        (TradesAggregationActor.ref ? LastTenMinutesKlines).mapTo[List[Kline]].foreach { klines =>
            val publishFlow = connect.flow
            val bufferSize = 1024
            val overflowStrategy = akka.stream.OverflowStrategy.dropHead
            val messageSource = Source.actorRef[Kline](bufferSize, overflowStrategy)
            val marshalFlow = Flow[Kline].map(kline => ByteString(kline.toJson.toString))
            val subscriberRef = messageSource.via(marshalFlow)
                .via(publishFlow)
                .to(Sink.ignore)
                .run()
            klines.sortBy(_.timestamp).foreach(kline => subscriberRef ! kline)
            system.eventStream.subscribe(subscriberRef, classOf[Kline])
        }
    }





}
