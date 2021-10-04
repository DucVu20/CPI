package sislab.cpi

import chisel3._
import org.scalatest._
import chiseltest._

import chiseltest.internal.WriteVcdAnnotation
import chiseltest.experimental.TestOptionBuilder._

class SCCBInterfaceTest extends FlatSpec with ChiselScalatestTester{
  behavior of "SCCB Interface - I2C"
  def SCCBInterfaceTest [T <: I2CMaster](dut: T, nRandomTest: Int) = {

    var numberOfTestsPassed = 0
    dut.clock.setTimeout(80000)

    dut.io.config.poke(false.B)
    dut.io.coreEna.poke(true.B)
    dut.io.preScalerLow.poke(1.U)
    dut.io.preScalerHigh.poke(1.U)
    dut.clock.step(20)

    for (nTest <- 0 until (nRandomTest)) {
      val slaveAddr = (0x42)
      val controlAddr = 128//scala.util.Random.nextInt(255)
      val configData = 255 //scala.util.Random.nextInt(255)

      dut.io.controlAddr.poke(controlAddr.U)
      dut.io.configData.poke(configData.U)

      while (!dut.io.sccbReady.peek.litToBoolean) {
        dut.clock.step(1)
      }
      dut.io.config.poke(true.B)
      dut.clock.step(1)
      dut.io.config.poke(false.B)
      dut.clock.step(1)

      var dataBitIdx = 7
      val transmittedSlaveAddr = Array.fill(8){0}
      val transmittedAddr = Array.fill(8){0}
      val transmittedData = Array.fill(8) {0}
      var phase = 0

      while (!dut.io.sccbReady.peek.litToBoolean) {
        var cLow = dut.io.SIOC.peek.litValue()
        dut.clock.step(1)
        var cHigh = dut.io.SIOC.peek.litValue()

        if ((cHigh - cLow) == 1) { //detect edge in SIOC
          var SIOD = dut.io.SIOD.peek.litValue
          if ((phase == 0) && (dataBitIdx != -1)) {
            transmittedSlaveAddr(dataBitIdx) = SIOD.toInt
          }
          else if ((phase == 1) && (dataBitIdx != -1)) {
            transmittedAddr(dataBitIdx) = SIOD.toInt
          }
          else if (phase == 2 && (dataBitIdx != -1)) {
            transmittedData(dataBitIdx) = SIOD.toInt
          }
          dataBitIdx = dataBitIdx - 1
          if (dataBitIdx == -2) {
            dataBitIdx = 7
            phase = phase + 1
          }
        }
      }
      //=======test config is inserted but coreEna is disabled=========//
      for (a <- 0 until 1000) {
        dut.io.coreEna.poke(false.B)
        dut.io.config.poke(true.B)
        dut.clock.step(5)
        if (dut.io.SIOC.peek.litToBoolean != dut.io.SIOD.peek.litToBoolean) {
          println("CoreDisable Mode failed")
        }
      }
      dut.io.coreEna.poke(true.B)
      dut.io.config.poke(false.B)

      // software check transmitted bits //

      if (slaveAddr == bin2dec(transmittedSlaveAddr)) {
        if ((controlAddr == bin2dec(transmittedAddr)) && (configData == bin2dec(transmittedData))) {
          numberOfTestsPassed = numberOfTestsPassed + 1
        }
      }
    }
  Console.out.println(Console.YELLOW+"test result of SCCB interface: " + numberOfTestsPassed.toString+
      " tests passed over "+nRandomTest.toString+" being tested"+Console.RESET)
    def bin2dec(in : Array[Int]): Int={
      val arrayLength = in.length
      var dec = 0
      for(idx<-0 until arrayLength){
        dec = dec + in(idx) * scala.math.pow(2,idx).toInt
      }
      dec
    }
  }
  it should "pass" in {
    test(new I2CMaster())
    { dut => SCCBInterfaceTest(dut, 40)}
  }

  "CaptureModule wave" should "pass" in{
    test(new I2CMaster()).withAnnotations(Seq(WriteVcdAnnotation)){
      dut => SCCBInterfaceTest(dut, 4)
    }
  }
}