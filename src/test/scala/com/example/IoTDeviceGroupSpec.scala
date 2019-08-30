package com.example

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import com.example.messages._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.Duration

class IoTDeviceGroupSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("IoTDeviceGroup"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "be able to register a device actor" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(IoTDeviceGroup.props("group"))

    groupActor.tell(RequestTrackIoTDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val firstDeviceActor = probe.lastSender

    groupActor.tell(RequestTrackIoTDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val secondDeviceActor = probe.lastSender

    firstDeviceActor should !==(secondDeviceActor)

    firstDeviceActor.tell(RecordTemperature(requestId = 0L, value = 15.6), probe.ref)
    probe.expectMsg(TemperatureRecorded(requestId = 0L))

    secondDeviceActor.tell(RecordTemperature(requestId = 5L, value = 24.1), probe.ref)
    probe.expectMsg(TemperatureRecorded(requestId = 5L))
  }

  "ignore requests for other groupId" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(IoTDeviceGroup.props("some-group-id"))

    groupActor.tell(RequestTrackIoTDevice("some-other-group-id", "somedeviceid"), probe.ref)
    probe.expectNoMessage(Duration(500, "millis"))
  }

  "return same actor for same deviceId" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(IoTDeviceGroup.props("some-group-id"))

    groupActor.tell(RequestTrackIoTDevice("some-group-id", "device-id"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val firstDeviceActor = probe.lastSender

    groupActor.tell(RequestTrackIoTDevice("some-group-id", "device-id"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val secondDeviceActor = probe.lastSender

    firstDeviceActor should ===(secondDeviceActor)
  }

  "be able to list active devices" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(IoTDeviceGroup.props("group"))

    groupActor.tell(RequestTrackIoTDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceRegistered)

    groupActor.tell(RequestTrackIoTDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceRegistered)

    groupActor.tell(RequestIoTDeviceList(0L), probe.ref)
    probe.expectMsg(ReplyIoTDeviceList(requestId = 0L, deviceIds = Set("device1", "device2")))
  }

  "be able to list active devices after one shuts down" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(IoTDeviceGroup.props("group"))

    groupActor.tell(RequestTrackIoTDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    val toShutDown = probe.lastSender

    groupActor.tell(RequestTrackIoTDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceRegistered)

    groupActor.tell(RequestIoTDeviceList(requestId = 0), probe.ref)
    probe.expectMsg(ReplyIoTDeviceList(requestId = 0, Set("device1", "device2")))

    probe.watch(toShutDown)
    toShutDown ! PoisonPill
    probe.expectTerminated(toShutDown)

    probe.awaitAssert {
      groupActor.tell(RequestIoTDeviceList(requestId = 1), probe.ref)
      probe.expectMsg(ReplyIoTDeviceList(requestId = 1, Set("device2")))
    }
  }
}
