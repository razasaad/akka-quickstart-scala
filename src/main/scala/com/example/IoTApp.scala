package com.example

import akka.actor.ActorSystem

import scala.io.StdIn

object IoTApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("iot-system")

    try {
      val supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor")

      StdIn.readLine()
    } finally {
      system.terminate()
    }
  }
}
