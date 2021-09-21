package sislab.cpi

import chisel3._
import chisel3.iotesters._
import org.scalatest._
import chiseltest._

class CaptureModuleTester(dut:CaptureModule)(n:Int) extends PeekPokeTester(dut) {

  val height = dut.h
  val width = dut.w

  // To run the simulation, n must be greater than 1
  val pclock = n
  val tp = 2 * pclock
  val t_line = 784 * 2 * pclock

  //====================synthesized timing========================//

  for (imageFormat <- 0 until (2)) {
    poke(dut.io.imageFormat, imageFormat)
    val refFrame = new referenceFrame().generateRandomFrame(height * width, imageFormat)

    poke(dut.io.vsync, false.B)
    poke(dut.io.href, false.B)
    step(10)
    poke(dut.io.capture, true.B)
    step(2)
    poke(dut.io.capture, false.B)
    poke(dut.io.vsync, true.B)
    poke(dut.io.href, false.B)
    step(3 * t_line)
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

          poke(dut.io.href, true.B)
          poke(dut.io.vsync, false.B)
          p_clk = !p_clk
          poke(dut.io.pclk, p_clk.asBool())
          if (p_clk == false) {
            poke(dut.io.pixelIn, pixelIn)
          }
          step(pclock / 2)
          p_clk = !p_clk
          poke(dut.io.pclk, p_clk.asBool())
          step(pclock / 2)
        }
        idx = idx + 1
      }
      poke(dut.io.href, false.B)
      step(144 * tp)
    }
    step(1 * 784 * tp)
    poke(dut.io.vsync, true.B)
    step(1 * 784 * tp)
    //=========================validation============================//

    while (peek(dut.io.frameFull) == 1) {
      poke(dut.io.readFrame, true.B)
      step(1)

      var idx_out = peek(dut.io.pixelAddr).toInt // pixel_address
      var refPixelVal = new referenceFrame().validate(idx_out, refFrame)

//      println("ref: "+refPixelVal.toHexString+" got "+peek(dut.io.pixelOut).toInt.toHexString)
      if (imageFormat == 1) {
        expect(dut.io.pixelOut, refPixelVal)
      }
      else {
        expect(dut.io.pixelOut, refPixelVal)
      }
    }
    poke(dut.io.readFrame, false)
    step(200)
  }
  Console.out.println(Console.BLUE+"the total number of tests must be passed is: "+
    Console.YELLOW+width*height*2 + Console.RESET)
}

class referenceFrame{

  def generateRandomFrame(ImageResolution:Int, ImageFormat: Int): Array[Int]={
    if(ImageFormat == 0){
      val refFrame = Array.fill(ImageResolution){scala.util.Random.nextInt(255)}
      return refFrame
    }
    else {
      val refFrame = Array.fill(ImageResolution){scala.util.Random.nextInt(65535)}
      return refFrame
    }
  }

  def pixelStream(idx:Int,refFrame: Array[Int],
                  ImageFormat:Int,
                  pclk: Int): Int ={
    if(ImageFormat==0){
      return refFrame(idx)
    }
    else {
      var firstByte = refFrame(idx)>>8
      var secondByte = refFrame(idx)&0xFF
      if (pclk == 0) {
        return firstByte
      }
      else {
        return secondByte
      }
    }
  }

  def validate(idx: Int , refFrame: Array[Int]): Int={
    return refFrame(idx)
  }
}


//class CaptureModuleSpec extends FlatSpec with Matchers {
//  "Capture Module Single Clock Gray and RGB " should "pass" in {
//    chisel3.iotesters.Driver(() => new CaptureModule(
//      10,20,
//      2,100*100)) { c =>
//      new CaptureModuleTester(c)(4)
//    } should be(true)
//  }
//}

class CaptureModuleChiselTest extends FlatSpec with ChiselScalatestTester{
  behavior of "Capture module"

  def CaptureModuleTest[T <: CaptureModule](dut: T, n :Int)={


    val width = dut.w
    val height = dut.h
    val pclock = n
    var nTestPassed = 0

    println("generate a random frame")
    val tp = 2 * pclock
    val t_line = 10 * pclock
    //====================synthesized timing========================//

    for (imageFormat <- 0 until(2)) {
      dut.io.imageFormat.poke(imageFormat.U)
      val refFrame = new referenceFrame().generateRandomFrame(height * width, imageFormat)

      dut.io.vsync.poke(false.B)
      dut.io.href.poke(false.B)
      dut.clock.step(10)
      dut.io.capture.poke(true.B)
      dut.clock.step(2)
      dut.io.capture.poke(false.B)
      dut.io.vsync.poke(true.B)
      dut.io.href.poke(false.B)
      dut.clock.step(3 * t_line)
      dut.io.vsync.poke(false.B)
      dut.io.href.poke(false.B)
      dut.clock.step(t_line)

      var idx = 0
      var pclk = true
      for (col <- 0 until(width)){
        dut.io.href.poke(true.B)
        for (row <- 0 until(height)){
          for (plkClock<- 0 until(imageFormat+1)){
            var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
              imageFormat, plkClock)

            dut.io.href.poke(true.B)
            dut.io.vsync.poke(false.B)
            pclk = !pclk
            dut.io.pclk.poke(pclk.asBool())
            if (pclk == false){
              dut.io.pixelIn.poke(pixelIn.asUInt())
            }
            dut.clock.step(pclock/2)
            pclk = !pclk
            dut.io.pclk.poke(pclk.asBool())
            dut.clock.step(pclock/2)
          }
          idx = idx + 1
        }
        dut.io.href.poke(false.B)
        dut.clock.step(2*tp)
      }
      dut.clock.step(tp)
      dut.io.vsync.poke(true.B)
      dut.clock.step(tp)

      //====================validation=======================//

      while (dut.io.frameFull.peek.litToBoolean) {
        dut.io.readFrame.poke(true.B)
        dut.clock.step(1)

        var idx_out = dut.io.pixelAddr.peek.litValue.toInt // pixel_address
        var refPixelVal = new referenceFrame().validate(idx_out, refFrame).toInt
        dut.io.pixelOut.expect(refPixelVal.U)
        if(refPixelVal==dut.io.pixelOut.peek.litValue.toInt){
          nTestPassed += 1
        }
      }
      dut.io.readFrame.poke(false.B)
      dut.clock.step(50)
    }
    Console.out.println(Console.YELLOW+nTestPassed+" tests passed over "+
      Console.YELLOW+width*height*2+ " being tested"+ Console.RESET)
  }

  it should "pass" in {
    test(new CaptureModule(20, 20, 2, 20*22))
    { dut => CaptureModuleTest(dut,4)}
  }
}