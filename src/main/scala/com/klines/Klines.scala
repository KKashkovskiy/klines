package com.klines

import com.klines.dataflow.{ServerTcp, TradesAggregationActor, Upstream}

object Klines extends App {
    TradesAggregationActor.ref
    Upstream
    ServerTcp
}
