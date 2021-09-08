package chipyard.CPI_test.DualClockCPITest

import chisel3._
import chisel3.iotesters.{Driver, _}
import org.scalatest._
import CPI.DualClockCPI.CaptureModuleDualClockDemo
import chipyard.CPI_test.SingleClockCPITest.referenceFrame

class CaptureModuleDualClockTester(dut: CaptureModuleDualClockDemo)
                                  (width:Int,height: Int
                                  )extends PeekPokeTester(dut){

  val prescaler = 32
  poke(dut.io.prescaler,prescaler)
  step(200)

  //====================synthesized timing========================//
  val tp = 2
  val t_line = 784 * 2
  for (imageFormat <- 0 until (2)) {
    poke(dut.io.imageFormat, imageFormat)
    val refFrame = new referenceFrame().generateRandomFrame(width*height, imageFormat)

    poke(dut.io.vsync, false.B)
    poke(dut.io.href, false.B)
    step(10)
    poke(dut.io.capture, true.B)
    step(prescaler)

    poke(dut.io.capture, false.B)
    poke(dut.io.vsync, true.B)
    poke(dut.io.href, false.B)
    step(t_line)
    poke(dut.io.vsync, false.B)
    poke(dut.io.href, false.B)
    step(17 * t_line)
    var idx = 0
    var p_clk = true

    for (col <- 0 until width) {
      poke(dut.io.href, true.B)
      for (row <- 0 until height) {
        for (plk_clock <- 0 until (imageFormat + 1)) {
          var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
            imageFormat, plk_clock)

          poke(dut.io.pixelIn,pixelIn)

          for(clock<- 0 until(prescaler)){
            step(1)
          }
        }
        var refPixel = new referenceFrame().validate(idx, refFrame)
        expect(dut.io.pixelOut,refPixel)
        idx = idx + 1
      }
      poke(dut.io.href, false.B)
      step(144 * tp)
    }
    step(200)
  }
}