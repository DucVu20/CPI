package chipyard.CPI_test.DualClockCPITest

import CPI.DualClockCPI.{CaptureInterfaceDemo}
import chipyard.CPI_test.SingleClockCPITest.referenceFrame
import chisel3._
import chisel3.iotesters.Driver
import org.scalatest._
import chisel3.iotesters._
import chisel3.util._

class CaptureInterfaceTester(dut: CaptureInterfaceDemo)
                            (width: Int,height: Int) extends PeekPokeTester(dut){

  val prescaler = 8
  poke(dut.io.prescaler,prescaler)
  step(5*prescaler)

  //====================synthesized timing========================//
  val tp = prescaler
  val t_line = 10*prescaler
  poke(dut.io.capture, true.B)
  step(prescaler)
  for (imageFormat <- 0 until (2)) {
    poke(dut.io.imageFormat, imageFormat)
    val refFrame = new referenceFrame().generateRandomFrame(width*height, imageFormat)
    poke(dut.io.vsync, false.B)
    poke(dut.io.href, false.B)
    step(prescaler)

    poke(dut.io.vsync, true.B)
    poke(dut.io.href, false.B)
    step(t_line)
    poke(dut.io.vsync, false.B)
    poke(dut.io.href, false.B)
    step(2 * t_line)
    var idx = 0
    var p_clk = true

    for (col <- 0 until width) {
      poke(dut.io.href, true.B)
      for (row <- 0 until height) {
        if(imageFormat==0){
          print(" Format: Gray ")
        }
        else{
          print(" Format: RGB ")
        }
        for (plk_clock <- 0 until (imageFormat + 1)) {

          var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
            imageFormat, plk_clock)

          poke(dut.io.pixelIn,pixelIn)
          if(imageFormat==0){
            print(" pixelIn "+pixelIn.toHexString)
          }
          else {
            if(plk_clock==0){
              print(" first pixel in: "+ pixelIn.toHexString)
            }
            else {
              print(", second pixel in: "+pixelIn.toHexString)
            }
          }
          step(prescaler)
          print(", pixel_index is: " + peek(dut.io.pixelIndex).toInt)
        }
        var refPixel = new referenceFrame().validate(idx, refFrame)
        print(", Got: "+peek(dut.io.pixelOut).toInt.toHexString +
          " ref: "+ refPixel.toHexString+"   ")
        println(" ")
        expect(dut.io.pixelOut,refPixel)

        idx = idx + 1
      }
      poke(dut.io.href, false.B)
      step(5*prescaler)
    }
    step(prescaler*2)
  }
}

class WaveformCaptureInterface extends FlatSpec with Matchers {
  "WaveformCounter" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
      new CaptureInterfaceDemo(5*5,64)){ c =>
      new CaptureInterfaceTester(c)(5,5)
    } should be (true)
  }
}

class CaptureInterfaceSpec extends FlatSpec with Matchers {
    "CaptureInterface" should "pass" in {
    chisel3.iotesters.Driver (() =>
      new CaptureInterfaceDemo(5*5,64)) { c =>
      new CaptureInterfaceTester(c)(5,5)
    } should be (true)
  }
}
object CaptureInterfaceTester extends App{
  chisel3.iotesters.Driver(() => new CaptureInterfaceDemo(5*5,64)) { c =>
    new CaptureInterfaceTester(c)(5,5)
  }
}
