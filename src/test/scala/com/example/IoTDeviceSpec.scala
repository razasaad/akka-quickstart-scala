package com.example

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.example.messages._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.Duration
import scala.util.Random

class IoTDeviceSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("IoTDevice"))

  override def afterAll: Unit = {
    shutdown(system)
  }


  implicit val stringProvider = () => Random.nextString(Random.nextInt(100))

  implicit val longProvider = () => Random.nextLong()

  implicit val doubleProvider = () => Random.nextDouble()

  def SomeRandomGenerator[T](implicit fun: () => T): T = {
    fun.apply()
  }

  "reply with empty reading if no temperature is known" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(IoTDevice.props(SomeRandomGenerator, SomeRandomGenerator))
    val requestId = SomeRandomGenerator[Long]
    deviceActor.tell(ReadTemperature(requestId), probe.ref)

    val response = probe.expectMsgType[RespondTemperature]

    response.requestId should ===(requestId)
    response.value should ===(None)
  }

  "respond on getting a temperature" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(IoTDevice.props(SomeRandomGenerator, SomeRandomGenerator))
    val requestId = SomeRandomGenerator[Long]
    val temperature = SomeRandomGenerator[Double]
    deviceActor.tell(RecordTemperature(requestId, temperature), probe.ref)

    val response = probe.expectMsgType[TemperatureRecorded]

    response.requestId should ===(requestId)
  }

  "should respond and on RecordTemperature and return RespondTemperature with the valid value" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(IoTDevice.props(SomeRandomGenerator, SomeRandomGenerator))
    val requestId = SomeRandomGenerator[Long]
    val temperature = SomeRandomGenerator[Double]
    deviceActor.tell(RecordTemperature(requestId, temperature), probe.ref)

    val response = probe.expectMsgType[TemperatureRecorded]

    response.requestId should ===(requestId)

    deviceActor.tell(ReadTemperature(requestId), probe.ref)

    val responseRespondTemperature = probe.expectMsgType[RespondTemperature]

    responseRespondTemperature.requestId should ===(requestId)
    responseRespondTemperature.value should ===(Some(temperature))
  }

  "reply to registration requests" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(IoTDevice.props("group", "device"))

    deviceActor.tell(RequestTrackIoTDevice("group", "device"), probe.ref)

    probe.expectMsg(DeviceRegistered)
    probe.lastSender should ===(deviceActor)
  }

  "ignore wrong registration requests" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(IoTDevice.props("group", "device"))

    deviceActor.tell(RequestTrackIoTDevice("someGroup", "deviceId"), probe.ref)
    probe.expectNoMessage(Duration(500, "millis"))

    deviceActor.tell(RequestTrackIoTDevice("someGroup", "someDevice"), probe.ref)
    probe.expectNoMessage(Duration(500, "millis"))
  }
}
